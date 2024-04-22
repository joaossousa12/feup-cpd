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
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getToken() {
        return this.token;
    }

    public int getElo() {
        return this.elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }
}
