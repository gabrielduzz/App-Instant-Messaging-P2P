package model;

public class InfoPeer {
  private String ipMaquina;
  private int portaUDP;
  private int portaTCP;
  private long timestampInicializacao;
  private EstadoPeer estado;
  private ConexaoPeer conexao = null;

  public ConexaoPeer getConexao() {
    return conexao;
  }

  public void setConexao(ConexaoPeer conexao) {
    this.conexao = conexao;
  }

  public int getPortaUDP() {
    return portaUDP;
  }

  public void setPortaUDP(int portaUDP) {
    this.portaUDP = portaUDP;
  }

  public int getPortaTCP() {
    return portaTCP;
  }

  public void setPortaTCP(int portaTCP) {
    this.portaTCP = portaTCP;
  }

  public String getIpMaquina() {
    return ipMaquina;
  }

  public long getTimestampInicializacao() {
    return timestampInicializacao;
  }

  public long getPrioridade() {
    return timestampInicializacao;
  }

  public EstadoPeer getEstado() {
    return estado;
  }

  public void setIpMaquina(String ipMaquina) {
    this.ipMaquina = ipMaquina;
  }

  public void setEstado(EstadoPeer estado) {
    this.estado = estado;
  }

  public void setTimestampInicializacao(long timestampInicializacao) {
    this.timestampInicializacao = timestampInicializacao;
  }

  public void setPrioridade(long timestampInicializacao) {
    this.timestampInicializacao = timestampInicializacao;
  }

  public InfoPeer(String ipMaquinaLocal, int portaUDP, long timestampInicializacao,
      EstadoPeer estado) {
    this.ipMaquina = ipMaquinaLocal;
    this.portaUDP = portaUDP;
    this.portaTCP = portaUDP + 1;
    this.timestampInicializacao = timestampInicializacao;
    this.estado = estado;
  }

  public String codificar(String tipo) {
    return tipo + "*" + ipMaquina + "*" + portaUDP + "*" + timestampInicializacao + "*" + estado;
  }

  public static InfoPeer decodificar(String dados) {
    String[] partes = dados.split("\\*");
    String tipo = partes[0];
    String ipMaquina = partes[1];
    int portaUDP = Integer.parseInt(partes[2]);
    long timestampInicializacao = Long.parseLong(partes[3]);
    EstadoPeer estado = null;
    switch (partes[4]) {
      case "DESCOBRINDO": {
        estado = EstadoPeer.DESCOBRINDO;
        break;
      }
      case "ATIVO": {
        estado = EstadoPeer.ATIVO;
        break;
      }
      case "STANDBY": {
        estado = EstadoPeer.STANDBY;
        break;
      }
    }

    return new InfoPeer(ipMaquina, portaUDP, timestampInicializacao, estado);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    InfoPeer infoServidor = (InfoPeer) obj;
    return ipMaquina.equals(infoServidor.ipMaquina) && portaUDP == infoServidor.portaUDP;
  }

  @Override
  public String toString() {
    return ipMaquina + " " + portaUDP + " " + timestampInicializacao + " " + estado;
  }
}
