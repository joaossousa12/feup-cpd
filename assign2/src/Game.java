import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

public class Game {
    private static final String CSV_FILE = "./triviadb.csv";
    private static final int NUMBER_OF_QUESTIONS = 5;
    private static int numberOfPlayers;
    private static Server server;
    private static int currCorrectAnswer = 0;
    private static final ReentrantLock lock = new ReentrantLock();

    public Game(Server server, int numberOfPlayers) {
        Game.server = server;
        Game.numberOfPlayers = numberOfPlayers;
    }

    public void startGame() {
        List<String[]> questions = readQuestionsFromCSV(CSV_FILE);
        Collections.shuffle(questions);
        int[] scores = new int[numberOfPlayers];
    
        for (int i = 0; i < NUMBER_OF_QUESTIONS; i++) {
            String[] question = questions.get(i);
            if (question.length < 6) {
                continue; // Skipping malformed questions
            }
    
            String questionText = "\nQuestion " + (i + 1) + ": " + question[0] + "\n1: " + question[2] + "\n2: " + question[3] + "\n3: " + question[4] + "\n4: " + question[5];
            
            if(question[2].equals(question[1]))
                currCorrectAnswer = 1;
            
            else if(question[3].equals(question[1]))
                currCorrectAnswer = 2;
            
            else if(question[4].equals(question[1]))
                currCorrectAnswer = 3;
            
            else if(question[5].equals(question[1]))
                currCorrectAnswer = 4;

            server.notifyAllClients(questionText);
            server.notifyAllClients(String.valueOf(currCorrectAnswer));
    
            ExecutorService collectorExecutor = Executors.newSingleThreadExecutor();
            collectorExecutor.execute(() -> {
                // try {
                    //Thread.sleep(10000);
                    collectAndProcessAnswers(question, server, scores);
                // } 
                // catch (InterruptedException ie) {
                //     Thread.currentThread().interrupt();
                // }
            });
    
            collectorExecutor.shutdown();
            try {
                if (!collectorExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                    collectorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                collectorExecutor.shutdownNow();  
            }
        }

        System.out.println("Game Over! Final scores:");

        server.evaluateScores();
    }    
    
    
    private static void collectAndProcessAnswers(String[] question, Server server, int[] scores) {
        Map<Socket, Future<String>> answers = server.collectAnswers();
    
        answers.forEach((socket, future) -> {
            try {
                String clientAnswer = future.get(10, TimeUnit.SECONDS); 
                int answerIndex = Integer.parseInt(clientAnswer.trim()) - 1;
    
                if (question[1].equals(question[answerIndex + 2])) {
                    System.out.println("Player " + socket + " answered correctly!");
                } else {
                    System.out.println("Player " + socket + " answered incorrectly.");
                }
            } catch (TimeoutException e) {
                System.out.println("Timeout Exception handle in future.");
            } catch (InterruptedException | ExecutionException e) {
                if (e.getMessage() != null) 
                    System.out.println("Failed to get answer from client " + socket + ": " + e.getMessage());

                Thread.currentThread().interrupt(); 
            }
        });
    
        System.out.println("\nQuestion " + question[0] + " completed.\n");
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

    public static int getCurrCorrectAnswer() {
        return currCorrectAnswer;
    }
}
