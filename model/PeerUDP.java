package model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;

import controller.ControllerChat;
import controller.ControllerGrupos;
import javafx.application.Platform;

public class PeerUDP extends Thread {
  private MensagemAPDU mensagemAPDU;
  private static String ipServidor = Usuario.ipServidor;

  private static int portaUDP = 6785;

  public static void setIpServidor(String ipServidor) {
    PeerUDP.ipServidor = ipServidor;
  }

  public void run() {
    try {
      DatagramSocket socket = new DatagramSocket();
      InetAddress enderecoIPServidor = InetAddress.getByName(ipServidor);
      byte[] saida = new byte[1024];

      String mensagemEnviada = new String(mensagemAPDU.codificar());
      saida = mensagemEnviada.getBytes();
      DatagramPacket pacoteEnviado = new DatagramPacket(saida, saida.length,
          enderecoIPServidor, portaUDP);
      socket.send(pacoteEnviado);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static int getPortaUDP() {
    return portaUDP;
  }

  public static void setPortaUDP(int portaUDP) {
    PeerUDP.portaUDP = portaUDP;
  }

  public void setMensagemAPDU(MensagemAPDU mensagemAPDU) {
    this.mensagemAPDU = mensagemAPDU;
  }

  public void receberMensagem() {
    try {
      DatagramSocket socket = new DatagramSocket(6781);
      byte[] dadosEntrada = new byte[1024];

      while (true) {
        DatagramPacket pacoteRecebido = new DatagramPacket(dadosEntrada, dadosEntrada.length);
        try {
          socket.receive(pacoteRecebido);
        } catch (IOException e) {
          e.printStackTrace();
        }

        String mensagemRecebida = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
        new Thread(() -> processarMensagem(mensagemRecebida)).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void processarMensagem(String mensagemRecebida) {
    MensagemAPDU mensagemAPDU = MensagemAPDU.decodificar(mensagemRecebida);

    Mensagem mensagem = new Mensagem(mensagemAPDU.getUsuario(), mensagemAPDU.getMensagem(), LocalDateTime.now());
    String nomeGrupo = mensagemAPDU.getGrupo();
    Grupo grupo = Grupo.getGrupoPorNome(nomeGrupo);
    grupo.aumentarNumeroDeMensagensNaoLidas();
    grupo.getListaDeMensagens().add(mensagem);

    Platform.runLater(() -> {
      ControllerChat.getInstancia().exibirMensagens();
      ControllerGrupos.getInstancia().listarGruposDoUsuario(Usuario.grupos);
    });

  }
}
