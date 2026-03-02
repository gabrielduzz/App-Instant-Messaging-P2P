package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Grupo {
  private String nome;
  private String ultimaMensagem;
  private int numeroDeMensagensNaoLidas;
  private List<Mensagem> listaDeMensagens;
  private static Map<String, Grupo> grupoPorNome = new HashMap<>();

  public Grupo(String nome) {
    this.nome = nome;
    grupoPorNome.put(nome, this);
    listaDeMensagens = new ArrayList<>();
  }

  public static Grupo getGrupoPorNome(String nomeGrupo) {
    return grupoPorNome.get(nomeGrupo);
  }

  public String getNome() {
    return nome;
  }

  public String getUltimaMensagem() {
    return ultimaMensagem;
  }

  public int getNumeroDeMensagensNaoLidas() {
    return numeroDeMensagensNaoLidas;
  }

  public List<Mensagem> getListaDeMensagens() {
    return listaDeMensagens;
  }

  public void aumentarNumeroDeMensagensNaoLidas() {
    numeroDeMensagensNaoLidas++;
  }

  public void zerarNumeroDeMensagensNaoLidas() {
    numeroDeMensagensNaoLidas = 0;
  }

}
