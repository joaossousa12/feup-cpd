import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Server {
    private ServerSocket serverSocket;
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 10;
    private AtomicInteger connectedPlayers = new AtomicInteger(0);
    private Timer timer = new Timer();
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean gameStarted = false;
    private Map<Socket, ClientHandler> clientHandlers = Collections.synchronizedMap(new HashMap<>());
    private Authentication auth = new Authentication();
    private Game game;

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
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers.values()) {
                handler.out.println(message);
            }
        }
    }
    

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Server server;
        private PrintWriter out;
        private BufferedReader in;
        private int score = 0;
        private String username;
        private int elo;



        public ClientHandler(Socket socket, Server server) {
            this.clientSocket = socket;
            this.server = server;


            try {
                this.out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                System.err.println("Error initializing PrintWriter for client: " + clientSocket);
                e.printStackTrace();
            }
            try {
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.err.println("Error initializing BufferedReader for client: " + clientSocket);
                e.printStackTrace();
            }
        }

        public String collectAnswer() {
            try {
                return in.readLine();
            } catch (IOException e) {
                return "Error collecting answer";
            }
        }    
        
        public int getScore() {
            return score;
        }
        
        public int getElo() {
            return elo;
        }

        private void updateEloInDatabase(String username, int newElo) {
            try {
                List<String> lines = Files.readAllLines(Paths.get("./database.csv"));
                for (int i = 0; i < lines.size(); i++) {
                    String[] parts = lines.get(i).split(",");
                    if (parts[0].equals(username)) {
                        parts[2] = String.valueOf(newElo);
                        lines.set(i, String.join(",", parts));
                        break;
                    }
                }
                Files.write(Paths.get("./database.csv"), lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                System.err.println("Error updating Elo in database: " + e.getMessage());
            }
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                
                System.out.println("Connection accepted from " + clientSocket.getRemoteSocketAddress());
        
                String inputLine;
                while ((inputLine = in.readLine()) != null) { // Keep reading as long as there's data
                    System.out.println("Received from client: " + inputLine);
        
                    if (inputLine.startsWith("LOGIN,")) {
                        String[] parts = inputLine.split(",");
                        if (parts.length == 3) {
                            this.username = parts[1]; 
                            String token = server.auth.login(parts[1], parts[2]);
                            if (token != null) {
                                out.println("TOKEN," + token);  // Send token back to client
                                server.connectedPlayers.incrementAndGet();
                            } else {
                                out.println("ERROR,Invalid credentials");
                                return;
                            }
                        }
                    } else if (inputLine.startsWith("REGISTER,")) {
                        String[] parts = inputLine.split(",");
                        if (parts.length == 3 && server.auth.registerUser(parts[1], parts[2], 0)) {
                            out.println("REGISTERED");
                        } else {
                            out.println("ERROR,Invalid input");
                            return;
                        }
                    } 
        
                    if (inputLine.startsWith("ready")) {
                        System.out.println("Client is ready to start the game.");
                        int playerCount = server.connectedPlayers.get();
                        System.out.println("Connected to client: " + clientSocket.getRemoteSocketAddress() + " - Total players: " + playerCount);
                        
                        if (playerCount == MIN_PLAYERS) {
                            server.startTimer();
                        }
                        if (playerCount >= MIN_PLAYERS && playerCount <= MAX_PLAYERS && !server.gameStarted) {
                            out.println("start"); // Signal client to start the game
                        }
                    }

                    if(inputLine.startsWith("ANSWER,")){
                        String[] parts = inputLine.split(",");
                        if (parts.length == 4) {
                            if(parts[2].equals(parts[3])){
                                score += 10;
                            } else {
                                if(score >= 5)
                                    score -= 5;
                            }

                            System.out.println("Player " + username + " scored: " + score);
                        } else {
                            out.println("ERROR,Invalid input");
                            return;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client #" + clientSocket.getRemoteSocketAddress());
                e.printStackTrace();
            } finally {
                try {
                    System.out.println("Closing connection to client #" + clientSocket.getRemoteSocketAddress());
                    clientSocket.close();
                    clientHandlers.remove(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }

    public synchronized void evaluateScores() {
        int highestScore = Integer.MIN_VALUE;
        ClientHandler winner = null;

        for (ClientHandler handler : clientHandlers.values()) {
            System.out.println("Player " + handler.username + " scored: " + handler.getScore());
            int clientScore = handler.getScore();
            if (clientScore > highestScore) {
                highestScore = clientScore;
                winner = handler;
            }
        }

        if (winner != null) {
            notifyAllClients("The winner is: " + winner.username + " with a score of: " + highestScore);
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
        }, 1000); // Wait for 30 seconds
    }

    private synchronized void startGame() {
        if (!gameStarted) {
            gameStarted = true;
            System.out.println("Game started!");
            notifyAllClients("Game started!");
            game = new Game(this, connectedPlayers.get());
            game.startGame();

        }
    }

    public Map<Socket, Future<String>> collectAnswers() {
        Map<Socket, Future<String>> answerFutures = Collections.synchronizedMap(new HashMap<>());

        clientHandlers.forEach((socket, handler) -> {
            Future<String> futureAnswer = executor.submit(handler::collectAnswer);
            answerFutures.put(socket, futureAnswer);
        });
        return answerFutures;
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

    private static int getUserElo(String username) {
        try (BufferedReader fileReader = new BufferedReader(new FileReader("./database.csv"))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                String[] userData = line.split(",");
                if (userData[0].equals(username)) {
                    return Integer.parseInt(userData[2]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1; // User not found
    }
    
}
