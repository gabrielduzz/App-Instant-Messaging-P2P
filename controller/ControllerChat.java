package controller;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.PeerUDP;
import model.Grupo;
import model.Mensagem;
import model.MensagemAPDU;
import model.Usuario;

public class ControllerChat implements Initializable {
  @FXML
  private VBox vboxMensagens;
  @FXML
  private ScrollPane scrollPaneMensagens;
  @FXML
  private Label labelNomeGrupo;
  @FXML
  private ImageView botaoEnviar;
  @FXML
  private ImageView botaoVoltar;
  @FXML
  private ImageView botaoSair;
  @FXML
  private TextField campoDeTextoMensagem;

  private static ControllerChat controllerChat;
  private static Grupo grupoAtual;

  private ControllerChat() {

  }

  public static ControllerChat getInstancia() {
    if (controllerChat == null) {
      controllerChat = new ControllerChat();
    }
    return controllerChat;
  }

  public static void setGrupoAtual(Grupo grupoAtual) {
    ControllerChat.grupoAtual = grupoAtual;
  }

  @Override
  public void initialize(URL arg0, ResourceBundle rb) {
    grupoAtual.zerarNumeroDeMensagensNaoLidas();

    campoDeTextoMensagem.setStyle("-fx-background-radius: 20;" +
        "-fx-border-radius: 20;" +
        "-fx-background-color: #EFF7CF;" +
        "-fx-padding: 10;" +
        "-fx-border-color: transparent;" +
        "-fx-border-width: 0;" +
        "-fx-font-family: Arial;" +
        "-fx-font-size: 14px;");

    scrollPaneMensagens.setStyle("-fx-background: #000E3B; " +
        "-fx-background-color: transparent; " +
        "-fx-padding: 0;");

    scrollPaneMensagens.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPaneMensagens.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

    scrollPaneMensagens.getContent().setStyle("-fx-background-color: transparent;");

    botaoEnviar.setOnMouseEntered(e -> botaoEnviar.setStyle(
        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.2), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"));

    botaoSair.setOnMouseEntered(e -> botaoSair.setStyle(
        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.2), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"));

    botaoVoltar.setOnMouseEntered(e -> botaoVoltar.setStyle(
        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.2), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"));

    botaoVoltar.setOnMouseClicked(event -> {
      voltar();
    });

    vboxMensagens.heightProperty().addListener((observable, oldValue, newValue) -> {
      scrollPaneMensagens.setVvalue(1.0);
    });

    labelNomeGrupo.setText(grupoAtual.getNome());
    exibirMensagens();
  }

  @FXML
  void enviar(MouseEvent event) {
    String textoMensagem = campoDeTextoMensagem.getText().trim();

    if (textoMensagem.isEmpty()) {
      System.out.println("[CHAT] Nenhuma mensagem digitada.");
      return;
    }

    MensagemAPDU mensagemAPDU = new MensagemAPDU("SEND", grupoAtual.getNome(), Usuario.nome, textoMensagem);
    Mensagem mensagem = new Mensagem(Usuario.nome, textoMensagem, LocalDateTime.now());
    grupoAtual.getListaDeMensagens().add(mensagem);
    Usuario.clienteUDP = new PeerUDP();
    Usuario.clienteUDP.setMensagemAPDU(mensagemAPDU);
    Usuario.clienteUDP.start();
    campoDeTextoMensagem.clear();
    exibirMensagens();
  }

  @FXML
  void sair(MouseEvent event) {
    String nomeGrupo = grupoAtual.getNome();
    Usuario.grupos.remove(grupoAtual);
    Usuario.clienteTCP.setMensagemAPDU(new MensagemAPDU("LEAVE", nomeGrupo, Usuario.nome, Usuario.ipCliente));
    Usuario.clienteTCP.run();
    voltar();
  }

  void voltar() {
    try {
      grupoAtual.zerarNumeroDeMensagensNaoLidas();
      Stage stage = (Stage) botaoSair.getScene().getWindow();
      Scene scene = new Scene(createContentTelaGrupos());
      scene.setFill(null);
      stage.setScene(scene);
    } catch (Exception e) {
      System.out.println("[TELA] Erro ao voltar para tela de grupos: " + e.getMessage());
    }
  }

  private Parent createContentTelaGrupos() throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/view_grupos.fxml"));
    loader.setController(ControllerGrupos.getInstancia());
    Pane root = loader.load();
    root.setStyle("-fx-background-color: transparent;");
    return root;
  }

  public void exibirMensagens() {
    Platform.runLater(() -> {
      vboxMensagens.getChildren().clear();
      List<Mensagem> mensagensDoGrupo = grupoAtual.getListaDeMensagens();
      for (Mensagem mensagem : mensagensDoGrupo) {
        Platform.runLater(
            () -> criarComponenteMensagem(mensagem.getNomeUsuario(),
                mensagem.getTextoMensagem(), mensagem.getHora()));
      }
    });
  }

  public void criarComponenteMensagem(String nomeUsuario, String texto, LocalDateTime hora) {
    VBox vboxMensagem = new VBox();
    vboxMensagem.setSpacing(5);

    HBox hboxMensagem = new HBox();
    hboxMensagem.setSpacing(5);

    Label labelUsuario = new Label(nomeUsuario);
    labelUsuario.setStyle("-fx-font-family: 'Arial'; " +
        "-fx-font-size: 14px; " +
        "-fx-font-weight: bold; " +
        "-fx-text-fill: #EFF7CF;");

    Label labelMensagem = new Label(texto);
    labelMensagem.setWrapText(true);
    labelMensagem.setMaxWidth(200);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    Label labelData = new Label(hora.format(formatter));
    labelData.setStyle("-fx-font-family: 'Arial'; " +
        "-fx-font-size: 12px; " +
        "-fx-text-fill: #B0B0B0;");

    String usuarioAtual = Usuario.nome;
    if (nomeUsuario.equals(usuarioAtual)) {
      hboxMensagem.setAlignment(Pos.CENTER_RIGHT);
      labelMensagem.setStyle("-fx-background-color: #4695FF; " +
          "-fx-background-radius: 20px; " +
          "-fx-border-radius: 20px; " +
          "-fx-text-fill: white; " +
          "-fx-padding: 5 15 5 15; " +
          "-fx-font-family: 'Arial'; " +
          "-fx-font-size: 16px;");
    } else {
      hboxMensagem.setAlignment(Pos.CENTER_LEFT);
      labelMensagem.setStyle("-fx-background-color: #EFF7CF; " +
          "-fx-background-radius: 20px; " +
          "-fx-border-radius: 20px; " +
          "-fx-text-fill: #000E3B; " +
          "-fx-padding: 5 15 5 15; " +
          "-fx-font-family: 'Arial'; " +
          "-fx-font-size: 16px;");
    }

    vboxMensagem.getChildren().addAll(labelUsuario, labelMensagem, labelData);
    vboxMensagem.setStyle("-fx-background-color: transparent; " +
        "-fx-padding: 5; " +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");

    hboxMensagem.getChildren().add(vboxMensagem);

    vboxMensagens.getChildren().add(hboxMensagem);
  }
}
