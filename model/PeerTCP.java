package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class PeerTCP extends Thread {
  private MensagemAPDU mensagemAPDU;
  private String host;
  private Socket socket;
  private ObjectOutputStream saida;
  private ServerSocket socketLider;
  private int portaTCP = 6786;
  private volatile boolean conectado = true;

  public PeerTCP() {
    host = Usuario.ipServidor;
    try {
      criarConexao();
      socketLider = new ServerSocket(6784);
      new Thread(() -> receberNotificacaoDeNovoLider()).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void criarConexao() {
    try {
      if (socket != null && !socket.isClosed()) {
        try {
          socket.close();
        } catch (IOException e) {
        }
      }

      socket = new Socket(host, portaTCP);
      saida = new ObjectOutputStream(socket.getOutputStream());
      System.out.println("[CONEXAO] Conectado ao servidor: " + host + ":" + portaTCP);
    } catch (IOException e) {
      e.printStackTrace();
      tentarReconectar();
    }
  }

  private void tentarReconectar() {
    System.out.println("[CONEXAO] Tentando reconectar ao servidor");
    try {
      Thread.sleep(2000);
      criarConexao();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void setMensagemAPDU(MensagemAPDU mensagemAPDU) {
    this.mensagemAPDU = mensagemAPDU;
  }

  public void run() {
    try {
      String mensagem = mensagemAPDU.codificar();
      saida.writeObject(mensagem);
      saida.flush();
    } catch (IOException e) {
      e.printStackTrace();
      tentarReconectar();
    }
  }

  private void receberNotificacaoDeNovoLider() {
    while (true) {
      Socket conexao = null;
      ObjectInputStream entrada = null;

      try {
        conexao = socketLider.accept();
        entrada = new ObjectInputStream(conexao.getInputStream());

        String mensagem = (String) entrada.readObject();
        System.out.println("[LIDER] Notificacao recebida: " + mensagem);

        String[] partes = mensagem.split("\\*");

        if (partes.length >= 3) {
          Usuario.ipServidor = partes[1];
          host = Usuario.ipServidor;
          portaTCP = Integer.parseInt(partes[2]) + 1;
          PeerUDP.setIpServidor(host);
          PeerUDP.setPortaUDP(portaTCP - 1);
          fecharRecursos();
          criarConexao();
        }
      } catch (SocketException se) {
        System.out.println("[ERRO] Conexao: " + se.getMessage());
      } catch (IOException e) {
        System.out.println("[ERRO] IO: " + e.getMessage());
      } catch (ClassNotFoundException e) {
        System.out.println("[ERRO] Classe nao encontrada: " + e.getMessage());
      } finally {
        try {
          if (entrada != null)
            entrada.close();
          if (conexao != null && !conexao.isClosed())
            conexao.close();
        } catch (IOException e) {
          System.out.println("[ERRO] Ao fechar recursos: " + e.getMessage());
        }
      }
    }
  }

  private void fecharRecursos() {
    try {
      if (saida != null) {
        saida.close();
        saida = null;
      }
      if (socket != null) {
        socket.close();
        socket = null;
      }
    } catch (IOException e) {
      System.out.println("[ERRO] Ao fechar recursos: " + e.getMessage());
    }
  }
}