import java.util.Map;
import java.util.UUID;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Authentication {

    private Map<String, String> userCredentials = Collections.synchronizedMap(new HashMap<>());
    private Map<String, Integer> userScores = Collections.synchronizedMap(new HashMap<>());


    public static String decrypt(String encryptedPassword, String secretKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
        
        return new String(decryptedBytes);
    }

    private static void writeTokenToClient(String username, String token) {
        String filePath = "clients/" + username + "/token.csv";
        try {
            Files.deleteIfExists(Paths.get(filePath));

            Files.createDirectories(Paths.get(filePath).getParent());

            FileWriter writer = new FileWriter(filePath, true);
            writer.write(token);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeTokenToServer(String username, String token, String currentHour) {
        String filePath = "serverTokens/tokens.csv";

        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts.length > 0 && parts[0].equals(username)) {
                    lines.remove(i);
                    break;
                }
            }

            FileWriter writer = new FileWriter(filePath);
            for (String line : lines) {
                writer.write(line + "\n");
            }

            writer.write(username + "," + token + "," + currentHour + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    public String login(String username, String password) {
        loadUserData();
        
        try {
            if (userCredentials.containsKey(username) && decrypt(userCredentials.get(username), "Xb8WzSs3u8nH4sGw").equals(password)) {
                String token = generateToken();
                String currentHour = LocalTime.now().toString();

                writeTokenToClient(username, token);
                writeTokenToServer(username, token, currentHour);

                return token; 
            }
        } catch (Exception e) {
            System.err.println("Failed to decrypt password: " + e.getMessage());
        }
        
        return null; 
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
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

    public static String encrypt(String password, String secretKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        
        byte[] encryptedBytes = cipher.doFinal(password.getBytes());
        
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private void saveUserData(String username, String password, int score) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("./database.csv", true))) {
            pw.println(username + "," + encrypt(password, "Xb8WzSs3u8nH4sGw") + "," + score);
        } catch (IOException e) {
            System.err.println("Failed to save user data: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Failed to encrypt password: " + e.getMessage());
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
