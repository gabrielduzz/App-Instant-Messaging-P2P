package model;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GerenciadorDeReplicacao {
  private static final String PREFIXO_SINCRONIZACAO = "SINCRONIZACAO";
  private static final String PREFIXO_JOIN = "JOIN";
  private final String PREFIXO_ELEICAO = "ELEICAO";

  private static GerenciadorDeReplicacao instancia;
  private final PeerListener servidorAtual;
  private final InfoPeer minhasInformacoes;
  private final InfoPeer infoLider;
  private final GerenciadorDePeers gerenciadorDeServidores;
  private ServerSocket socketReplicacao;
  private Thread recebimentoDeSolicitacoesDeAtualizacao;
  private Thread solicitacaoEstadoAtualDoLog;

  private GerenciadorDeReplicacao() {
    this.servidorAtual = PeerListener.getInstancia();
    this.minhasInformacoes = servidorAtual.getMinhasInformacoes();
    this.infoLider = servidorAtual.getInfoLider();
    this.gerenciadorDeServidores = GerenciadorDePeers.getInstancia();
  }

  public static GerenciadorDeReplicacao getInstancia() {
    if (instancia == null) {
      instancia = new GerenciadorDeReplicacao();
    }
    return instancia;
  }

  public ServerSocket getSocketReplicacao() {
    return socketReplicacao;
  }

  public void setSocketReplicacao(ServerSocket socketReplicacao) {
    this.socketReplicacao = socketReplicacao;
  }

  public void iniciarProcessoDeReplicacao() {
    if (minhasInformacoes.getEstado() == EstadoPeer.ATIVO) {
      iniciarComoServidorAtivo();
    } else {
      iniciarComoServidorStandby();
    }
  }

  private void iniciarComoServidorAtivo() {
    System.out.println("[REPLICACAO] Aguardando solicitacoes de atualizacao");
    recebimentoDeSolicitacoesDeAtualizacao = new Thread(this::receberSolicitacoesDeAtualizacao);
    if (solicitacaoEstadoAtualDoLog != null) {
      solicitacaoEstadoAtualDoLog.interrupt();
    }
    recebimentoDeSolicitacoesDeAtualizacao.start();
  }

  private void iniciarComoServidorStandby() {
    System.out.println("[REPLICACAO] Solicitando estado atual do log");
    solicitacaoEstadoAtualDoLog = new Thread(this::solicitarEstadoAtualDoLog);
    solicitacaoEstadoAtualDoLog.start();
    new Thread(() -> GerenciadorDeEleicao.getInstancia().receberPedidosDeEleicao()).start();
  }

  private void receberSolicitacoesDeAtualizacao() {
    try {
      while (true) {
        Socket conexao = socketReplicacao.accept();
        System.out.println("[REPLICACAO] Servidor standby conectado");

        processarConexaoStandby(conexao);
      }
    } catch (Exception e) {
      System.out.println("[REPLICACAO] Erro ao receber solicitacoes: " + e.getMessage());
    }
  }

  private void processarConexaoStandby(Socket conexao) {
    try {
      ObjectInputStream entrada = new ObjectInputStream(conexao.getInputStream());
      ObjectOutputStream saida = new ObjectOutputStream(conexao.getOutputStream());
      saida.flush();

      String mensagem = (String) entrada.readObject();
      System.out.println("[REPLICACAO] Solicitacao recebida: " + mensagem);

      sincronizarEstadoDoLogComServidor(mensagem, conexao, entrada, saida);
    } catch (Exception e) {
      System.out.println("[REPLICACAO] Erro ao processar conexao standby: " + e.getMessage());
    }
  }

  private void sincronizarEstadoDoLogComServidor(String mensagem, Socket conexao, ObjectInputStream entrada,
      ObjectOutputStream saida) {
    try {
      System.out.println("[REPLICACAO] Mensagem recebida: " + mensagem);

      if (mensagem.startsWith(PREFIXO_SINCRONIZACAO)) {
        processarSolicitacaoSincronizacao(mensagem, conexao, entrada, saida);
      } else {
        System.out.println("[REPLICACAO] Tipo de mensagem desconhecido: " + mensagem);
      }
    } catch (Exception e) {
      System.out.println("[REPLICACAO] Erro na sincronizacao: " + e.getMessage());
    }
  }

  public void processarSolicitacaoSincronizacao(String mensagem, Socket conexao, ObjectInputStream entrada,
      ObjectOutputStream saida) {
    try {
      System.out.println("[SINCRONIZACAO] Processando solicitacao");
      String infoServidorStr = mensagem.substring(PREFIXO_SINCRONIZACAO.length());
      InfoPeer infoServidorSolicitante = InfoPeer.decodificar(infoServidorStr);

      InfoPeer infoServidorRegistrado = gerenciadorDeServidores.buscarServidorRegistrado(infoServidorSolicitante);

      if (infoServidorRegistrado != null) {
        registrarConexaoServidor(infoServidorRegistrado, conexao, saida, entrada);
        enviarListaDeGrupos(infoServidorRegistrado, saida);
      } else {
        System.out.println("[SINCRONIZACAO] Servidor nao encontrado: " + infoServidorSolicitante);
      }
    } catch (Exception e) {
      System.out.println("[SINCRONIZACAO] Erro no processamento: " + e.getMessage());
    }
  }

  private void registrarConexaoServidor(InfoPeer infoServidor, Socket conexao,
      ObjectOutputStream saida, ObjectInputStream entrada) {
    try {
      infoServidor.setConexao(new ConexaoPeer(conexao, saida, entrada));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void enviarListaDeGrupos(InfoPeer infoServidor, ObjectOutputStream saida) {
    try {
      System.out.println("[SINCRONIZACAO] Enviando lista de grupos para " + infoServidor);

      Map<String, List<String>> gruposUsuarios = GerenciadorDeGrupos.getGruposUsuarios();

      for (Entry<String, List<String>> entry : gruposUsuarios.entrySet()) {
        String nomeGrupo = entry.getKey();
        List<String> usuarios = entry.getValue();

        enviarUsuariosDeGrupo(nomeGrupo, usuarios, saida);
      }
    } catch (Exception e) {
      System.out.println("[SINCRONIZACAO] Erro ao enviar grupos: " + e.getMessage());
    }
  }

  private void enviarUsuariosDeGrupo(String nomeGrupo, List<String> usuarios, ObjectOutputStream saida) {
    try {
      for (String usuario : usuarios) {
        String mensagem = PREFIXO_JOIN + "*" + nomeGrupo + "*" + usuario
            + "*" + GerenciadorDeGrupos.getInstancia().getIpDoUsuario(usuario);
        saida.writeObject(mensagem);
        saida.flush();
      }
    } catch (Exception e) {
      System.out.println("[SINCRONIZACAO] Erro ao enviar usuarios do grupo: " + e.getMessage());
    }
  }

  private void solicitarEstadoAtualDoLog() {
    try {
      ConexaoPeer conexaoLider = conectarAoLider();

      if (conexaoLider != null) {
        enviarSolicitacaoSincronizacao(conexaoLider);
        receberAtualizacoesDoLog(conexaoLider.socket, conexaoLider.entrada);
      }
    } catch (Exception e) {
      System.out.println("[REPLICACAO] Erro ao solicitar estado do log: " + e.getMessage());
    }
  }

  private ConexaoPeer conectarAoLider() {
    try {
      int portaReplicacao = infoLider.getPortaTCP() + 1;
      System.out.println("[REPLICACAO] Conectando ao lider: " + infoLider.getIpMaquina() + ":" + portaReplicacao);

      Socket socket = new Socket(infoLider.getIpMaquina(), portaReplicacao);

      ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
      saida.flush();

      ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

      System.out.println("[REPLICACAO] Conexao estabelecida com lider");

      ConexaoPeer conexaoLider = new ConexaoPeer(socket, saida, entrada);
      infoLider.setConexao(conexaoLider);

      return conexaoLider;
    } catch (Exception e) {
      System.out.println("[REPLICACAO] Falha na conexao com lider: " + e.getMessage());
      return null;
    }
  }

  private void enviarSolicitacaoSincronizacao(ConexaoPeer conexaoLider) {
    try {
      String mensagem = minhasInformacoes.codificar(PREFIXO_SINCRONIZACAO);
      System.out.println("[SINCRONIZACAO] Enviando solicitacao: " + mensagem);

      conexaoLider.saida.writeObject(mensagem);
      conexaoLider.saida.flush();
    } catch (Exception e) {
      System.out.println("[SINCRONIZACAO] Erro no envio: " + e.getMessage());
    }
  }

  private void receberAtualizacoesDoLog(Socket socket, ObjectInputStream entrada) {
    try {
      while (!socket.isClosed()) {
        System.out.println("[SINCRONIZACAO] Aguardando resposta do lider");
        String resposta = (String) entrada.readObject();
        System.out.println("[SINCRONIZACAO] Resposta recebida: " + resposta);

        processarRespostaLider(resposta);
      }
    } catch (Exception e) {
      System.out.println("[SINCRONIZACAO] Erro ao receber atualizacoes: " + e.getMessage());
    }
  }

  private void processarRespostaLider(String resposta) {
    try {
      if (resposta.startsWith(PREFIXO_JOIN)) {
        System.out.println("[SINCRONIZACAO] Processando informacao de grupo");
        APDU join = APDU.decodificar(resposta);
        GerenciadorDeGrupos.getInstancia().adicionarUsuario(join.getGrupo(), join.getUsuario(), join.getMensagem());
      }
    } catch (Exception e) {
      System.out.println("[SINCRONIZACAO] Erro ao processar resposta: " + e.getMessage());
    }
  }

  public void enviarAtualizacaoParaServidores(String mensagemRecebida) {
    try {
      for (InfoPeer servidor : gerenciadorDeServidores.getListaDeServidores()) {
        System.out.println("[REPLICACAO] Enviando para " + servidor);
        servidor.getConexao().saida.writeObject(mensagemRecebida);
      }
    } catch (Exception e) {
      System.out.println("[REPLICACAO] Erro ao replicar para servidores: " + e.getMessage());
    }
  }
}