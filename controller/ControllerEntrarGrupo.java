package controller;

import java.net.URL;
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
import model.Grupo;
import model.MensagemAPDU;
import model.Usuario;

public class ControllerEntrarGrupo implements Initializable {
  @FXML
  private ImageView botaoEntrar;

  @FXML
  private ImageView botaoVoltar;

  @FXML
  private TextField campoDeTextoNomeGrupo;

  private static ControllerEntrarGrupo controllerEntrarGrupo;

  private ControllerEntrarGrupo() {

  }

  public static ControllerEntrarGrupo getInstancia() {
    if (controllerEntrarGrupo == null) {
      controllerEntrarGrupo = new ControllerEntrarGrupo();
    }
    return controllerEntrarGrupo;
  }

  @Override
  public void initialize(URL arg0, ResourceBundle rb) {
    botaoEntrar.setOnMouseEntered(e -> botaoEntrar.setStyle(
        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.2), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"));

    botaoVoltar.setOnMouseEntered(e -> botaoVoltar.setStyle(
        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.2), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"));

    botaoVoltar.setOnMouseClicked(event -> {
      voltar();
    });
  }

  @FXML
  void entrarGrupo(MouseEvent event) throws Exception {
    if (Usuario.grupos.size() > 9) {
      return;
    }

    String nomeGrupo = campoDeTextoNomeGrupo.getText();
    if (nomeGrupo.isEmpty()) {
      return;
    }

    for (Grupo grupo : Usuario.grupos) {
      if (grupo.getNome().equals(nomeGrupo)) {
        System.out.println("[GRUPO] Usuario ja esta no grupo: " + nomeGrupo);
        return;
      }
    }

    Grupo grupoAtual = new Grupo(nomeGrupo);
    Usuario.grupos.add(grupoAtual);
    Usuario.clienteTCP.setMensagemAPDU(new MensagemAPDU("JOIN", nomeGrupo, Usuario.nome, Usuario.ipCliente));
    Usuario.clienteTCP.run();
    voltar();
  }

  void trocarTelaChat() throws Exception {
    Stage stage = (Stage) botaoEntrar.getScene().getWindow();
    Scene scene = new Scene(createContentTelaChat());
    scene.setFill(null);
    stage.setScene(scene);
  }

  private Parent createContentTelaChat() throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/view_chat.fxml"));
    loader.setController(ControllerChat.getInstancia());
    Pane root = loader.load();
    root.setStyle("-fx-background-color: transparent;");
    return root;
  }

  void voltar() {
    try {
      Stage stage = (Stage) botaoEntrar.getScene().getWindow();
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
}
