package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConexaoPeer {
  Socket socket;
  ObjectOutputStream saida;
  ObjectInputStream entrada;

  public ConexaoPeer(Socket socket, ObjectOutputStream saida, ObjectInputStream entrada) throws IOException {
    this.socket = socket;
    this.saida = saida;
    this.saida.flush();
    this.entrada = entrada;
  }

  public void fechar() {
    try {
      if (saida != null) {
        saida.close();
      }
      if (entrada != null) {
        entrada.close();
      }
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      System.out.println("[ERRO] Erro ao fechar conexao: " + e.getMessage());
    }
  }
}
