import java.util.ArrayList;

import lenz.htw.sawhian.Move;

class GameState {
    // size of tuples containing weights for the evaluation function
    public final static int EVAL_TUPLE_SIZE = 4;
    // values of a field: 0 = empty, 1 = player1, 2 = player2, ...
    private int[][] field;
    private int[] playerStacks; // stones on players' hands
    private int[] playerPoints; // points scored for each player

    // default constructor, start position
    public GameState() {
        // start with empty field
        field = new int[7][7];
        // start with 7 stones per player
        playerStacks = new int[4];
        for(int i = 0; i < playerStacks.length; i++) {
            playerStacks[i] = 7;
        }
        // and zero score per player
        playerPoints = new int[4];
    }
    
    // creates a mutation of existing game state, after the move is performed
    public GameState(GameState gameState, Move move) {
        this.field = gameState.getPlayingField();
        this.playerStacks = gameState.getPlayerStacks();
        this.playerPoints = gameState.getPlayerPoints();

        this.performMove(move);
    }

    // assigns a 4-dimensional vector ("balance") to the current field position. Each dimension is the current strength of a player.
    // The dimensions always add up to zero
	public float[] evaluate(float[] weights) {
        // weight documentation:
        // 0 - stone progression
        // 1 - stones held back in hand
        // 2 - stones that left the board
        // 3 - crowding (keeping your stones close together)
        float[] balance = new float[4];

        // add win points
        for(int i = 0; i < 4; i++) {
            balance[i] += playerPoints[i] * 8 * weights[0]; // has to count for at least one more than max distance stone on field (7)
            balance[i] += playerStacks[i] * weights[1];
            balance[i] += playerPoints[i] * weights[2];
        }

        // iterate over field
        for(int x = 0; x < 7; x++) {
            for(int y = 0; y < 7; y++) {
                int tileID = tileAt(x, y);
                
                if(tileID != 0) {
                    int playerID = tileID - 1;
                    int locX = localX(playerID, x, y);
                    int locY = localY(playerID, x, y);
                    // points for that player
                    balance[playerID] += (locY + 1) * weights[0];

                    // calculate crowding value (test if stone of same player is in front of this stone)
                    if(locY < 6)
                        balance[playerID] += tileAtLocal(playerID, locX, locY+1) == playerID? weights[3] : 0;
                }
            }
        }

        // offset by average to normalize
        float offset = -(balance[0] + balance[1] + balance[2] + balance[3]) / 4.0f;
        for(int i = 0; i < 4; i++) {
            balance[i] += offset;
        }

        return balance;
    }

    // tests if the game state is a win by points for a player
	public boolean isEndState() {
        for(int i = 0; i < 4; i++) {
            if(playerPoints[i] == 7) {
                return true;
            }
        }
		return false;
    } 
    
    // give a balance for an end state
    public float[] evaluateEndState() {
       float[] balance = new float[4];
       
        for(int i = 0; i < 4; i++) {
            balance[i] = playerPoints[i] == 7? Float.MAX_VALUE : -Float.MAX_VALUE;
        }

        return balance;
    }

    public int[][] getPlayingField() {
        int[][] copy = new int[7][7];
        for(int i = 0; i < 7; i++)
        for(int j = 0; j < 7; j++) {
            copy[i][j] = field[i][j];
        }
        return copy;
    }

    public int[] getPlayerStacks() {
        return playerStacks.clone();
    }

    public int[] getPlayerPoints() {
        return playerPoints.clone();
    }

    // find possible moves
    public ArrayList<Move> getPossibleMoves(int playerID) {
        ArrayList<Move> moves = new ArrayList<>();


        // check all existing stones
        for(int x = 0; x < 7; x++)
        for(int y = 0; y < 7; y++) {
            if(processMove(playerID, x, y, false)) {
                moves.add(new Move(playerID, x, y));
            }
        }

        return moves;
    }

    // move stone in global coords
    public boolean performMove(Move move) {
        return processMove(move.player, move.x, move.y, true);
    }

