package model;

import java.util.ArrayList;

public class GerenciadorDeComunicacao {
  private final PeerListener servidorAtual;
  private final InfoPeer minhasInformacoes;
  private static GerenciadorDeComunicacao instancia;

  private GerenciadorDeComunicacao() {
    this.servidorAtual = PeerListener.getInstancia();
    this.minhasInformacoes = servidorAtual.getMinhasInformacoes();
  }

  public static GerenciadorDeComunicacao getInstancia() {
    if (instancia == null) {
      instancia = new GerenciadorDeComunicacao();
    }
    return instancia;
  }

  public void iniciarServidorUDP(int portaUDP) {
    PeerListenerUDP servidorUDP = new PeerListenerUDP();
    servidorUDP.setPortaUDP(portaUDP);
    servidorUDP.start();
  }

  public void iniciarServidorTCP(int portaTCP) {
    PeerListenerTCP servidorTCP = new PeerListenerTCP();
    servidorTCP.setPortaTCP(portaTCP);
    servidorTCP.start();
  }
}
