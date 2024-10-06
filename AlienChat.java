import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * This is a program implementing an alien chat protocol. The entry point is
 * `main()`.
 *
 * The protocol is as follows:
 * - clients connect to the server on port 5999 (by default)
 * - initially, each client must send `AlienChat.PASSWORD` followed by newline
 * to the server
 * - then, lines of input from each user should be sent to the server
 * - simultaneously, the server describes activity in the chat room
 * line-by-line; these
 * lines should be printed at the client for the user to read.
 *
 * The input sent to and the output received from the server is line based;
 * that is, newline "\n" separates each record. A limitation of the protocol is
 * thus
 * that newlines cannot appear in messages!
 *
 * Lines from the server have one of the following formats:
 * - "* USERNAME connected" -- a user known as USERNAME has joined the chat
 * - "* USERNAME disconnected" -- a user known as USERNAME has left the chat
 * - "- USERNAME >>> MESSAGE" -- a user known as USERNAME has sent the message
 * MESSAGE into the chat.
 *
 * You may rely on USERNAME not containing any whitespace characters (space,
 * newline, tab, etc.)
 *
 * However, each MESSAGE may contain arbitrary text except for newlines.
 */
public class AlienChat {
    public static final String PASSWORD = "F4EF9A36-5FCD-4D27-8A0A-FC7C77D3DBB2";
    public static final String HOSTNAME = getenv("ALIENCHAT_SERVER", "alienchat.demo.leastfixedpoint.com");
    public static final short PORT = Short.parseShort(getenv("ALIENCHAT_PORT", "5999"));

