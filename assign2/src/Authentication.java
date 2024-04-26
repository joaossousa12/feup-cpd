import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.springframework.security.crypto.bcrypt.BCrypt;

public class Authentication {

    private Map<String, String> userCredentials = new ConcurrentHashMap<>(); 
    private Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private Map<String, Integer> userScores = new ConcurrentHashMap<>();
    
    public String login(String username, String password) {
        loadUserData();
        if (userCredentials.containsKey(username) && userCredentials.get(username).equals(password)) {
            String token = generateToken();
            activeSessions.put(token, username);
            return token; 
        }
        return null; 
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    public boolean validateToken(String token) {
        return activeSessions.containsKey(token);
    }

    private void loadUserData() {
        try (BufferedReader br = new BufferedReader(new FileReader("./database.csv"))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    userCredentials.put(parts[0], parts[1]);
                    userScores.put(parts[0], Integer.parseInt(parts[2]));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load user data: " + e.getMessage());
        }
    }

    private String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    private void saveUserData(String username, String password, int score) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("./database.csv", true))) {
            // for (Map.Entry<String, String> entry : userCredentials.entrySet()) {
            //     String username = entry.getKey();
            //     // Hashing password not working for some reason
            //     //String hashedPassword = hashPassword(entry.getValue());
            //     String hashedPassword = entry.getValue();

            //     int score = userScores.getOrDefault(username, 0);
            //     pw.println(username + "," + hashedPassword + "," + score);
            // }
            pw.println(username + "," + password + "," + score);
        } catch (IOException e) {
            System.err.println("Failed to save user data: " + e.getMessage());
        }
    }

    private void saveUserData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("./database.csv"))) {
            pw.println("username,password,score");
            for (Map.Entry<String, String> entry : userCredentials.entrySet()) {
                String username = entry.getKey();
                String hashedPassword = entry.getValue();

                int score = userScores.getOrDefault(username, 0);
                pw.println(username + "," + hashedPassword + "," + score);
            }
        } catch (IOException e) {
            System.err.println("Failed to save user data: " + e.getMessage());
        }
    }

    public boolean registerUser(String username, String password, int initialScore) {
        loadUserData();
        if (!userCredentials.containsKey(username)) {
            userCredentials.put(username, password);
            userScores.put(username, initialScore);
            saveUserData(username, password, initialScore);
            return true;
        } else {
            System.err.println("User already exists!");
            return false;
        }
    }

    public void updateScore(String username, int newScore) {
        if (userCredentials.containsKey(username)) {
            userScores.put(username, newScore);
            saveUserData();
        } else {
            System.err.println("User does not exist!");
        }
    }
    
}
