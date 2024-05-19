import java.nio.channels.SocketChannel;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;

public class Client {
    private String username, password;
    private String token;
    private int elo;
    //private int score;
    private SocketChannel socketChannel;
    private BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    private PrintWriter writer;
    private BufferedReader reader;
    //private int elapsedTime = 0;

    @SuppressWarnings("resource")
    Client(String username, String password, String token, int elo, SocketChannel socketChannel){
        this.username = username;
        this.password = password;
        this.token = token;
        this.elo = elo;
        this.socketChannel = socketChannel;
        //this.score = 0;
        try {
            this.writer = new PrintWriter(new OutputStreamWriter(socketChannel.socket().getOutputStream(), StandardCharsets.UTF_8), true);
            this.reader = new BufferedReader(new InputStreamReader(socketChannel.socket().getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error initializing I/O: " + e.getMessage());
        }
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

    public boolean register(String newUsername, String newPassword) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketChannel.socket().getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socketChannel.socket().getInputStream(), StandardCharsets.UTF_8));
            
            writer.write("REGISTER," + newUsername + "," + newPassword);
            writer.newLine();
            writer.flush();
    
            String response = reader.readLine();
            System.out.println(response);
            if (response != null && response.startsWith("REGISTERED")) {
                // After registration

                return true;  
            } else if (response != null && response.startsWith("ERROR,")) {
                System.err.println("Registration failed: " + response.split(",")[1]);
            }
        } catch (IOException e) {
            System.err.println("Error during registration: " + e.getMessage());
        }
        return false;
    }

    public boolean login() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketChannel.socket().getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socketChannel.socket().getInputStream(), StandardCharsets.UTF_8));
    
            
            writer.write("LOGIN," + username + "," + password);
            writer.newLine();
            writer.flush();
            
    
            // Read response from the server
            String response = reader.readLine();
            if (response != null && response.startsWith("TOKEN,")) {
                this.token = response.split(",")[1];
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error during login: " + e.getMessage());
        }
        return false;
    }

    private static int getUserElo(String username) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new FileReader("./database.csv"))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                String[] userData = line.split(",");
                if (userData[0].equals(username)) {
                    return Integer.parseInt(userData[2]);
                }
            }
        }
        return -1; // User not found
    }

    private static String getUserPassword(String username) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new FileReader("./database.csv"))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                String[] userData = line.split(",");
                if (userData[0].equals(username)) {
                    try {
                        return Authentication.decrypt(userData[1], "Xb8WzSs3u8nH4sGw");
                    } catch (Exception e) {
                        System.err.println("Error decrypting password: " + e.getMessage());
                    }
                }
            }
        }
        return null; // User not found
    }

    public void chooseGameMode() {
        try {
            System.out.println("Choose game mode: (1) Matchmaking, (2) Direct play");
            String choice = consoleReader.readLine().trim();

            if (choice.equals("1")) {
                writer.println("MODE,MATCHMAKING");
            } else if (choice.equals("2")) {
                writer.println("MODE,DIRECT_PLAY");
            } else {
                System.out.println("Invalid choice. Please enter 1 or 2.");
                chooseGameMode();
            }
        } catch (IOException e) {
            System.err.println("Error choosing game mode: " + e.getMessage());
        }
    }

    private void sendReadySignal() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketChannel.socket().getOutputStream(), StandardCharsets.UTF_8));
            writer.write("ready");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error sending ready signal: " + e.getMessage());
        }
    }

    private void listenToServer() {
        int defaultAnswer = 0;
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println(message); 
    
                if (message.startsWith("Question")) {
                    for (int i = 0; i < 4; i++) {  // Display options
                        if ((message = reader.readLine()) != null) {
                            System.out.println(message);
                        }
                    }
                   
                    String answer2 = reader.readLine();
                    int correctAnswer = Integer.parseInt(answer2);

                    Timer timer = new Timer();
                    TimerTask sendDefaultAnswer = new TimerTask() {
                        public void run() {
                        
                            sendAnswer("ANSWER," + username + "," + defaultAnswer + "," + correctAnswer);
                            System.out.println("Time's up! No answer was provided.");
                        }
                    };
                    timer.schedule(sendDefaultAnswer, 10000);  // 10-second timeout
    
                    System.out.print("Your answer (1-4): ");
                    try {
                        
                        long startTime = System.currentTimeMillis();
                        try {
                            while (!consoleReader.ready() && (System.currentTimeMillis() - startTime) < 10000) {
                                Thread.sleep(200);  
                            }
                            if (consoleReader.ready()) {
                                String input = consoleReader.readLine();
                                int answer = Integer.parseInt(input);
                                if (answer >= 1 && answer <= 4) {
                                    sendDefaultAnswer.cancel(); // Cancel the timer task if input is valid
                                    sendAnswer("ANSWER," + username + "," + answer + "," + correctAnswer);
                                    
                                    if(answer == correctAnswer) {
                                        System.out.println("Correct answer!");
                                    } else {
                                        System.out.println("Incorrect answer.");
                                    }
                                } else {
                                    System.out.println("Invalid input. Please enter a number between 1 and 4.");
                                }
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a valid integer.");
                        } catch (InterruptedException e) {
                            System.err.println("Thread was interrupted during sleep: " + e.getMessage());
                            Thread.currentThread().interrupt(); 
                        }
                        if (consoleReader.ready()) {
                            String input = consoleReader.readLine();
                            int answer = Integer.parseInt(input);
                            if (answer >= 1 && answer <= 4) {
                                sendDefaultAnswer.cancel(); 
                                sendAnswer("ANSWER," + username + "," + answer + "," + correctAnswer);
                            } else {
                                System.out.println("Invalid input. Please enter a number between 1 and 4.");
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid integer.");
                    } finally {
                        timer.cancel(); 
                    }
                }
                else if (message.startsWith("UPDATE_ELO")) {
                    String[] parts = message.split(",");
                    if (parts.length == 2) {
                        int newElo = Integer.parseInt(parts[1]);
                        setElo(newElo);
                        System.out.println("Your new Elo rating is: " + newElo);
                        
                        updateElo(newElo);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error listening to server: " + e.getMessage());
        }
    }
    
    private void sendAnswer(String answer) {
        writer.println(answer);
    }

    private void updateElo(int newElo) {
        this.elo = newElo;

        try {
            List<String> lines = Files.readAllLines(Paths.get("./database.csv"));
            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts[0].equals(this.username)) {
                    parts[2] = String.valueOf(this.elo);
                    lines.set(i, String.join(",", parts));
                    break;
                }
            }
            Files.write(Paths.get("./database.csv"), lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error updating Elo: " + e.getMessage());
        }
    }
    

    public static void main(String[] args) {

        // since we are running the client from the command line, we need to pass the client directory as an argument
        // for token authentication to work
        if(args.length != 1) {
            System.err.println("Usage: java Client <username>");
            return;
        }

        String clUsername = args[0];

        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            boolean tokenValid = false;
            String tokenGlobal = null;
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8000));

            File tokenFile = new File("clients/" + clUsername + "/token.csv");
            if (tokenFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(tokenFile));
                String token = br.readLine();
                if (token != null) {
                    List<String> lines = Files.readAllLines(Paths.get("serverTokens/tokens.csv"));
                    int lineToModify = -1;
                    for (int i = 0; i < lines.size(); i++) {
                        String[] parts = lines.get(i).split(",");
                        if (parts.length > 2 && parts[0].equals(clUsername) && parts[1].equals(token)) {
                            LocalTime tokenTime = LocalTime.parse(parts[2]);
                            LocalTime currentTime = LocalTime.now();
                            long minutesBetween = ChronoUnit.MINUTES.between(tokenTime, currentTime);

                            if(Math.abs(minutesBetween) <= 30){
                                tokenGlobal = token;
                                tokenValid = true;
                                lineToModify = i;
                                parts[2] = currentTime.toString();
                                lines.set(i, String.join(",", parts));
                                break;
                            }
                        }
                    }

                    if (tokenValid && lineToModify != -1) {
                        Files.write(Paths.get("serverTokens/tokens.csv"), lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                    }
                }

                br.close();
            }

            if(tokenValid) {
                System.out.println("Token authentication successful.");
                Client client = new Client(clUsername, getUserPassword(clUsername), tokenGlobal, getUserElo(clUsername), socketChannel);
                if(client.login()){
                    client.chooseGameMode();
                    client.sendReadySignal();
                    client.listenToServer();
                }
            }
            
            else{
                System.out.println("Welcome! Do you need to register a new account? (yes/no)");
                String response = consoleReader.readLine().trim().toLowerCase();
        
                if (response.equals("yes")) {
                    System.out.println("Do you want " + clUsername + " to be your username? (yes/no)");
                    response = consoleReader.readLine().trim().toLowerCase();
                    
                    if (response.equals("no")) {
                        System.err.println("Enter desired username: ");
                        clUsername = consoleReader.readLine().trim();
                    }

                    System.out.println("Enter password:");
                    String password = consoleReader.readLine().trim();
        
                    Client client = new Client(clUsername, password, "", 0, socketChannel);
                    if (client.register(clUsername, password)) {
                        System.out.println("Registration successful. Please log in." + client.getToken());
                        if (client.login()) {
                            System.out.println("Logged in successfully. Token: " + client.getToken());
                            client.sendReadySignal();
                            client.listenToServer();
                        } else {
                            System.err.println("Login failed after registration.");
                            return;
                        }
                    } else {
                        System.err.println("Registration failed. Please try again.");
                        return;
                    }
                } else if(response.equals("no")) {
                    System.out.println("Please log in.");
                    
                    System.out.println("Is your username " + clUsername + "? (yes/no)");
                    response = consoleReader.readLine().trim().toLowerCase();
                    
                    if(!response.equals("yes")) {
                        System.err.println("Enter your username: ");
                        clUsername = consoleReader.readLine().trim();
                    }

                    System.out.println("Enter password:");
                    String password = consoleReader.readLine().trim();
        
                    Client client = new Client(clUsername, password, "", getUserElo(clUsername), socketChannel);
                    if (client.login()) {
                        System.out.println("Logged in successfully. Token: " + client.getToken());
                        client.chooseGameMode();
                        client.sendReadySignal();
                        client.listenToServer();
                    } else {
                        System.err.println("Login failed.");
                    }
                }
                
                else if (response.equals("Game started!")) {
                    System.out.println("Game started!");
                }
                
                else {
                    System.err.println("Invalid response. Please try again.");
                    return;
                }    
            }
            
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }
}
