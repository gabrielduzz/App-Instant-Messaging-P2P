package model;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class GerenciadorDePeers {
  private static final int TEMPO_DE_TIMEOUT = 7000;
  private static final int INTERVALO_ENTRE_HEARTBEATS = 5000;
  private static final int TAMANHO_BUFFER = 1024;
  private static final String MENSAGEM_HEARTBEAT = "HEARTBEAT";

  private static GerenciadorDePeers instancia;
  private final PeerListener servidorAtual;
  private final InfoPeer minhasInformacoes;
  private InfoPeer infoLider;
  private ArrayList<InfoPeer> listaDeServidores;
  private Thread envioHeartbeats;
  private Thread monitoramentoFalhas;

  private DatagramSocket socketHeartbeats;
  private long tempoUltimoHeartbeatLider;

  private GerenciadorDePeers() {
    this.servidorAtual = PeerListener.getInstancia();
    this.minhasInformacoes = servidorAtual.getMinhasInformacoes();
    this.listaDeServidores = new ArrayList<>();
    this.tempoUltimoHeartbeatLider = System.currentTimeMillis();
    this.infoLider = servidorAtual.getInfoLider();
  }

  public static GerenciadorDePeers getInstancia() {
    if (instancia == null) {
      instancia = new GerenciadorDePeers();
    }
    return instancia;
  }

  public long getTempoUltimoHeartbeatLider() {
    return tempoUltimoHeartbeatLider;
  }

  public void setTempoUltimoHeartbeatLider(long tempoUltimoHeartbeatLider) {
    this.tempoUltimoHeartbeatLider = tempoUltimoHeartbeatLider;
  }

  public void setSocketHeartbeats(DatagramSocket socketHeartbeats) {
    this.socketHeartbeats = socketHeartbeats;
  }

  public void setListaDeServidores(ArrayList<InfoPeer> listaDeServidores) {
    this.listaDeServidores = listaDeServidores;
  }

  public ArrayList<InfoPeer> getListaDeServidores() {
    return listaDeServidores;
  }

  public void gerenciarServidores() {
    if (minhasInformacoes.getEstado() == EstadoPeer.ATIVO) {
      iniciarServidorAtivo();
    } else {
      iniciarServidorStandby();
    }
  }

  private void iniciarServidorAtivo() {
    envioHeartbeats = new Thread(this::enviarHeartbeatsPeriodicos);
    if (monitoramentoFalhas != null) {
      monitoramentoFalhas.interrupt();
    }
    envioHeartbeats.start();
  }

  private void iniciarServidorStandby() {
    monitoramentoFalhas = new Thread(this::monitorarFalhas);
    monitoramentoFalhas.start();
  }

  private void enviarHeartbeatsPeriodicos() {
    try {
      while (true) {
        enviarHeartbeatsParaTodos();
        System.out.println("[HEARTBEAT] Enviado para todos os servidores");
        Thread.sleep(INTERVALO_ENTRE_HEARTBEATS);
      }
    } catch (Exception e) {
      System.out.println("[HEARTBEAT] Erro no envio: " + e.getMessage());
    }
  }

  private void enviarHeartbeatsParaTodos() {
    try {
      byte[] dados = MENSAGEM_HEARTBEAT.getBytes();

      for (InfoPeer servidor : listaDeServidores) {
        InetAddress endereco = InetAddress.getByName(servidor.getIpMaquina());
        int porta = servidor.getPortaUDP() - 1;

        DatagramPacket pacote = new DatagramPacket(dados, dados.length, endereco, porta);
        socketHeartbeats.send(pacote);
      }
    } catch (Exception e) {
      System.out.println("[HEARTBEAT] Erro no envio para servidores: " + e.getMessage());
    }
  }

  private void monitorarFalhas() {
    new Thread(this::verificarTimeoutDoLider).start();
    receberHeartbeats();
  }

  private void receberHeartbeats() {
    try {
      byte[] dadosEntrada = new byte[TAMANHO_BUFFER];
      System.out.println("[MONITORAMENTO] Iniciado monitoramento de falhas");

      while (true) {
        DatagramPacket pacoteRecebido = new DatagramPacket(dadosEntrada, dadosEntrada.length);
        socketHeartbeats.receive(pacoteRecebido);

        if (ehPacoteProprio(pacoteRecebido)) {
          continue;
        }

        String mensagemRecebida = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());

        if (mensagemRecebida.contains("*")) {
          continue;
        }

        System.out.println("[HEARTBEAT] Recebido: " + mensagemRecebida);
        atualizarTempoUltimoHeartbeat();
      }
    } catch (Exception e) {
      System.out.println("[HEARTBEAT] Erro na recepcao: " + e.getMessage());
    }
  }

  private void atualizarTempoUltimoHeartbeat() {
    tempoUltimoHeartbeatLider = System.currentTimeMillis();
  }

  private void verificarTimeoutDoLider() {
    try {
      while (true) {
        Thread.sleep(TEMPO_DE_TIMEOUT);

        long tempoDecorrido = System.currentTimeMillis() - tempoUltimoHeartbeatLider;
        if (tempoDecorrido > TEMPO_DE_TIMEOUT) {
          System.out.println("[MONITORAMENTO] Falha detectada no lider");
          tratarFalhaDoLider();
          break;
        }
      }
    } catch (Exception e) {
      System.out.println("[MONITORAMENTO] Erro na verificacao de timeout: " + e.getMessage());
    }
  }

  private void tratarFalhaDoLider() {
    InfoPeer liderRemovido = null;
    for (InfoPeer servidor : listaDeServidores) {
      if (servidor.getEstado() == EstadoPeer.ATIVO) {
        liderRemovido = servidor;
        break;
      }
    }

    listaDeServidores.remove(liderRemovido);
    System.out.println("[ELEICAO] Iniciando processo de eleicao");

    iniciarEleicao();
  }

  private void iniciarEleicao() {
    try {
      GerenciadorDeEleicao.getInstancia().chamarEleicao();
    } catch (Exception e) {
      System.out.println("[ELEICAO] Erro ao iniciar: " + e.getMessage());
    }
  }

  public void adicionarNovoServidor(InfoPeer info) {
    InfoPeer servidorNaLista = buscarServidorRegistrado(info);
    if (servidorNaLista != null) {
      listaDeServidores.remove(servidorNaLista);
    }
    try {
      listaDeServidores.add(info);
    } catch (Exception e) {
      e.printStackTrace();
    }

    for (InfoPeer infos : listaDeServidores) {
      System.out.println("[SERVIDOR] Registrado: " + infos.toString());
    }
  }

  public InfoPeer buscarServidorRegistrado(InfoPeer infoServidor) {
    int indexServidor = listaDeServidores.indexOf(infoServidor);
    if (indexServidor >= 0) {
      return listaDeServidores.get(indexServidor);
    }
    return null;
  }

  private boolean ehPacoteProprio(DatagramPacket pacote) {
    return pacote.getPort() == socketHeartbeats.getPort() &&
        pacote.getAddress().getHostAddress().equals(minhasInformacoes.getIpMaquina());
  }
}