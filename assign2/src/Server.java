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
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class Server {
    private ServerSocket serverSocket;
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 10;
    private AtomicInteger connectedPlayers = new AtomicInteger(0);
    private Timer timer = new Timer();
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean gameStarted = false;
    private Map<Socket, ClientHandler> clientHandlers = new HashMap<>();
    private List<ClientHandler> matchmakingClients = new ArrayList<>();
    private List<ClientHandler> directPlayClients = new ArrayList<>();
    private Authentication auth = new Authentication();
    private Game game;
    private ReentrantLock matchmakingLock = new ReentrantLock();
    private final Lock clientHandlersLock = new ReentrantLock();
    private final Lock gameLock = new ReentrantLock();

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started. Listening on Port " + port);
    }

    public void listen() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientHandlersLock.lock();
                try {
                    clientHandlers.put(clientSocket, handler);
                } finally {
                    clientHandlersLock.unlock();
                }
                executor.submit(handler);
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    break;
                }
            }
        }
    }

    public void notifyAllClients(String message) {
        clientHandlersLock.lock();
        try {
            for (ClientHandler handler : clientHandlers.values()) {
                handler.out.println(message);
            }
        } finally {
            clientHandlersLock.unlock();
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
        private long joinTime;
        private String gameMode = "MATCHMAKING";
        private final Lock handlerLock = new ReentrantLock();



        public ClientHandler(Socket socket, Server server) {
            this.clientSocket = socket;
            this.server = server;
            this.joinTime = System.currentTimeMillis();


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
            handlerLock.lock();
            try {
                return in.readLine();
            } catch (IOException e) {
                System.err.println("Error collecting answer from client " + clientSocket);
                e.printStackTrace();
                return null;
            } finally {
                handlerLock.unlock();
            }
        }    
        
        public int getScore() {
            return score;
        }

        public String getUsername() {
            return username;
        }
        
        public int getElo() {
            return elo;
        }

        public long getJoinTime() {
            return joinTime;

        }
        public void updateElo(int opponentElo, boolean won) {
            
                if (won) {
                    this.elo += 10;
                } else if (this.elo >= 5) {
                    this.elo -= 5;
                }
                updateEloInDatabase(this.username, this.elo);
          
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

        public void close() {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing client handler resources: " + e.getMessage());
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
                            this.elo = getUserElo(parts[1]); 
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
                    else if (inputLine.startsWith("MODE,")) {
                        handleGameMode(inputLine);
                    } 
        
                    if (inputLine.startsWith("ready")) {
                        
                        server.addClientToGameModeList(this);
                        int playerCount = server.directPlayClients.size();
                        if (gameMode.equals("MATCHMAKING")) {
                            server.startMatchmaking();
                        } else {
                            if (playerCount == MIN_PLAYERS) {
                                server.startTimer();
                            }
                            if (playerCount >= MIN_PLAYERS && playerCount <= MAX_PLAYERS && !server.gameStarted) {
                                out.println("start"); // Signal client to start the game
                            }
                        }
                

                    }

                    if(inputLine.startsWith("ANSWER,")){
                        handlerLock.lock();
                        try {
                            String[] parts = inputLine.split(",");
                            if (parts.length == 4) {
                                if (parts[2].equals(parts[3])) {
                                    score += 10;
                                } else {
                                    if (score >= 5) score -= 5;
                                }
                            } else {
                                out.println("ERROR,Invalid input");
                                return;
                            }
                        } finally {
                            handlerLock.unlock();
                        }
                    }
                }
            } catch (SocketException e) {
            }
             catch (IOException e) {
                System.out.println("Error handling client #" + clientSocket.getRemoteSocketAddress());
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    server.removeClientFromGameModeList(this);
                    clientHandlersLock.lock();
                    try {
                        clientHandlers.remove(clientSocket);
                    } finally {
                        clientHandlersLock.unlock();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleGameMode(String inputLine) {
            String[] parts = inputLine.split(",");
            if (parts.length == 2) {
                this.gameMode = parts[1];
                System.out.println("Client " + clientSocket.getRemoteSocketAddress() + " selected game mode: " + this.gameMode);
            } else {
                System.err.println("Invalid game mode input from client: " + inputLine);
            }
        }
        
    }

    public synchronized void evaluateScores() {
        gameLock.lock();
        try {
            int highestScore = Integer.MIN_VALUE;
            ClientHandler winner = null;

            clientHandlersLock.lock();
            try {
                for (ClientHandler handler : clientHandlers.values()) {
                    System.out.println("Player " + handler.getUsername() + " scored: " + handler.getScore());
                    int clientScore = handler.getScore();
                    if (clientScore > highestScore) {
                        highestScore = clientScore;
                        winner = handler;
                    }
                }

                if (winner != null) {
                    notifyAllClients("The winner is: " + winner.username + " with a score of: " + highestScore);

                    for (ClientHandler handler : clientHandlers.values()) {
                        boolean won = handler == winner;
                        handler.updateElo(winner.getElo(), won);
                        System.out.println("Player " + handler.username + " has an Elo of: " + handler.getElo());
                        handler.out.println("UPDATE_ELO," + handler.getElo());
                    }
                }
            } finally {
                clientHandlersLock.unlock();
            }
        } finally {
            gameLock.unlock();
            shutdownServer();
        }
    }

    private void startTimer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                gameLock.lock();
                try {
                    if (!gameStarted && directPlayClients.size() >= MIN_PLAYERS) {
                        System.out.println("Timer triggered, starting game with current players.");
                        startGame();
                    }
                } finally {
                    gameLock.unlock();
                }
            }
        }, 30000); // Wait for 30 seconds
    }

    private synchronized void startGame() {
        gameLock.lock();
        try {
            if (!gameStarted) {
                gameStarted = true;
                System.out.println("Game started!");
                notifyAllClients("Game started!");
                game = new Game(this, directPlayClients.size());
                game.startGame();
            }
        } finally {
            gameLock.unlock();
        }
    }

    public Map<Socket, Future<String>> collectAnswers() {
        Map<Socket, Future<String>> answerFutures = new HashMap<>();

        clientHandlersLock.lock();
        try {
            clientHandlers.forEach((socket, handler) -> {
                Future<String> futureAnswer = executor.submit(handler::collectAnswer);
                answerFutures.put(socket, futureAnswer);
            });    
        } finally {
            clientHandlersLock.unlock();
        }
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

    public void startMatchmaking() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                matchmakingLock.lock();
                try {
                    if (!gameStarted && connectedPlayers.get() >= MIN_PLAYERS) {
                       matchPlayers();
                    }
                } finally {
                    matchmakingLock.unlock();
                }
            }
        }, 30000, 10000); // attempt to match players every 10 seconds
    }

    private void matchPlayers() {
        List<ClientHandler> players = new ArrayList<>(matchmakingClients);

        players.sort(Comparator.comparingInt(ClientHandler::getElo));

        List<List<ClientHandler>> teams = new ArrayList<>();
        List<ClientHandler> currentQueue = new ArrayList<>();

        int eloRange = 100; // initial ELO range
        long currentTime = System.currentTimeMillis();

        for (ClientHandler player : players) {
            if (currentQueue.isEmpty()) {
                currentQueue.add(player);
            } else {
                ClientHandler lastPlayer = currentQueue.get(currentQueue.size() - 1);
                if (Math.abs(player.getElo() - lastPlayer.getElo()) <= eloRange ||
                    (currentTime - player.getJoinTime()) > 30000) { // relax ELO range if player waited > 30 sec
                    currentQueue.add(player);
                } else {
                    if (currentQueue.size() >= MIN_PLAYERS) {
                        teams.add(new ArrayList<>(currentQueue));
                        currentQueue.clear();
                        currentQueue.add(player);
                    }
                }
            }
        }
        if (currentQueue.size() >= MIN_PLAYERS) {
            teams.add(currentQueue);
        }

        for (List<ClientHandler> team : teams) {
            notifyAllClients("Queue formed with players: " + team.stream().map(h -> h.username).collect(Collectors.joining(", ")));
            startGameRanked(team);
        }
    }

    private void startGameRanked(List<ClientHandler> team) {
        notifyAllClients("Starting game for team: " + team.stream().map(h -> h.username).collect(Collectors.joining(", ")));
        game = new Game(this, team.size());
        game.startGame();
    }

    public void addClientToGameModeList(ClientHandler handler) {
        clientHandlersLock.lock();
        try {
            if (handler.gameMode.equals("MATCHMAKING")) {
                matchmakingClients.add(handler);
            } else {
                directPlayClients.add(handler);
            }
        } finally {
            clientHandlersLock.unlock();
        }
    }

    public void removeClientFromGameModeList(ClientHandler handler) {
        clientHandlersLock.lock();
        try {
            if (handler.gameMode.equals("MATCHMAKING")) {
                matchmakingClients.remove(handler);
            } else {
                directPlayClients.remove(handler);
            }
        } finally {
            clientHandlersLock.unlock();
        }
    }

    private void shutdownServer() {
        // Close all client sockets
        clientHandlersLock.lock();
        try {
            for (ClientHandler handler : clientHandlers.values()) {
                handler.close();
            }
            clientHandlers.clear();
        } finally {
            clientHandlersLock.unlock();
        }
    
        // Close the server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    
        // Shut down the executor service
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            System.err.println("Executor service interrupted during shutdown: " + e.getMessage());
        }
    
        System.out.println("Server shut down completed.");
    }
    
    
}
