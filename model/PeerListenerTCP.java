package model;

import java.net.*;
import java.io.*;

public class PeerListenerTCP extends Thread {
  private int portaTCP;
  private InfoPeer minhasInformacoes;

  public void setMinhasInformacoes(InfoPeer minhasInformacoes) {
    this.minhasInformacoes = minhasInformacoes;
  }

  public void setPortaTCP(int portaTCP) {
    this.portaTCP = portaTCP;
  }

  public void run() {
    System.out.println("[TCP] Iniciando servidor na porta " + portaTCP);
    try {
      ServerSocket servidor = new ServerSocket(portaTCP);
      System.out.println("[TCP] Servidor escutando na porta " + portaTCP);

      while (true) {
        try {
          Socket conexao = servidor.accept();
          System.out.println("[CONEXAO] Cliente conectado: " + conexao.getInetAddress().getHostAddress());

          PeerHandler clienteHandler = new PeerHandler(conexao);
          clienteHandler.start();

        } catch (IOException e) {
          System.out.println("[ERRO] Falha ao aceitar conexao: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      System.out.println("[ERRO] Falha ao iniciar servidor: " + e.getMessage());
    }
  }
}