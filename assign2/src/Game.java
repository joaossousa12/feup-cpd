import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class Game {
    private static final String CSV_FILE = "./triviadb.csv";
    private static final int NUMBER_OF_QUESTIONS = 5;
    private static final int NUMBER_OF_PLAYERS = 4;
    private static ClientConnection connection;

    public static void main(String[] args) {
        try {
            connection = new ClientConnection("localhost", 8000);
            connection.sendMessage("ready");

            while (true) {
                String response = connection.receiveMessage();
                if (response != null && response.equals("start")) {
                    break;
                }
                System.out.println("Waiting for enough players to connect...");
                Thread.sleep(30000);
            }
            
            startGame();
            connection.closeConnection();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void startGame() {
        List<String[]> questions = readQuestionsFromCSV(CSV_FILE);
        Collections.shuffle(questions);
        Scanner scanner = new Scanner(System.in);
        int[] scores = new int[NUMBER_OF_PLAYERS];

        for (int i = 0; i < NUMBER_OF_QUESTIONS; i++) {
            String[] question = questions.get(i);
            if (question.length < 6) {
                System.out.println("Skipping a malformed question entry.");
                continue;
            }
            System.out.println("Question " + (i + 1) + ": " + question[0]);
            System.out.println("1: " + question[2]);
            System.out.println("2: " + question[3]);
            System.out.println("3: " + question[4]);
            System.out.println("4: " + question[5]);
        
            for (int player = 0; player < NUMBER_OF_PLAYERS; player++) {
                int answer = 0;
                boolean validInput = false; 
            
                while (!validInput) {
                    System.out.print("Player " + (player + 1) + ", enter your answer (1-4): ");
                    if (scanner.hasNextInt()) {
                        answer = scanner.nextInt();
                        if (answer >= 1 && answer <= 4) {
                            validInput = true;
                        } else {
                            System.out.println("Invalid answer. Please answer with numbers 1 to 4.");
                        }
                    } else {
                        System.out.println("Invalid input. Please enter a number.");
                        scanner.next();
                    }
                }
            
                if (question[1].equals(question[answer + 1])) {
                    scores[player]++;
                    System.out.println("Correct!");
                } else {
                    System.out.println("Wrong! Correct answer was: " + question[1]);
                }
            }
            
            System.out.println();

            for (int player = 0; player < NUMBER_OF_PLAYERS; player++) {
                System.out.println("Player " + (player + 1) + " scored " + scores[player]);
            }
    
        }
        scanner.close();
    }

    private static List<String[]> readQuestionsFromCSV(String fileName) {
        List<String[]> questions = new ArrayList<>();
        Path pathToFile = Paths.get(fileName);
        
        try (BufferedReader br = Files.newBufferedReader(pathToFile, StandardCharsets.UTF_8)) {
            String line = br.readLine(); // skip the header
            
            while ((line = br.readLine()) != null) {
                String[] attributes = line.split(",");
                questions.add(attributes);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return questions;
    }
}
