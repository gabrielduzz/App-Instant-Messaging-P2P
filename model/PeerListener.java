package model;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;

public class PeerListener {
  private static PeerListener servidor;
  private InfoPeer minhasInformacoes;
  private InfoPeer infoLider;

  private int portaUDP;
  private int portaTCP;
  private GerenciadorDeDescoberta gerenciadorDeDescoberta;
  private GerenciadorDePeers gerenciadorDeServidores;
  private GerenciadorDeComunicacao gerenciadorDeComunicacao;
  private GerenciadorDeReplicacao gerenciadorDeReplicacao;
  private GerenciadorDeGrupos gerenciadorDeGrupos;

  private PeerListener() {
  }

  public static PeerListener getInstancia() {
    if (servidor == null) {
      servidor = new PeerListener();
    }
    return servidor;
  }

  public InfoPeer getInfoLider() {
    return infoLider;
  }

  public void setInfoLider(InfoPeer infoLider) {
    this.infoLider = infoLider;
  }

  public void setMinhasInformacoes(InfoPeer minhasInformacoes) {
    this.minhasInformacoes = minhasInformacoes;
  }

  public void setPortaUDP(int portaUDP) {
    this.portaUDP = portaUDP;
  }

  public void setPortaTCP(int portaTCP) {
    this.portaTCP = portaTCP;
  }

  public InfoPeer getMinhasInformacoes() {
    return minhasInformacoes;
  }

  public int getPortaUDP() {
    return portaUDP;
  }

  public int getPortaTCP() {
    return portaTCP;
  }

  public void inicializar() {
    try {
      gerenciadorDeGrupos = GerenciadorDeGrupos.getInstancia();

      gerenciadorDeDescoberta = GerenciadorDeDescoberta.getInstancia();
      gerenciadorDeDescoberta.setSocketDescoberta(new DatagramSocket(portaUDP - 2));

      gerenciadorDeServidores = GerenciadorDePeers.getInstancia();
      gerenciadorDeServidores.setSocketHeartbeats(new DatagramSocket(portaUDP - 1));

      gerenciadorDeComunicacao = GerenciadorDeComunicacao.getInstancia();
      gerenciadorDeComunicacao.iniciarServidorTCP(portaTCP);
      gerenciadorDeComunicacao.iniciarServidorUDP(portaUDP);

      ArrayList<InfoPeer> listaServidores = gerenciadorDeDescoberta.descobrirServidores();
      gerenciadorDeServidores.setListaDeServidores(listaServidores);
      gerenciadorDeServidores.gerenciarServidores();

      gerenciadorDeReplicacao = GerenciadorDeReplicacao.getInstancia();
      gerenciadorDeReplicacao.setSocketReplicacao(new ServerSocket(portaTCP + 1));
      gerenciadorDeReplicacao.iniciarProcessoDeReplicacao();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
