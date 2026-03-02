package model;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

public class PeerHandler extends Thread {
  private Socket conexao = null;
  private String ipUsuario;

  public PeerHandler(Socket conexao) {
    this.conexao = conexao;
  }

  public void run() {
    try {
      ObjectInputStream entrada = new ObjectInputStream(conexao.getInputStream());
      Map<String, String> ipPorUsuario = GerenciadorDeGrupos.getIpPorUsuario();
      Map<String, String> usuarioPorIp = GerenciadorDeGrupos.getUsuarioPorIp();

      while (!conexao.isClosed()) {
        try {
          String mensagem = (String) entrada.readObject();
          if (mensagem == null)
            break;
          System.out.println("[MENSAGEM] Recebida: " + mensagem);

          if (PeerListener.getInstancia().getMinhasInformacoes().getEstado() == EstadoPeer.ATIVO) {
            GerenciadorDeReplicacao.getInstancia().enviarAtualizacaoParaServidores(mensagem);
          }

          APDU mensagemAPDU = APDU.decodificar(mensagem);

          String tipo = mensagemAPDU.getTipo();
          String usuario = mensagemAPDU.getUsuario();
          String grupo = mensagemAPDU.getGrupo();
          System.out.println("[CLIENTE] Tipo: " + tipo + " | Usuario: " + usuario + " | Grupo: " + grupo);

          String ipCliente = mensagemAPDU.getMensagem();
          GerenciadorDeGrupos gerenciador = GerenciadorDeGrupos.getInstancia();
          if (gerenciador.usuarioJaConectado(ipCliente)) {
            gerenciador.removerUsuarioDeTodosOsGrupos(ipCliente);
            System.out.println("[CLIENTE] Usuario antigo removido: " + ipCliente);
          }
          this.ipUsuario = ipCliente;
          System.out.println("[CLIENTE] IP: " + ipCliente);

          switch (tipo) {
            case "JOIN": {
              processarJoin(grupo, usuario, ipCliente);
              break;
            }

            case "LEAVE": {
              processarLeave(grupo, usuario, ipCliente);
              break;
            }

            default:
              break;
          }

        } catch (EOFException e) {
          System.out.println("[CONEXAO] Encerrada pelo cliente: " + ipUsuario);
          break;
        } catch (SocketException e) {
          if ("Connection reset".equals(e.getMessage())) {
            System.out.println("[CONEXAO] Resetada pelo cliente: " + ipUsuario);
            break;
          } else {
            System.out.println("[ERRO] Socket: " + e.getMessage());
          }
        } catch (IOException e) {
          System.out.println("[ERRO] E/S: " + e.getMessage());
          break;
        } catch (Exception e) {
          System.out.println("[ERRO] Inesperado: " + e.getMessage());
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (!conexao.isClosed()) {
          conexao.close();
        }
        String usuarioDesconectado = GerenciadorDeGrupos.getUsuarioPorIp().get(ipUsuario);
        if (usuarioDesconectado != null) {
          GerenciadorDeGrupos.getInstancia().removerUsuarioDeTodosOsGrupos(usuarioDesconectado);
          GerenciadorDeGrupos.getIpPorUsuario().remove(usuarioDesconectado);
          GerenciadorDeGrupos.getUsuarioPorIp().remove(ipUsuario);
          System.out.println("[CLIENTE] Desconectado e removido: " + usuarioDesconectado);
        }
      } catch (Exception e) {
        System.out.println("[ERRO] Ao finalizar conexao: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public void processarJoin(String grupo, String usuario, String ipUsuario) {
    GerenciadorDeGrupos.getInstancia().adicionarUsuario(grupo, usuario, ipUsuario);
  }

  public void processarLeave(String grupo, String usuario, String ipUsuario) {
    GerenciadorDeGrupos.getInstancia().removerUsuario(grupo, usuario, ipUsuario);
  }
}