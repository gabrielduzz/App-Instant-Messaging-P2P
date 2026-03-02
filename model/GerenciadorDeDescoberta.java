package model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

public class GerenciadorDeDescoberta {
  private static GerenciadorDeDescoberta instancia;
  private final PeerListener servidorAtual;
  private final InfoPeer minhasInformacoes;
  private DatagramSocket socketDescoberta;
  private InetAddress enderecoBroadcast;
  private static final int TEMPO_DE_TIMEOUT = 5000;
  private static final int TAMANHO_BUFFER = 1024;
  private static final int[] PORTAS_BROADCAST = { 6783, 6788, 6793 };

  private GerenciadorDeDescoberta() {
    this.servidorAtual = PeerListener.getInstancia();
    this.minhasInformacoes = servidorAtual.getMinhasInformacoes();
  }

  public static GerenciadorDeDescoberta getInstancia() {
    if (instancia == null) {
      instancia = new GerenciadorDeDescoberta();
    }
    return instancia;
  }

  public InetAddress getEnderecoBroadcast() {
    return enderecoBroadcast;
  }

  public DatagramSocket getSocketDescoberta() {
    return socketDescoberta;
  }

  public void setSocketDescoberta(DatagramSocket socketDescoberta) {
    this.socketDescoberta = socketDescoberta;
  }

  public ArrayList<InfoPeer> descobrirServidores() {
    ArrayList<InfoPeer> servidoresEncontrados = new ArrayList<>();

    try {
      List<InetAddress> enderecosBroadcast = listarEnderecosDeBroadcast();
      if (!enderecosBroadcast.isEmpty()) {
        enviarBroadcast(minhasInformacoes.codificar("INFORMACAO"), enderecosBroadcast.get(0));
        aguardarRespostas(servidoresEncontrados);
      }
    } catch (Exception e) {
      System.out.println("[DESCOBERTA] Erro ao descobrir servidores: " + e.getMessage());
    }

    definirEstadoServidor(servidoresEncontrados);
    iniciarRecepcaoMensagensDescoberta();

    return servidoresEncontrados;
  }

