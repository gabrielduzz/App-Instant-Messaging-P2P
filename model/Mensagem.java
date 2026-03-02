package model;

import java.time.LocalDateTime;

public class Mensagem {
  private String nomeUsuario;
  private String textoMensagem;
  private LocalDateTime hora;

  public Mensagem(String nomeUsuario, String textoMensagem, LocalDateTime hora) {
    this.nomeUsuario = nomeUsuario;
    this.textoMensagem = textoMensagem;
    this.hora = hora;
  }

  public String getNomeUsuario() {
    return nomeUsuario;
  }

  public String getTextoMensagem() {
    return textoMensagem;
  }

  public LocalDateTime getHora() {
    return hora;
  }
}
