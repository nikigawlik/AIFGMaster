import java.util.ArrayList;

import lenz.htw.sawhian.Move;

class GameState {
    // 0 = empty, 1 = player1, 2 = player2, ...
    private int[][] field;

    public GameState() {
        field = new int[7][7];
    }

    // find possible moves
    public ArrayList<Move> getPossibleMoves(int playerID) {
        ArrayList<Move> moves = new ArrayList<>();

        // check first line
        for(int x = 0; x < 7; x++) {
            if(tileAt(transformX(-playerID, x, 0), transformY(-playerID, x, 0)) == 0) {
                moves.add(new Move(playerID, transformX(-playerID, x, 0), transformY(-playerID, x, 0)));
            }
        }

        return moves;
    }

    // move stone in global coords
    public boolean performMove(Move move) {
        // int tx = transformX(move.player, move.x, move.y);
        int ty = transformY(move.player, move.x, move.y);

        int tile = tileAt(move.x, move.y);

        // tile is empty
        if(tile == 0) {
            // check if first line, add new stone
            if(ty == 0) {
                setTile(move.x, move.y, move.player + 1);
            }
        }

        return true;
    }

    // gets a tile
    private int tileAt(int x, int y) {
        return field[y][x];
    }

    private void setTile(int x, int y, int value) {
        field[y][x] = value;
    }

    // rotates playing field by 90 degrees counterclockwise <r> times.
    final static int[] COS = {1, 0, -1, 0};
    final static int[] SIN = {0, 1, 0, -1}; 
    
    public static int transformX(int r, int x, int y) {
        r = r & 0b11; // modulo 4
        x = (x * 2 - 6);
        y = (y * 2 - 6);
        x = COS[r] * x - SIN[r] * y;
        x = (x + 6) / 2;
        return x;
    }
    
    public static int transformY(int r, int x, int y) {
        r = r & 0b11; // modulo 4
        x = (x * 2 - 6);
        y = (y * 2 - 6);
        y = SIN[r] * x + COS[r] * y;
        y = (y + 6) / 2;
        return y;
    }

    // tests (no junit bec setting it up is a pain apparently)
    private static void testTransform() {
        char[][] testField = new char[7][7];
        for(int y = 0; y < 7; y++) 
        for(int x = 0; x < 7; x++) {
            testField[y][x] = (char)((int)'a' + (y*7 + x)%26);
        }

        System.out.println("print normal");
        for(int y = 0; y < 7; y++) {
            String line = "";
            for(int x = 0; x < 7; x++) {
                line = line + testField[y][x];
            } 
            System.out.println(line);
        }

        for(int r = -4; r < 8; r++) {
            System.out.println("test r=" + r + " = " + (r & 0b11)) ;

            for(int y = 0; y < 7; y++) {
                String line = "";
                for(int x = 0; x < 7; x++) {
                    int xT = transformX(r, x, y);
                    int yT = transformY(r, x, y);
                    line = line + testField[yT][xT];
                } 
                System.out.println(line);
            }
        }
    } 

    // public static void main(String[] args) {
    //     testTransform();
    // }

}