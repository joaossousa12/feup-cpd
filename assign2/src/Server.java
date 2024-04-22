import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Server {
    private ServerSocket serverSocket;
    private static final int MIN_PLAYERS = 4;
    private static final int MAX_PLAYERS = 10;
    private AtomicInteger connectedPlayers = new AtomicInteger(0);
    private Timer timer = new Timer();
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean gameStarted = false;
    private Map<Socket, ClientHandler> clientHandlers = new ConcurrentHashMap<>();

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started. Listening on Port " + port);
    }

    public void listen() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientHandlers.put(clientSocket, handler);
                executor.submit(handler);
            } catch (IOException e) {
                System.out.println("Exception caught when trying to listen on port or listening for a connection");
                System.out.println(e.getMessage());
                if (serverSocket.isClosed()) {
                    break;
                }
            }
        }
    }

    public void notifyAllClients(String message) {
        for (ClientHandler handler : clientHandlers.values()) {
            //handler.sendMessage(message);
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Server server;
        private PrintWriter out;



        public ClientHandler(Socket socket, Server server) {
            this.clientSocket = socket;
            this.server = server;
            //this.out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                
                String inputLine = in.readLine();
                System.out.println(inputLine);
                if ("ready".equals(inputLine)) {
                    int playerCount = server.connectedPlayers.incrementAndGet();
                    System.out.println("Connected to client: " + clientSocket.getRemoteSocketAddress() + " - Total players: " + playerCount);
                    
                    if (playerCount == MIN_PLAYERS) {
                        server.startTimer();
                    }
                    if (playerCount >= MIN_PLAYERS && playerCount <= MAX_PLAYERS && !server.gameStarted) {
                        out.println("start"); // Signal client to start the game
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client #");
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    clientHandlers.remove(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startTimer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (Server.this) {
                    if (!gameStarted && connectedPlayers.get() >= MIN_PLAYERS) {
                        System.out.println("Timer triggered, starting game with current players.");
                        startGame();
                    }
                }
            }
        }, 30000); // Wait for 30 seconds
    }

    private synchronized void startGame() {
        if (!gameStarted) {
            gameStarted = true;
            System.out.println("Game started!");
            notifyAllClients("Game started!");

            // Notify all clients that the game has started
            // (Assuming you have a list of connected clients somewhere)
            // for (ClientHandler client : connectedClients) {
            //     client.sendMessage("game_start");
            // }
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server(8000);
            server.listen();
        } catch (IOException e) {
            System.err.println("Could not listen on port: 8000");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
