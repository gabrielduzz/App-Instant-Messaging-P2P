package model;

import java.net.*;
import java.io.*;

public class PeerListenerUDP extends Thread {
  private int portaUDP;
  private static PeerListenerUDP servidorUDP;
  private InfoPeer minhasInformacoes;

  public void setMinhasInformacoes(InfoPeer minhasInformacoes) {
    this.minhasInformacoes = minhasInformacoes;
  }

  public void setPortaUDP(int portaUDP) {
    this.portaUDP = portaUDP;
  }

  public void run() {
    System.out.println("[UDP] Iniciando servidor na porta " + portaUDP);
    try {
      DatagramSocket servidor = new DatagramSocket(portaUDP);
      System.out.println("[UDP] Servidor escutando na porta " + portaUDP);

      byte[] dadosEntrada = new byte[1024];

      while (true) {
        DatagramPacket pacoteRecebido = new DatagramPacket(dadosEntrada, dadosEntrada.length);

        servidor.receive(pacoteRecebido);

        String mensagemRecebida = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());

        new Thread(() -> processarMensagem(mensagemRecebida)).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void processarMensagem(String mensagemRecebida) {
    System.out.println("[MENSAGEM] Recebida via UDP: " + mensagemRecebida);
    APDU mensagemAPDU = APDU.decodificar(mensagemRecebida);

    System.out.println("[MENSAGEM] Dados - Tipo: " + mensagemAPDU.getTipo() +
        " | Grupo: " + mensagemAPDU.getGrupo() +
        " | Usuario: " + mensagemAPDU.getUsuario());

    String grupo = mensagemAPDU.getGrupo();
    GerenciadorDeGrupos gerenciadorDeGrupos = GerenciadorDeGrupos.getInstancia();

    for (String usuario : GerenciadorDeGrupos.getUsuariosDoGrupo(grupo)) {
      if (usuario.equals(mensagemAPDU.getUsuario())) {
        continue;
      }
      String ipDestino = gerenciadorDeGrupos.getIpDoUsuario(usuario);
      enviarMensagem(ipDestino, mensagemAPDU);
    }
  }

  public void enviarMensagem(String ipDestino, APDU mensagemAPDU) {
    try {
      DatagramSocket socket = new DatagramSocket();
      InetAddress enderecoIPServidor = InetAddress.getByName(ipDestino);
      byte[] saida = new byte[1024];
      String mensagemEnviada = mensagemAPDU.codificar();
      saida = mensagemEnviada.getBytes();
      DatagramPacket pacoteEnviado = new DatagramPacket(saida, saida.length, enderecoIPServidor, 6781);
      socket.send(pacoteEnviado);
      System.out.println("[UDP] Mensagem enviada para " + ipDestino);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}