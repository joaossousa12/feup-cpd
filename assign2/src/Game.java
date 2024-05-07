import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class Game {
    private static final String CSV_FILE = "./triviadb.csv";
    private static final int NUMBER_OF_QUESTIONS = 5;
    private static int numberOfPlayers;
    private static Server server;
    private static ClientConnection connection;

    public Game(Server server, int numberOfPlayers) {
        this.server = server;
        this.numberOfPlayers = numberOfPlayers;
    }

/*     public static void main(String[] args) {
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
    } */

    public static void startGame() {
        List<String[]> questions = readQuestionsFromCSV(CSV_FILE);
        Collections.shuffle(questions);
        int[] scores = new int[numberOfPlayers];
    
        for (int i = 0; i < NUMBER_OF_QUESTIONS; i++) {
            String[] question = questions.get(i);
            if (question.length < 6) {
                continue; // Skipping malformed questions
            }
    
            String questionText = "Question " + (i + 1) + ": " + question[0] + "\n1: " + question[2] + "\n2: " + question[3] + "\n3: " + question[4] + "\n4: " + question[5];
            server.notifyAllClients(questionText);
            Map<Socket, Future<String>> answers = server.collectAnswers();
            answers.forEach((socket, future) -> {
                try {
                    String clientAnswer = future.get(20, TimeUnit.SECONDS);
                    int answerIndex = Integer.parseInt(clientAnswer.trim()) - 1;
                    if (question[1].equals(question[answerIndex + 2])) {
                        //server.updateScore(socket);
                        System.out.println("Player " + socket + " answered correctly!");
                    }
                } catch (TimeoutException te) {
                    System.out.println("Player " + socket + " did not answer in time.");
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Failed to get answer from client " + socket + ": " + e.getMessage());
                }
            });
            System.out.println("Question " + (i + 1) + " completed.");
        }
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
