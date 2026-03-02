package model;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GerenciadorDeEleicao {
  private final String PREFIXO_ELEICAO = "ELEICAO";
  private final String PREFIXO_OK = "OK";
  private final String PREFIXO_LIDER = "LIDER";
  private final String PREFIXO_LIDER_RECEBIDO = "LIDER_RECEBIDO";
  private static GerenciadorDeEleicao instancia;
  private GerenciadorDePeers gerenciadorDeServidores;
  private final PeerListener servidorAtual;
  private final InfoPeer minhasInformacoes;
  private GerenciadorDeReplicacao gerenciadorDeReplicacao;
  private ServerSocket socketEleicao;
  private volatile boolean recebeuRespostasOK = false;

  private GerenciadorDeEleicao() {
    this.servidorAtual = PeerListener.getInstancia();
    this.minhasInformacoes = servidorAtual.getMinhasInformacoes();
    this.gerenciadorDeServidores = GerenciadorDePeers.getInstancia();
    this.gerenciadorDeReplicacao = GerenciadorDeReplicacao.getInstancia();
    this.socketEleicao = gerenciadorDeReplicacao.getSocketReplicacao();
  }

  public static GerenciadorDeEleicao getInstancia() {
    if (instancia == null) {
      instancia = new GerenciadorDeEleicao();
    }
    return instancia;
  }

  public void chamarEleicao() {
    try {
      recebeuRespostasOK = false;
      List<InfoPeer> servidores = new ArrayList<>(gerenciadorDeServidores.getListaDeServidores());

      for (InfoPeer servidor : servidores) {
        if (servidor.getTimestampInicializacao() > minhasInformacoes.getTimestampInicializacao()) {
          continue;
        }

        ConexaoPeer conexao = servidor.getConexao();
        if (conexao == null || conexao.socket.isClosed()) {
          System.out.println("[ELEICAO] Tentando conectar com servidor " + servidor.toString());
          try {
            Socket socket = new Socket(servidor.getIpMaquina(), servidor.getPortaTCP() + 1);
            ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
            saida.flush();
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            servidor.setConexao(new ConexaoPeer(socket, saida, entrada));
            System.out.println("[ELEICAO] Conexao estabelecida com servidor " + servidor.toString());
          } catch (Exception e) {
            System.out.println("[ELEICAO] Falha na conexao com servidor " + servidor + " - " + e.getMessage());
            continue;
          }
        }

        try {
          System.out.println("[ELEICAO] Enviando pedido para " + servidor.toString());
          String mensagem = minhasInformacoes.codificar(PREFIXO_ELEICAO);
          ConexaoPeer novaConexao = servidor.getConexao();
          novaConexao.saida.writeObject(mensagem);
          novaConexao.saida.flush();
          System.out.println("[ELEICAO] Pedido enviado para " + servidor.toString());

          Thread threadResposta = new Thread(() -> {
            processarMensagensEleicao(novaConexao.socket, novaConexao.saida, novaConexao.entrada);
          });

          threadResposta.start();
          threadResposta.join(5000);

          if (!recebeuRespostasOK) {
            System.out.println("[ELEICAO] Timeout aguardando resposta de " + servidor.toString());
          }
        } catch (Exception e) {
          System.out.println("[ELEICAO] Erro na comunicacao com " + servidor.toString() + ": " + e.getMessage());
        }
      }

      if (!recebeuRespostasOK) {
        minhasInformacoes.setEstado(EstadoPeer.ATIVO);
        servidorAtual.setInfoLider(minhasInformacoes);

        System.out.println("[LIDER] Me tornando lider e notificando outros servidores");
        notificarNovoLider();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void notificarNovoLider() {
    for (InfoPeer infoServidor : gerenciadorDeServidores.getListaDeServidores()) {
      try {
        if (infoServidor.getTimestampInicializacao() == minhasInformacoes.getTimestampInicializacao()) {
          continue;
        }

        if (infoServidor.getConexao() == null || infoServidor.getConexao().socket.isClosed()) {
          System.out.println("[LIDER] Estabelecendo conexao com " + infoServidor.toString());
          try {
            Socket socket = new Socket(infoServidor.getIpMaquina(), infoServidor.getPortaTCP() + 1);
            ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
            saida.flush();
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            infoServidor.setConexao(new ConexaoPeer(socket, saida, entrada));
            System.out.println("[LIDER] Conexao estabelecida com " + infoServidor.toString());
          } catch (Exception e) {
            System.out.println("[LIDER] Falha na conexao com " + infoServidor.toString() + ": " + e.getMessage());
            continue;
          }
        }

        String mensagemLider = PREFIXO_LIDER + minhasInformacoes.codificar("");
        infoServidor.getConexao().saida.writeObject(mensagemLider);
        infoServidor.getConexao().saida.flush();
        System.out.println("[LIDER] Notificacao enviada para " + infoServidor.toString());
      } catch (Exception e) {
        System.out.println("[LIDER] Erro ao notificar " + infoServidor.toString() + ": " + e.getMessage());
      }
    }

    try {
      for (String ipCliente : GerenciadorDeGrupos.getInstancia().getIpUsuarios()) {
        System.out.println("[LIDER] Notificando cliente " + ipCliente);
        Socket socket = new Socket(ipCliente, 6784);
        ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
        saida.writeObject(minhasInformacoes.codificar(PREFIXO_LIDER));
        saida.flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    gerenciadorDeServidores.gerenciarServidores();
    gerenciadorDeReplicacao.iniciarProcessoDeReplicacao();
  }

  public void enviarOK(ObjectOutputStream saida) {
    try {
      saida.writeObject(minhasInformacoes.codificar(PREFIXO_OK));
      saida.flush();
      System.out.println("[ELEICAO] OK enviado");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void receberPedidosDeEleicao() {
    while (true) {
      try {
        Socket socket = socketEleicao.accept();
        System.out.println("[CONEXAO] Nova conexao de: " + socket.getInetAddress());

        ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
        saida.flush();
        ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

        new Thread(() -> {
          processarMensagensEleicao(socket, saida, entrada);
        }).start();

      } catch (Exception e) {
        System.out.println("[CONEXAO] Erro ao aceitar conexao: " + e.getMessage());
      }
    }
  }

  private void processarMensagensEleicao(Socket socket, ObjectOutputStream saida, ObjectInputStream entrada) {
    try {
      while (!socket.isClosed()) {
        try {
          String mensagem = (String) entrada.readObject();
          System.out.println("[MENSAGEM] Recebida: " + mensagem);

          if (mensagem.startsWith(PREFIXO_OK)) {
            recebeuRespostasOK = true;
            System.out.println("[ELEICAO] Resposta OK recebida");
          } else if (mensagem.startsWith(PREFIXO_ELEICAO)) {
            new Thread(() -> tratarPedidoDeEleicao(mensagem, socket, saida, entrada)).start();
          } else if (mensagem.startsWith(PREFIXO_LIDER)) {
            new Thread(() -> tratarNotificacaoNovoLider(mensagem)).start();
          } else if (mensagem.startsWith("SINCRONIZACAO")) {
            gerenciadorDeReplicacao.processarSolicitacaoSincronizacao(mensagem, socket, entrada, saida);
          } else {
            System.out.println("[MENSAGEM] Formato desconhecido: " + mensagem);
          }
        } catch (Exception e) {
          if (socket.isClosed()) {
            System.out.println("[CONEXAO] Fechada");
          } else {
            System.out.println("[MENSAGEM] Erro ao processar: " + e.getMessage());
          }
          // break;
        }
      }
    } catch (Exception e) {
      System.out.println("[THREAD] Erro de processamento: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void tratarPedidoDeEleicao(String mensagem, Socket socket, ObjectOutputStream saida,
      ObjectInputStream entrada) {

    try {
      System.out.println("[ELEICAO] Pedido recebido: " + mensagem);

      enviarOK(saida);

      InfoPeer origem = InfoPeer.decodificar(mensagem);
      InfoPeer servidorNaLista = gerenciadorDeServidores.buscarServidorRegistrado(origem);

      if (servidorNaLista != null) {
        servidorNaLista.setConexao(new ConexaoPeer(socket, saida, entrada));
      } else {
        System.out.println("[ELEICAO] Servidor nao encontrado: " + origem);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void tratarNotificacaoNovoLider(String mensagem) {
    try {
      System.out.println("[LIDER] Notificacao recebida: " + mensagem);

      String infoLiderStr = mensagem.substring(PREFIXO_LIDER.length());
      InfoPeer lider = InfoPeer.decodificar(infoLiderStr);

      InfoPeer servidorNaLista = gerenciadorDeServidores.buscarServidorRegistrado(lider);

      if (servidorNaLista != null) {
        servidorNaLista.setEstado(EstadoPeer.ATIVO);

        servidorAtual.setInfoLider(servidorNaLista);
        System.out.println("[LIDER] Novo lider aceito: " + servidorNaLista);

        gerenciadorDeServidores.setTempoUltimoHeartbeatLider(System.currentTimeMillis());
      } else {
        System.out.println("[LIDER] Erro: Lider nao encontrado na lista de servidores");
      }
    } catch (Exception e) {
      System.out.println("[LIDER] Erro ao processar notificacao: " + e.getMessage());
      e.printStackTrace();
    }
  }
}