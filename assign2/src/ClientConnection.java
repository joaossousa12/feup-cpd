import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientConnection {
    private SocketChannel socketChannel;

    public ClientConnection(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
        this.socketChannel.configureBlocking(true); 
    }

    public void sendMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        while(buffer.hasRemaining()) {
            this.socketChannel.write(buffer);
        }
    
    }

    public String receiveMessage() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = this.socketChannel.read(buffer);
        return new String(buffer.array(), 0, bytesRead);
    }

    public void closeConnection() throws IOException {
        this.socketChannel.close();
    }
}
