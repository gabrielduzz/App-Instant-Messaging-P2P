import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import controller.ControllerInicial;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.PeerTCP;
import model.PeerUDP;
import model.EstadoPeer;
import model.InfoPeer;
import model.PeerListener;
import controller.ControllerInicial;

public class Principal extends Application {

  private Pane createContent() throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/view_inicial.fxml"));
    loader.setController(ControllerInicial.getInstancia());
    Pane root = loader.load();
    root.setStyle("-fx-background-color: transparent;");
    return root;
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    primaryStage.initStyle(StageStyle.TRANSPARENT);

    int larguraPreferida = 1200;
    int alturaPreferida = 900;
    Pane root = createContent();

    Scene scene = new Scene(root, larguraPreferida, alturaPreferida);
    scene.setFill(null);

    primaryStage.setScene(scene);
    primaryStage.setResizable(false);
    primaryStage.show();
  }

  public static void main(String[] args) {
    String ipMaquinaLocal = getIPMaquina();
    System.out.println(ipMaquinaLocal);

    int portaLocal = args.length == 0 ? 6785 : Integer.parseInt(args[0]);

    PeerListener servidor = PeerListener.getInstancia();
    servidor.setMinhasInformacoes(
        new InfoPeer(ipMaquinaLocal, portaLocal, System.currentTimeMillis(), EstadoPeer.DESCOBRINDO));
    servidor.setPortaUDP(portaLocal);
    servidor.setPortaTCP(portaLocal + 1);
    servidor.inicializar();
    launch(args);
  }

  public static String getIPMaquina() {
    String ipMaquinaLocal = null;
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        if (iface.isLoopback() || !iface.isUp())
          continue;

        Enumeration<InetAddress> enderecos = iface.getInetAddresses();
        while (enderecos.hasMoreElements()) {
          InetAddress addr = enderecos.nextElement();
          if (addr instanceof Inet6Address)
            continue;
          ipMaquinaLocal = addr.getHostAddress();
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }

    return ipMaquinaLocal;
  }
}
