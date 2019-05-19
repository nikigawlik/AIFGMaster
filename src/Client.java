import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import lenz.htw.sawhian.Move;
import lenz.htw.sawhian.net.NetworkClient;

public class Client extends Thread {
    public static void main(String[] args) {
        // start 4 clients for testing purposes
        for (int i = 0; i < 4; i++) {
            Client c = new Client();
            c.start();
        }
    }

    private int id;
    private float timeLimit;
    private NetworkClient client;
    private GameState gameState;
    private Random random;

    // static final private float[] WEIGHTS = new float[] {1.645f, -1.119f, -0.617f, 0.692f};
    static final private float[] WEIGHTS = new float[] {1.436f, -0.378f, -0.541f, -0.366f}; // values determined by genetic algorithm
    static final private int MAX_DEPTH = 10;

    public Client() {
        this.id = -1;
    }

    public void run() {
        BufferedImage logo;
        try {
            File f = new File("D:/projects/_SOSE19/AIFG/aifgm/src/logo.png");
            logo = ImageIO.read(f);
        } catch (IOException e) {
            logo = null;
            System.out.println("No user image specified.");
            System.out.println(e);
        }

        this.random = new Random();
        this.client = new NetworkClient(null, generateName(), logo);
        this.id = client.getMyPlayerNumber();
        this.timeLimit = (float)client.getTimeLimitInSeconds() - client.getExpectedNetworkLatencyInMilliseconds() / 1000f;
        this.gameState = new GameState();

        while (true) {
            Move move;
            
            try {
                move = client.receiveMove();
            } catch (RuntimeException e) {
                // e.printStackTrace();
                // System.out.println("game state: " + gameState.toString());
                break;
            }

            long t0 = System.nanoTime();

            if (this.id == 0) {
                // print
                // System.out.println(gameState.toString());
            }

            if (move == null) {
                // my turn
                GameTreeEvaluator gte = new GameTreeEvaluator(
                    gameState, 
                    this.id, 
                    MAX_DEPTH, 
                    WEIGHTS
                );
                logTimedStatus(this.id, t0, "start calculation.");
                gte.start();

                // wait for GTE to finish, but wait for a maximum of timeLimit minus 200ms
                try {
                    gte.join((long)(timeLimit * 1000 - 200));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                logTimedStatus(this.id, t0, "aborting search or done.");
                move = gte.getResult(true);
                logTimedStatus(this.id, t0, "result get.");
                if(move != null) {
                    client.sendMove(move);
                    logTimedStatus(this.id, t0, "move sent.");
                } else {
                    System.out.println("NO POSSIBLE MOVES!");
                }

            } else {
                // other players turn
                gameState.performMove(move);
            }
        }
    }

    // convenience function to log a message with player id and timestamp
    private void logTimedStatus(int playerID, long t0, String message) {
        System.out.println("p" + playerID + ": " + message + " t+" + ((System.nanoTime() - t0) / 1000000) + "ms");
    }

    // simple name generator for funny player names
    private String generateName() {
        String[] kons = {"qu", "w", "wh", "r", "rr", "rh", "t", "th", "tz", "tr", "z", "zh", "p", "ph", "phl", "pt", "s", "sh", "sch", "sc", "sk", "sl", "sw", "sn", "d", "dh", "dn", "dw", "f", "fl", "fr", "g", "gh", "gl", "gr", "h", "k", "kl", "kh", "kr", "kw", "l", "y", "x", "c", "ch", "cl", "v", "vl", "b", "bl", "bh", "bw", "n", "nl", "nh", "m", "mh", "ml"};
        String[] vocs = {"a", "a", "aa", "au", "e", "ei", "ee", "eh", "i", "ii", "ie", "i", "o", "oo", "oof", "oh", "ou", "oe", "oau", "u", "uu", "u", "ui", "ue"};

        String name = "";
        for(int i = 0; i < 3; i++) {
            name += kons[random.nextInt(kons.length)];
            name += vocs[random.nextInt(vocs.length)];
        }
        name += kons[random.nextInt(kons.length)];

        return name;
    }
}