    // checks if a move is legal and performs it if perform flag is set
    private boolean processMove(int playerID, int x, int y, boolean perform) {
        int locX = localX(playerID, x, y);
        int locY = localY(playerID, x, y);
        
        // check first line (new stone)
        if(locY == 0) {
            if(tileAt(x, y) == 0 && playerStacks[playerID] > 0) {
                // new stone
                if(perform) {
                    playerStacks[playerID]--;
                    setTilePlayer(x, y, playerID);
                }

                return true;
            }
        }

        // check if player is at position, if yes check if moves are possible
        if(isPlayerAt(playerID, x, y)) {
            //check normal move
            if(locY + 1 >= 7 || tileAtLocal(playerID, locX, locY + 1) == 0) {
                if(perform) {
                    setTileEmpty(x, y);
                    // check if move leaves playing field
                    if(locY + 1 < 7) {
                        setTilePlayer(globalX(playerID, locX, locY+1), globalY(playerID, locX, locY+1), playerID);
                    } else {
                        playerPoints[playerID]++;
                    }
                }
                return true;
            }

            //check jumping move
            int offsetY = 0;
            boolean jumpedOnce = false;
            while(true) {
                offsetY++;
                // check for jump over tile
                if(locY + offsetY >= 7 || !isOtherPlayer(playerID, tileAtLocal(playerID, locX, locY + offsetY))) {
                    //chain ends
                    if(!jumpedOnce) {
                        // not a chain
                        return false;
                    } else {
                        // register move
                        if(perform) {
                            offsetY--; // undo the last step
                            setTileEmpty(x, y);
                            setTilePlayer(globalX(playerID, locX, locY+offsetY), globalY(playerID, locX, locY+offsetY), playerID);
                        }
                        return true;
                    }
                }

                offsetY++;

                // check for end of field
                if(locY + offsetY >= 7) {
                    // end of field
                    if(perform) {
                        setTileEmpty(x, y);
                        playerPoints[playerID]++;
                    }
                    return true;
                }

                // check if next tile is blocked
                if(tileAtLocal(playerID, locX, locY + offsetY) != 0) {
                    // chain ends
                    if(!jumpedOnce) {
                        // not a chain
                        return false;
                    } else {
                        // register move
                        if(perform) {
                            offsetY -= 2; // undo the last 2 steps
                            setTileEmpty(x, y);
                            setTilePlayer(globalX(playerID, locX, locY+offsetY), globalY(playerID, locX, locY+offsetY), playerID);
                        }
                        return true;
                    }
                }

                if(!perform) {
                    // no reason to check any further, we don't need to know how far the stone moves
                    return true;
                }

                jumpedOnce = true;
            }
        }
        return false;
    }

    private boolean isOtherPlayer(int playerID, int tileID) {
        return tileID != 0 && tileID != playerID + 1;
    }

    // checks if a player's stone is at x, y
    private boolean isPlayerAt(int playerID, int x, int y) {
        return tileAt(x, y) == playerID+1;
    }

    private boolean isPlayerAtLocal(int playerID, int localX, int localY) {
        return isPlayerAt(playerID, globalX(playerID, localX, localY), globalY(playerID, localX, localY));
    }

    // gets a tile
    private int tileAt(int x, int y) {
        return field[y][x];
    }

    private int tileAtLocal(int playerID, int localX, int localY) {
        return tileAt(globalX(playerID, localX, localY), globalY(playerID, localX, localY));
    }

    private void setTileEmpty(int x, int y) {
        field[y][x] = 0;
    }

    private void setTilePlayer(int x, int y, int playerID) {
        field[y][x] = playerID + 1;
    }

    public String toString() {
        String str = "";

        for(int y = 6; y >= 0; y--) {
            for(int x = 0; x < 7; x++) {
                str = str + field[y][x];
            } 
            str = str + "\n";
        }

        return str;
    }
    
    final static int[] COS = {1, 0, -1, 0};
    final static int[] SIN = {0, 1, 0, -1}; 
    
    public static int globalX(int r, int x, int y) {
        return localX(-r, x, y);
    }

    public static int globalY(int r, int x, int y) {
        return localY(-r, x, y);
    }

    public static int localX(int r, int x, int y) {
        r = r & 0b11; // modulo 4
        x = (x * 2 - 6);
        y = (y * 2 - 6);
        x = COS[r] * x - SIN[r] * y;
        x = (x + 6) / 2;
        return x;
    }
    
    public static int localY(int r, int x, int y) {
        r = r & 0b11; // modulo 4
        x = (x * 2 - 6);
        y = (y * 2 - 6);
        y = SIN[r] * x + COS[r] * y;
        y = (y + 6) / 2;
        return y;
    }

    public int getWinner() {
        for(int i = 0; i < 4; i++) {
            if(playerPoints[i] == 7) {
                return i;
            }
        }

        return -1;
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
                    int xT = globalX(r, x, y);
                    int yT = globalY(r, x, y);
                    line = line + testField[yT][xT];
                } 
                System.out.println(line);
            }
        }
    }
}