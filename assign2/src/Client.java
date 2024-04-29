import java.nio.channels.SocketChannel;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class Client {
    private String username, password;
    private String token;
    private int elo;
    private SocketChannel socketChannel;
    //private int elapsedTime = 0;

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

    // private static int getUserElo(String username, String password) throws IOException {
    //     try (BufferedReader fileReader = new BufferedReader(new FileReader("./database.csv"))) {
    //         String line;
    //         while ((line = fileReader.readLine()) != null) {
    //             String[] userData = line.split(",");
    //             if (userData[0].equals(username) && userData[1].equals(password)) {
    //                 return Integer.parseInt(userData[2]);
    //             }
    //         }
    //     }
    //     return -1; // User not found
    // }

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

            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8000));

            File tokenFile = new File("clients/" + clUsername + "/token.csv");
            if (tokenFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(tokenFile));
                String token = br.readLine();
                if (token != null) {
                    List<String> lines = Files.readAllLines(Paths.get("serverTokens/tokens.csv"));
                    for (int i = 0; i < lines.size(); i++) {
                        String[] parts = lines.get(i).split(",");
                        if (parts.length > 2 && parts[0].equals(clUsername) && parts[1].equals(token)) {
                            LocalTime tokenTime = LocalTime.parse(parts[2]);
                            LocalTime currenTime = LocalTime.now();
                            long minutesBetween = ChronoUnit.MINUTES.between(tokenTime, currenTime);
                            if(Math.abs(minutesBetween) <= 30)
                                tokenValid = true;
                            break;
                        }
                    }
                }
                br.close();
            }

            if(tokenValid) {
                System.out.println("Token authentication successful.");
                return;
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
        
                    Client client = new Client(clUsername, password, "", 0, socketChannel);
                    if (client.login()) {
                        System.out.println("Logged in successfully. Token: " + client.getToken());
                    } else {
                        System.err.println("Login failed.");
                    }
                }
                else {
                    System.err.println("Invalid response. Please try again. (" + response + " is not yes neither no)");
                    return;
                }
            }
            
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }
}
