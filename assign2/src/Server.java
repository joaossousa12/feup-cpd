
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Server {
    private static String db = "./database.csv";

    private ServerSocketChannel serverSocketChannel;

    public Server(int port) throws IOException { 

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(port));

        System.out.println("Server created, starting...");
        run();

    }

    public void run() throws IOException {
    }



    
}