    /**
     * Connects to the server at `HOSTNAME`:`PORT`. Then, sends the `PASSWORD` and
     * runs two
     * asynchronous tasks, one for relaying lines of input from the client to the
     * server, and one
     * for relaying lines of output from the server for the client's user to read.
     *
     * Whichever task completes first cancels the other. Once both have completed
     * (or been
     * cancelled), the socket is closed and the program terminates.
     */
    public static void main(String[] args) {

        var recognitionCode = args.length > 0 ? args[0] : "";
        try {
            var socket = new Socket(HOSTNAME, PORT);
            InputStream fromServer = socket.getInputStream();
            PrintStream toServer = new PrintStream(socket.getOutputStream());

            loginToServer(toServer);

            var task1 = CompletableFuture.runAsync(() -> relayFromServer(fromServer));
            var task2 = CompletableFuture.runAsync(() -> relayToServer(recognitionCode, toServer));
            task1.thenApply((Void ignored) -> {
                task2.cancel(true);
                return null;
            });
            task2.thenApply((Void ignored) -> {
                task1.cancel(true);
                return null;
            });

            CompletableFuture.allOf(task1, task2).exceptionally((Throwable t) -> null).join();
            socket.close();
        } catch (Throwable e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Retrieve an environment variable, returning a default value if the
     * environment
     * variable is not defined.
     */
    static String getenv(String environmentVariable, String defaultValue) {
        var v = System.getenv(environmentVariable);
        return v == null ? defaultValue : v;
    }

    /**
     * Sends the `PASSWORD` to the server (or whatever is listening to the other end
     * of `toServer`).
     */
    static void loginToServer(PrintStream toServer) {
        toServer.println(PASSWORD);
    }

    /**
     * Accepts lines from `fromServer`, printing each to standard output.
     */
    static void relayFromServer(InputStream fromServer) {
        forEachLine("server", fromServer, (line) -> {
            var maybeMessage = parseMessageLine(line);
            if (maybeMessage != null) {
                var valid = validateMessage(maybeMessage);
                System.out.println("% The following message " + (valid ? "IS" : "IS NOT") + " from our clan:");
                // checking the validion of the message for decryption
                if (valid) {
                    // after checking the validation we splited the line variable and store it in
                    // array called msgs
                    String[] msgs = line.split("[, ]");
                    // we've initiated an new variable to store the new message after decryption
                    String newMsg = "";
                    for (int i = 0; msgs.length > i; i++) {
                        // this condition to to store the connection info at the beginning of the
                        // message
                        if (i < 3) {
                            newMsg = newMsg + " " + msgs[i];
                        } else {
                            // here we store the message after decryption
                            newMsg = newMsg + " " + Cipher.decrypt(msgs[i]);
                        }
                    }
                    line = newMsg;
                }
            }
            // here we print the decrypted message if its valid but if its not valid it will
            // print it as it is
            System.out.println(line);
        });
    }

    /**
     * Parses an incoming line of text from the server. If it is a "message" line,
     * according to the format description in the class comment, returns the
     * text of the message (only). Otherwise, returns null.
     */
    static String parseMessageLine(String line) {

        if (!line.startsWith("- "))
            return null;

        // We start looking for our separator starting at index two, because
        // we want to skip the space following the dash at the beginning of the line:
        final var SEPARATOR = " >>> ";
        var separatorPos = line.indexOf(SEPARATOR, 2);
        if (separatorPos == -1)
            return null;

        // Remove the first `separatorPos` characters, plus as many as are in the
        // separator itself:
        return line.substring(separatorPos + SEPARATOR.length());
    }

    /**
     * Accepts lines from standard input, forwarding them on to `toServer`.
     */
    static void relayToServer(String recognitionCode, PrintStream toServer) {
        forEachLine("user", System.in, (line) -> {
            // Encrypts message. The recognition code is not added automatically
            // it must be typed at the start of the message manually by the user.
            line = Cipher.encrypt(line);
            toServer.println(line);
        });
    }

    /**
     * Reads lines of input from `source`, sending each one to `sink` until there
     * are
     * no more (when `readLine()` returns `null`) or an exception is thrown.
     */
    static void forEachLine(String sourceDescription, InputStream source, Consumer<String> sink) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(source));
            while (true) {
                var line = in.readLine();
                if (line == null) {
                    System.out.println("(End-of-stream from " + sourceDescription + ")");
                    break;
                }
                sink.accept(line);
            }
        } catch (Throwable e) {
            System.err.println("Error processing input from " + sourceDescription);
            System.err.println(e);
        }
    }

    /**
     * Validates the recognitionCode (if any) at the front of a received message.
     * Returns true if the recognitionCode is valid according to the pattern for our
     * clan,
     * or false if the recognitionCode is invalid or missing.
     */
    static boolean validateMessage(String message) {
        // Expect "somerecognitioncode restofthemessage whichmaycontain spaces",
        // so use the `limit` parameter to `String.split()` to just split *once*:
        String[] pieces = message.split(" ", 2);

        if (pieces.length < 2) {
            // No space at all in the input! Clearly not valid.
            return false;
        }

        String recognitionCode = pieces[0];
        recognitionCode = Cipher.decrypt(recognitionCode);
        /*
         * Validates `recognitionCode` according to DFA.
         */
        return DFASimulator.simulateDFA(recognitionCode);
    }

    public static class DFASimulator {

        private static final int kNumStates = 24; // 24 states based on the table
        private static final int kNumSymbols = 4; // 4 symbols(a,b,c,d)
        private static final int[][] kTransitionTable = {
                { 1, 11, 11, 11 },
                { 2, 11, 11, 11 },
                { 2, 3, 12, 18 },
                { 3, 4, 13, 19 },
                { 4, 5, 14, 20 },
                { 5, 6, 15, 21 },
                { 7, 11, 16, 22 },
                { 7, 8, 16, 22 },
                { 9, 11, 11, 11 },
                { 11, 10, 11, 11 },
                { 11, 11, 11, 11 },
                { 11, 11, 11, 11 },
                { 12, 13, 11, 2 },
                { 13, 14, 11, 3 },
                { 14, 15, 11, 4 },
                { 15, 16, 11, 5 },
                { 17, 11, 11, 6 },
                { 17, 8, 11, 6 },
                { 18, 19, 2, 11 },
                { 19, 20, 3, 11 },
                { 20, 21, 4, 11 },
                { 21, 22, 5, 11 },
                { 23, 11, 6, 11 },
                { 23, 8, 6, 11 }

        };
        private static final boolean[] kAcceptTable = {
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false

        };

        public static boolean simulateDFA(String input) {
            int state = 0;
            char[] inputArray = input.toCharArray();
            for (int i = 0; i < inputArray.length; i++) {
                char ch = inputArray[i];
                // If the recognition code includes characters other than a,b,c,d
                // instead of throwing an error, we simply reject the message
                // so the chat program doesn't stop.
                if (ch != 'a' && ch != 'b' && ch != 'c' && ch != 'd') {
                    return false;
                }
                // When we read a character, in case we read 'a',
                // we need to go to column 1, in case of b to column 2
                // of the table and so on. This is achieved by simply
                // substracting the ascii code of 'a' from each character we read
                // and going to the column of that number.
                state = kTransitionTable[state][ch - 'a'];
            }
            return kAcceptTable[state];
        }
    }
}
