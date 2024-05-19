# How to compile

> To compile our program we designed a Makefile so one just needs to type:

```bash
$ cd assign2/src
$ make
```

> And all the java files used will be compiled and ready to use.

# How to use

> To use our program firstly open a terminal and start the Server:

```bash
$ java Server
```

> Then open ```n``` terminals for each client that is going to play the game:

```bash
$ java Client <username>
```

> After that proceed with either the login/register of that user and if you have logged in before not long ago you are going to be automatically logged in.

> After logging in you are going to be prompted to select the game mode either: MatchMaking (Ranked mode) or Direct Play (Simple mode).

## Architecture and Implementation

### Threads and Concurrency

> ```Server```: The server uses an ```ExecutorService``` to manage client connections concurrently. Each client connection is handled by a separate thread ```(ClientHandler)```.

> ```Client```: Each client interacts with the server in its own thread, handling user input and server responses concurrently.

### Slow Clients

> The server uses a timer to manage game start times, ensuring that slow clients do not hold up the game for others. If a client takes too long to answer, a default answer is sent automatically (0 that is a wrong answer always).

### Ranked Mode (MatchMaking)

> ```ELO System```: The game uses an ELO rating system to rank players. Players are matched based on their ELO ratings within a certain range. If a player has waited for more than 30 seconds, the ELO range is relaxed.

> ```Queue Formation```: Players are grouped into a queue based on their ELO ratings. The server periodically checks and forms queues when enough players are available.

### Direct Play (Simple Mode)

> In Direct Play mode, the game starts past a 30 second delay that starts as soon as the minimum number of players is reached.

### Game Description

> ```Trivia Question```: The game consists of multiple trivia questions. Each question is presented with four answer choices. Players have a limited time to respond.

> ```Scoring```: Players score points for correct answers and lose points for incorrect answers. The player with the highest score at the end of the game is the winner.

### Authentication
> ```User Data```: User credentials and scores are stored in a CSV file. Passwords are encrypted using AES encryption.

> ```Login```: Users provide their username and password to login. If successful, a token is generated and stored both on the client and server sides.

> ```Register```: New users can register with a username and password. Initial scores are set to zero.

> ```Token Management```: Tokens are used for authentication and are valid for 30 minutes. Tokens are stored in separate CSV files for clients and servers.

### Server Implementation Details

> ```Client Management```: The server keeps track of connected clients using a ```HashMap```. Each client is associated with a ```ClientHandler``` that manages communication.

> ```Game Control```: The ```Game``` class handles the game logic, including reading questions from a CSV file, presenting them to clients, and collecting answers.

> ```Synchronization```: Locks (```ReentrantLock```) are used to manage concurrent access to shared resources, such as the list of connected clients and game state.

### Client Implementation Details

> ```User Authentication```: Clients can register new accounts or login with existing credentials. Authentication is managed by the server.

> ```Token Management```: Upon successful login, a token is generated and stored in a CSV file on the client side. The token is also sent to the server for validation.

> ```Game Interaction```: Clients interact with the server to choose game modes, send answers, and receive updates. A timer ensures clients respond within the allowed time frame.



