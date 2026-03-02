package model;

public class MensagemAPDU {
    private String tipo;
    private String grupo;
    private String usuario;
    private String mensagem = null;

    public MensagemAPDU(String tipo, String grupo, String usuario, String mensagem) {
        this.tipo = tipo;
        this.grupo = grupo;
        this.usuario = usuario;
        this.mensagem = mensagem;
    }

    public MensagemAPDU(String tipo, String grupo, String usuario) {
        this.tipo = tipo;
        this.grupo = grupo;
        this.usuario = usuario;
    }

    public String getTipo() {
        return tipo;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String codificar() {
        String textoAPDU = tipo + "*" + grupo + "*" + usuario;
        if (mensagem != null) {
            textoAPDU += "*" + mensagem;
        }
        return textoAPDU;
    }

    public static MensagemAPDU decodificar(String dados) {
        String[] partes = dados.split("\\*");
        String tipo = partes[0];
        String grupo = partes[1];
        String usuario = partes[2];
        if (partes.length <= 3) {
            return new MensagemAPDU(tipo, grupo, usuario);
        }
        String mensagem = partes[3];
        return new MensagemAPDU(tipo, grupo, usuario, mensagem);
    }

}