import java.nio.channels.SocketChannel;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Client {
    private final String username, password;
    private String token;
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

    public boolean register(String newUsername, String newPassword) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketChannel.socket().getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socketChannel.socket().getInputStream(), StandardCharsets.UTF_8));
            
            writer.write("REGISTER," + newUsername + "," + newPassword);
            writer.newLine();
            writer.flush();
    
            String response = reader.readLine();
            System.out.println(response);
            if (response != null && response.startsWith("REGISTER,")) {
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
    public static void main(String[] args) {
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Welcome! Do you need to register a new account? (yes/no)");
            String response = consoleReader.readLine().trim().toLowerCase();
    
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8000));
            if (response.equals("yes")) {
                System.out.println("Enter username:");
                String username = consoleReader.readLine().trim();
                System.out.println("Enter password:");
                String password = consoleReader.readLine().trim();
    
                Client client = new Client(username, password, "", 1500, socketChannel);
                if (client.register(username, password)) {
                    System.out.println("Registration successful. Please log in.");
                } else {
                    System.err.println("Registration failed. Please try again.");
                    return;
                }
            }
    
            
            System.out.println("Please log in.");
            System.out.println("Enter username:");
            String username = consoleReader.readLine().trim();
            System.out.println("Enter password:");
            String password = consoleReader.readLine().trim();
    
            Client client = new Client(username, password, "", 1500, socketChannel);
            if (client.login()) {
                System.out.println("Logged in successfully. Token: " + client.getToken()); 
            } else {
                System.err.println("Login failed.");
            }
            
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }
}
