package model;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
  public static String nome;
  public static String ipServidor;
  public static String ipCliente;
  public static List<Grupo> grupos;
  public static PeerTCP clienteTCP;
  public static PeerUDP clienteUDP;

  public Usuario(String nome, String ipServidor, String ipCliente) {
    Usuario.nome = nome;
    Usuario.ipServidor = ipServidor;
    Usuario.ipCliente = ipCliente;
    grupos = new ArrayList<>();
    clienteTCP = new PeerTCP();
    clienteUDP = new PeerUDP();
    clienteTCP.setDaemon(true);
    clienteUDP.setDaemon(true);
  }
}