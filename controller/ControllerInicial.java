package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import model.Usuario;

public class ControllerInicial implements Initializable {

  @FXML
  private TextField campoDeTextoNome;

  @FXML
  private TextField campoDeTextoIp;

  @FXML
  private ImageView botaoIniciar;

  private static ControllerInicial instancia;
  private Usuario usuario;
  private DatagramSocket socketDescoberta;

  private static final int PORTA_SOCKET = 6782;
  private static final int TEMPO_DE_TIMEOUT = 5000;
  private static final int TAMANHO_BUFFER = 1024;
  private static final int[] PORTAS_BROADCAST = { 6783, 6788, 6793 };
  private static final String MENSAGEM_CLIENTE = "CLIENTE";
  private static final String ESTILO_BOTAO = "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.2), 10, 0, 0, 2); -fx-cursor: hand;";

  private ControllerInicial() {
    inicializarSocket();
  }

  private void inicializarSocket() {
    try {
      socketDescoberta = new DatagramSocket(PORTA_SOCKET);
    } catch (Exception e) {
      System.out.println("[SOCKET] Erro ao inicializar: " + e.getMessage());
    }
  }

  public static ControllerInicial getInstancia() {
    if (instancia == null) {
      instancia = new ControllerInicial();
    }
    return instancia;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  @Override
  public void initialize(URL arg0, ResourceBundle rb) {
    configurarEstiloBotao();
  }

  private void configurarEstiloBotao() {
    botaoIniciar.setOnMouseEntered(e -> botaoIniciar.setStyle(ESTILO_BOTAO));
  }

  public boolean verificarCamposDeTexto() {
    String nome = campoDeTextoNome.getText().trim();
    String ipServidor = descobrirServidorAtivo();

    if (nome.isEmpty()) {
      System.out.println("[VALIDACAO] Nome nao informado.");
      return false;
    }

    usuario = new Usuario(nome, ipServidor, obterIPMaquina());
    return true;
  }

  public String descobrirServidorAtivo() {
    String ipServidor = null;
    try {
      List<InetAddress> enderecosBroadcast = listarEnderecosDeBroadcast();

      if (!enderecosBroadcast.isEmpty()) {
        InetAddress enderecoBroadcast = enderecosBroadcast.get(0);
        enviarBroadcast(MENSAGEM_CLIENTE, enderecoBroadcast);
        ipServidor = receberRespostas();
      }
    } catch (Exception e) {
      System.out.println("[SERVIDOR] Erro ao descobrir servidor: " + e.getMessage());
    }

    return ipServidor;
  }

  private String receberRespostas() {
    try {
      socketDescoberta.setSoTimeout(TEMPO_DE_TIMEOUT);
      byte[] dadosEntrada = new byte[TAMANHO_BUFFER];

      DatagramPacket pacoteRecebido = new DatagramPacket(dadosEntrada, dadosEntrada.length);
      socketDescoberta.receive(pacoteRecebido);

      String mensagemRecebida = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
      return extrairIpServidor(mensagemRecebida);

    } catch (SocketTimeoutException e) {
      return descobrirServidorAtivo();
    } catch (Exception e) {
      System.out.println("[RESPOSTA] Erro ao receber resposta: " + e.getMessage());
      return null;
    }
  }

  private String extrairIpServidor(String mensagemRecebida) {
    String[] partes = mensagemRecebida.split("\\*");
    return partes.length > 1 ? partes[1] : null;
  }

  private void enviarBroadcast(String mensagem, InetAddress enderecoBroadcast) {
    try {
      socketDescoberta.setBroadcast(true);
      byte[] dados = mensagem.getBytes();

      for (int porta : PORTAS_BROADCAST) {
        DatagramPacket pacote = new DatagramPacket(dados, dados.length, enderecoBroadcast, porta);
        socketDescoberta.send(pacote);
      }
    } catch (IOException e) {
      System.out.println("[BROADCAST] Erro ao enviar: " + e.getMessage());
    }
  }

  private List<InetAddress> listarEnderecosDeBroadcast() throws SocketException {
    List<InetAddress> enderecosBroadcast = new ArrayList<>();
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

    while (interfaces.hasMoreElements()) {
      NetworkInterface interfaceRede = interfaces.nextElement();

      if (interfaceRede.isLoopback() || !interfaceRede.isUp()) {
        continue;
      }

      interfaceRede.getInterfaceAddresses().stream()
          .map(endereco -> endereco.getBroadcast())
          .filter(Objects::nonNull)
          .forEach(enderecosBroadcast::add);
    }

    return enderecosBroadcast;
  }

  public static String obterIPMaquina() {
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        if (iface.isLoopback() || !iface.isUp())
          continue;

        Enumeration<InetAddress> enderecos = iface.getInetAddresses();
        while (enderecos.hasMoreElements()) {
          InetAddress addr = enderecos.nextElement();
          if (!(addr instanceof Inet6Address))
            return addr.getHostAddress();
        }
      }
    } catch (SocketException e) {
      System.out.println("[IP] Erro ao obter IP: " + e.getMessage());
    }

    return null;
  }

  @FXML
  void trocarParaTelaGrupos(MouseEvent event) {
    if (!verificarCamposDeTexto()) {
      return;
    }

    try {
      Stage stage = (Stage) botaoIniciar.getScene().getWindow();
      Scene scene = new Scene(criarConteudoTelaGrupos());
      scene.setFill(null);
      stage.setScene(scene);
    } catch (Exception e) {
      System.out.println("[TELA] Erro ao trocar para tela de grupos: " + e.getMessage());
    }
  }

  private Parent criarConteudoTelaGrupos() throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/view_grupos.fxml"));
    loader.setController(ControllerGrupos.getInstancia());
    Pane root = loader.load();
    root.setStyle("-fx-background-color: transparent;");
    return root;
  }
}
