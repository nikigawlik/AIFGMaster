import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import lenz.htw.sawhian.Move;
import lenz.htw.sawhian.net.NetworkClient;

public class Client extends Thread {
    public static void main(String[] args) {
        for(int i = 0; i < 4; i++) {
            Client c = new Client(i);
            c.start();
        }
    }

    private int id;

    public Client(int id) {
        this.id = id;
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

        NetworkClient client = new NetworkClient(null, "Player " + this.id, logo);

        client.getMyPlayerNumber();
        client.getTimeLimitInSeconds();
        client.getExpectedNetworkLatencyInMilliseconds();

        while (true) {
            Move move = client.receiveMove();
            if (move == null) {
                move = getMove();
                //uncleverer Zug: new Move(client.getMyPlayerNumber(),0,0);
                client.sendMove(move);
            } else {
                updatePlayingField(move);
            }
        }
    }

    private Move getMove() {
        Random r = new Random();
        return new Move(this.id, r.nextInt(8), r.nextInt(8)); // TODO implement
    }

    private void updatePlayingField(Move move) {

    }
}