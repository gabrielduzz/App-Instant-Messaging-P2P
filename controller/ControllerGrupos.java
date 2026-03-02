package controller;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import model.Grupo;
import model.Mensagem;
import model.Usuario;

public class ControllerGrupos implements Initializable {

  @FXML
  private ImageView botaoEntrarGrupo;

  @FXML
  private VBox vBoxGrupos;

  private static ControllerGrupos controllerGrupos;
  private boolean primeiraVezNoGrupo = true;

  private ControllerGrupos() {

  }

  public static ControllerGrupos getInstancia() {
    if (controllerGrupos == null) {
      controllerGrupos = new ControllerGrupos();
    }
    return controllerGrupos;
  }

  @Override
  public void initialize(URL arg0, ResourceBundle rb) {
    botaoEntrarGrupo.setOnMouseEntered(e -> botaoEntrarGrupo.setStyle(
        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.2), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"));

    if (primeiraVezNoGrupo) {
      Thread thread = new Thread(Usuario.clienteUDP::receberMensagem);
      thread.setDaemon(true);
      thread.start();
      primeiraVezNoGrupo = false;
    }

    listarGruposDoUsuario(Usuario.grupos);
  }

  public void listarGruposDoUsuario(List<Grupo> grupos) {
    vBoxGrupos.getChildren().clear();
    for (Grupo grupo : grupos) {
      HBox novoGrupo = new HBox();
      novoGrupo.setOnMouseClicked(event -> {
        ControllerChat.setGrupoAtual(grupo);
        trocarParaTelaChat();
      });
      novoGrupo.setPrefHeight(70);
      novoGrupo.setAlignment(Pos.CENTER_LEFT);
      novoGrupo.setSpacing(10);
      novoGrupo.setPadding(new Insets(10, 15, 10, 15));

      VBox textos = new VBox();
      textos.setSpacing(5);

      Label titulo = new Label(grupo.getNome());
      titulo.setStyle("-fx-font-family: Arial; " +
          "-fx-font-size: 16px; " +
          "-fx-font-weight: bold; " +
          "-fx-text-fill: #696969");

      String textoUltimaMensagem = "Voce entrou no grupo";
      List<Mensagem> mensagensGrupo = grupo.getListaDeMensagens();
      LocalDateTime horaUltimaMensagem = null;
      if (!mensagensGrupo.isEmpty()) {
        Mensagem ultimaMensagem = grupo.getListaDeMensagens().get(mensagensGrupo.size() - 1);
        textoUltimaMensagem = ultimaMensagem.getNomeUsuario() + ": " + ultimaMensagem.getTextoMensagem();
        horaUltimaMensagem = ultimaMensagem.getHora();
      }

      Label labelUltimaMensagem = new Label(textoUltimaMensagem);
      labelUltimaMensagem.setFont(new Font(13));
      labelUltimaMensagem.setStyle("-fx-text-fill: #777777;");

      textos.getChildren().addAll(titulo, labelUltimaMensagem);

      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);
      VBox notificacoes = new VBox();
      notificacoes.setSpacing(5);
      notificacoes.setAlignment(Pos.CENTER);

      if (horaUltimaMensagem != null) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        Label horaUltimaMensagemLabel = new Label(horaUltimaMensagem.format(formatter));
        horaUltimaMensagemLabel.setStyle("-fx-font-family: Arial; " +
            "-fx-font-size: 12px; " +
            "-fx-text-fill: #777777;");
        notificacoes.getChildren().add(horaUltimaMensagemLabel);
      }

      if (grupo.getNumeroDeMensagensNaoLidas() > 0) {
        Label numeroMensagensNovas = new Label(Integer.toString(grupo.getNumeroDeMensagensNaoLidas()));
        numeroMensagensNovas.setStyle("-fx-font-family: Arial; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold;");
        numeroMensagensNovas.setTextFill(Color.WHITE);

        Circle circulo = new Circle(10);
        circulo.setFill(Color.valueOf("#1B9AAA"));

        StackPane circuloComNumero = new StackPane(circulo, numeroMensagensNovas);
        circuloComNumero.setPrefSize(20, 20);

        notificacoes.getChildren().add(circuloComNumero);
      }

      novoGrupo.setStyle("-fx-background-color: #ffffff; " +
          "-fx-background-radius: 10px; " +
          "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.1), 10, 0, 0, 2); " +
          "-fx-border-color: #e0e0e0; " +
          "-fx-border-radius: 10px; " +
          "-fx-cursor: hand;");

      novoGrupo.setOnMouseEntered(e -> novoGrupo.setStyle("-fx-background-color: #f9f9f9; " +
          "-fx-background-radius: 10px; " +
          "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.2), 10, 0, 0, 2); " +
          "-fx-border-color: #d0d0d0; " +
          "-fx-border-radius: 10px; " +
          "-fx-cursor: hand;"));
      novoGrupo.setOnMouseExited(e -> novoGrupo.setStyle("-fx-background-color: #ffffff; " +
          "-fx-background-radius: 10px; " +
          "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.1), 10, 0, 0, 2); " +
          "-fx-border-color: #e0e0e0; " +
          "-fx-border-radius: 10px; " +
          "-fx-cursor: hand;"));

      novoGrupo.getChildren().addAll(textos, spacer, notificacoes);
      vBoxGrupos.getChildren().add(novoGrupo);
    }
  }

  void trocarParaTelaChat() {
    try {
      Stage stage = (Stage) botaoEntrarGrupo.getScene().getWindow();
      Scene scene = new Scene(createContentTelaChat());
      scene.setFill(null);
      stage.setScene(scene);
    } catch (Exception e) {
      System.out.println("[ERRO] Erro ao carregar a proxima tela: " + e.getMessage());
    }
  }

  private Parent createContentTelaChat() throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/view_chat.fxml"));
    loader.setController(ControllerChat.getInstancia());
    Pane root = loader.load();
    root.setStyle("-fx-background-color: transparent;");
    return root;
  }

  @FXML
  void trocarTelaEntrarGrupo(MouseEvent event) throws Exception {
    Stage stage = (Stage) botaoEntrarGrupo.getScene().getWindow();
    Scene scene = new Scene(createContentTelaEntrarGrupo());
    scene.setFill(null);
    stage.setScene(scene);
  }

  private Parent createContentTelaEntrarGrupo() throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/view_entrar_grupo.fxml"));
    loader.setController(ControllerEntrarGrupo.getInstancia());
    Pane root = loader.load();
    root.setStyle("-fx-background-color: transparent;");
    return root;
  }
}
