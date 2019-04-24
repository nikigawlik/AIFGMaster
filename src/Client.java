import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;

import lenz.htw.sawhian.Move;
import lenz.htw.sawhian.net.NetworkClient;

public class Client extends Thread {
    public static void main(String[] args) {
        for(int i = 0; i < 4; i++) {
            Client c = new Client();
            c.start();
        }
    }

    private int id;
    private int timeLimit;
    NetworkClient client;
    private GameState gameState;
    Random random;

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

        this.client = new NetworkClient(null, "Bobobot" + (10 + random.nextInt(90)), logo);
        
        this.id = client.getMyPlayerNumber();
        this.timeLimit = client.getTimeLimitInSeconds();
        client.getExpectedNetworkLatencyInMilliseconds();

        this.gameState = new GameState();

        while (true) {
            Move move = client.receiveMove();
            if (move == null) {
                ArrayList<Move> moves = gameState.getPossibleMoves(this.id);
                if(moves.size() > 0) {
                    move = moves.get(random.nextInt(moves.size()));
                    client.sendMove(move);
                } else {
                    System.out.println("NO POSSIBLE MOVES!");
                }
            } else {
                gameState.performMove(move);
            }
        }
    }
}