  private void aguardarRespostas(ArrayList<InfoPeer> servidoresEncontrados) {
    try {
      socketDescoberta.setSoTimeout(TEMPO_DE_TIMEOUT);
      byte[] dadosEntrada = new byte[TAMANHO_BUFFER];

      while (true) {
        DatagramPacket pacoteRecebido = new DatagramPacket(dadosEntrada, dadosEntrada.length);
        socketDescoberta.receive(pacoteRecebido);

        processarPacoteRecebido(pacoteRecebido, servidoresEncontrados);
      }
    } catch (Exception e) {
      System.out.println("[DESCOBERTA] Processo de descoberta finalizado");
      try {
        socketDescoberta.setSoTimeout(0);
        for (InfoPeer servidor : servidoresEncontrados) {
          System.out.println("[SERVIDOR] " + servidor.toString());
          if (servidor.getEstado() == EstadoPeer.DESCOBRINDO) {
            String mensagemEnviada = minhasInformacoes.codificar("INFORMACAO");
            byte[] dados = mensagemEnviada.getBytes();

            DatagramPacket pacote = new DatagramPacket(dados, dados.length,
                InetAddress.getByName(servidor.getIpMaquina()), servidor.getPortaUDP() - 2);
            socketDescoberta.send(pacote);

            byte[] dadosEntrada = new byte[TAMANHO_BUFFER];

            DatagramPacket pacoteRecebido = new DatagramPacket(dadosEntrada, dadosEntrada.length);
            socketDescoberta.receive(pacoteRecebido);

            String mensagemRecebida = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
            System.out.println("[DESCOBERTA] Resposta recebida: " + mensagemRecebida);

            processarMensagemRecebida(mensagemRecebida, pacoteRecebido.getAddress(), pacoteRecebido.getPort());
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  private void processarPacoteRecebido(DatagramPacket pacote, ArrayList<InfoPeer> servidores) {
    String mensagemRecebida = new String(pacote.getData(), 0, pacote.getLength());

    if (ehPacoteProprio(pacote)) {
      return;
    }

    if (!mensagemRecebida.contains("*")) {
      return;
    }

    InfoPeer info = InfoPeer.decodificar(mensagemRecebida);

    InfoPeer servidorNaLista = GerenciadorDePeers.getInstancia().buscarServidorRegistrado(info);
    if (servidorNaLista != null) {
      GerenciadorDePeers.getInstancia().getListaDeServidores().remove(servidorNaLista);
    }

    if (info.getEstado() == EstadoPeer.ATIVO) {
      servidorAtual.setInfoLider(info);
    }

    servidores.add(info);
    System.out.println("[DESCOBERTA] Servidor encontrado: " + info);
  }

  private void definirEstadoServidor(ArrayList<InfoPeer> servidoresEncontrados) {
    if (servidoresEncontrados.isEmpty()) {
      minhasInformacoes.setEstado(EstadoPeer.ATIVO);
      System.out.println("[SERVIDOR] Tornando-me servidor lider");
    } else {
      minhasInformacoes.setEstado(EstadoPeer.STANDBY);
      System.out.println("[SERVIDOR] Modo standby ativado");
    }
  }

  private void iniciarRecepcaoMensagensDescoberta() {
    new Thread(this::receberMensagensDescoberta).start();
  }

  private void receberMensagensDescoberta() {
    try {
      byte[] dadosEntrada = new byte[TAMANHO_BUFFER];

      while (true) {
        DatagramPacket pacoteRecebido = new DatagramPacket(dadosEntrada, dadosEntrada.length);
        socketDescoberta.receive(pacoteRecebido);

        if (ehPacoteProprio(pacoteRecebido)) {
          continue;
        }

        String mensagemRecebida = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
        System.out.println("[DESCOBERTA] Mensagem recebida: " + mensagemRecebida);

        processarMensagemRecebida(mensagemRecebida, pacoteRecebido.getAddress(), pacoteRecebido.getPort());
      }
    } catch (Exception e) {
      System.out.println("[DESCOBERTA] Erro na recepcao: " + e.getMessage());
    }
  }

  private void processarMensagemRecebida(String mensagem, InetAddress ipOrigem, int portaOrigem) {
    if (mensagem.equals("CLIENTE") && minhasInformacoes.getEstado() == EstadoPeer.ATIVO) {
      enviarRespostaComMinhasInformacoes(ipOrigem, portaOrigem);
      return;
    }

    if (mensagem.contains("*")) {
      InfoPeer informacaoServidor = InfoPeer.decodificar(mensagem);

      if (mensagem.startsWith("INFORMACAO")) {
        try {
          informacaoServidor.setEstado(EstadoPeer.STANDBY);
          GerenciadorDePeers.getInstancia().adicionarNovoServidor(informacaoServidor);
          System.out.println("[DESCOBERTA] Enviando resposta ao servidor");
          String mensagemEnviada = minhasInformacoes.codificar("RESPOSTA");
          byte[] dados = mensagemEnviada.getBytes();

          DatagramPacket pacote = new DatagramPacket(dados, dados.length,
              InetAddress.getByName(informacaoServidor.getIpMaquina()), informacaoServidor.getPortaUDP() - 2);
          socketDescoberta.send(pacote);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return;
      }

      if (mensagem.startsWith("RESPOSTA")) {
        if (informacaoServidor.getEstado() == EstadoPeer.ATIVO) {
          servidorAtual.setInfoLider(informacaoServidor);
        }
        return;
      }

      informacaoServidor.setEstado(EstadoPeer.STANDBY);
      GerenciadorDePeers.getInstancia().adicionarNovoServidor(informacaoServidor);
      enviarRespostaComMinhasInformacoes(ipOrigem, portaOrigem);
    }
  }

  private boolean ehPacoteProprio(DatagramPacket pacote) {
    return pacote.getPort() == socketDescoberta.getLocalPort() &&
        pacote.getAddress().getHostAddress().equals(minhasInformacoes.getIpMaquina());
  }

  public void enviarRespostaComMinhasInformacoes(InetAddress ipDestino, int portaDestino) {
    try {
      String mensagemEnviada = minhasInformacoes.codificar("INFORMACAO");
      byte[] dados = mensagemEnviada.getBytes();

      DatagramPacket pacote = new DatagramPacket(dados, dados.length, ipDestino, portaDestino);
      socketDescoberta.send(pacote);

      System.out.println("[DESCOBERTA] Resposta enviada para: " + ipDestino + ":" + portaDestino);
    } catch (IOException e) {
      System.out.println("[DESCOBERTA] Erro ao enviar resposta: " + e.getMessage());
    }
  }

  private void enviarBroadcast(String mensagem, InetAddress enderecoBroadcast) {
    try {
      socketDescoberta.setBroadcast(true);
      byte[] dados = mensagem.getBytes();

      for (int porta : PORTAS_BROADCAST) {
        DatagramPacket pacote = new DatagramPacket(dados, dados.length, enderecoBroadcast, porta);
        socketDescoberta.send(pacote);
      }
    } catch (IOException e) {
      System.out.println("[DESCOBERTA] Erro no broadcast: " + e.getMessage());
    }
  }

  private List<InetAddress> listarEnderecosDeBroadcast() throws SocketException {
    List<InetAddress> enderecosBroadcast = new ArrayList<>();
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

    while (interfaces.hasMoreElements()) {
      NetworkInterface interfaceRede = interfaces.nextElement();

      if (interfaceRede.isLoopback() || !interfaceRede.isUp()) {
        continue;
      }

      interfaceRede.getInterfaceAddresses().stream()
          .map(endereco -> endereco.getBroadcast())
          .filter(Objects::nonNull)
          .forEach(enderecosBroadcast::add);
    }

    enderecoBroadcast = enderecosBroadcast.get(0);
    return enderecosBroadcast;
  }
}