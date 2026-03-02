package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GerenciadorDeGrupos {
  private static Map<String, List<String>> gruposUsuarios;

  private ArrayList<String> ipUsuarios;

  private static Map<String, String> ipPorUsuario;
  private static Map<String, String> usuarioPorIp;

  private static GerenciadorDeGrupos instancia = null;

  private GerenciadorDeGrupos() {
    ipUsuarios = new ArrayList<>();
    ipPorUsuario = new HashMap<>();
    usuarioPorIp = new HashMap<>();
    gruposUsuarios = new HashMap<>();
  }

  public ArrayList<String> getIpUsuarios() {
    return ipUsuarios;
  }

  public void setIpUsuarios(ArrayList<String> ipUsuarios) {
    this.ipUsuarios = ipUsuarios;
  }

  public static Map<String, String> getIpPorUsuario() {
    return ipPorUsuario;
  }

  public static Map<String, String> getUsuarioPorIp() {
    return usuarioPorIp;
  }

  public static List<String> getUsuariosDoGrupo(String grupo) {
    return gruposUsuarios.get(grupo);
  }

  public static Map<String, List<String>> getGruposUsuarios() {
    return gruposUsuarios;
  }

  public String getIpDoUsuario(String usuario) {
    return ipPorUsuario.get(usuario);
  }

  public static GerenciadorDeGrupos getInstancia() {
    if (instancia == null) {
      instancia = new GerenciadorDeGrupos();
    }
    return instancia;
  }

  public synchronized void adicionarUsuario(String grupo, String usuario, String ipUsuario) {
    if (!gruposUsuarios.keySet().contains(grupo)) {
      gruposUsuarios.put(grupo, new ArrayList<String>());
    }

    gruposUsuarios.get(grupo).add(usuario);

    if (!ipUsuarios.contains(ipUsuario)) {
      ipUsuarios.add(ipUsuario);
      ipPorUsuario.putIfAbsent(usuario, ipUsuario);
      usuarioPorIp.putIfAbsent(ipUsuario, usuario);
    }
  }

  public synchronized void removerUsuario(String grupo, String usuario, String ipUsuario) {
    List<String> usuarios = gruposUsuarios.get(grupo);
    ipPorUsuario.remove(usuario);
    usuarioPorIp.remove(ipUsuario);
    usuarios.remove(usuario);
    ipUsuarios.remove(ipUsuario);
    if (usuarios.isEmpty()) {
      gruposUsuarios.remove(grupo);
    }
  }

  public synchronized void removerUsuarioDeTodosOsGrupos(String usuario) {
    for (String grupo : gruposUsuarios.keySet()) {
      gruposUsuarios.get(grupo).remove(usuario);
    }
  }

  public boolean usuarioJaConectado(String ipUsuario) {
    String usuario = usuarioPorIp.get(ipUsuario);
    for (String grupo : gruposUsuarios.keySet()) {
      if (gruposUsuarios.get(grupo).contains(usuario)) {
        return true;
      }
    }
    return false;
  }
}
