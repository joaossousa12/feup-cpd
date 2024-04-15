import java.nio.channels.SocketChannel;

public class Client {
    private final String username, password, token;
    private int elo;
    private SocketChannel socketChannel;
    private int elapsedTime = 0;

    Client(String username, String password, String token, int elo, SocketChannel socketChannel){
        this.username = username;
        this.password = password;
        this.token = token;
        this.elo = elo;
        this.socketChannel = socketChannel;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }
}
