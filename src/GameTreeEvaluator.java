import java.util.ArrayList;
import java.util.Scanner;

import lenz.htw.sawhian.Move;

class GameTreeEvaluator extends Thread {
    public static void main(String[] args) {
        GameState gameState = new GameState();
        Scanner scanner = new Scanner(System.in);
        int playerID = 0;
        boolean skipInput = false;
        int passCount = 0;

        while(true) {
            System.out.println(gameState.toString());
            GameTreeEvaluator gte = new GameTreeEvaluator(gameState, playerID, 8);
            gte.start();
            Move move = gte.getResult(false);
            System.out.println("Best move: " + move);
            
            if(!skipInput){
                String str = scanner.nextLine();
                if(str.equals("q")) {
                    break;
                } else if(str.equals("s")) {
                    skipInput = true;
                }
            }
            
            if(move != null) {
                gameState.performMove(move);
                passCount = 0;
            } else {
                passCount++;
                System.out.println("pass");
                if(passCount == 4) {
                    // all players passed, gameover
                    break;
                }
            }
            System.out.println("---");
            
            playerID = playerAfter(playerID);
        }
        
        scanner.close();
    }
    
    // should be set to true to stop calculation
    private boolean stop;
    
    private GameState gameState;
    private int playerID;
    private int maxDepth;

    private Move result;

    public float[] evalWeights;

    public GameTreeEvaluator(GameState gameState, int playerID, int maxDepth) {
        this(gameState, playerID, maxDepth, new float[] {1f, 0f, 0f, 0f});
    }
    
    public GameTreeEvaluator(GameState gameState, int playerID, int maxDepth, float[] evalWeights) {
        this.gameState = gameState;
        this.playerID = playerID;
        this.maxDepth = maxDepth;
        this.evalWeights = evalWeights;
    }

    // wait for the thread to finish and return result
    // if stopCalculation is true terminate the thread softly and return the temporary result.
    public Move getResult(boolean stopCalculation) {
        this.stop = stopCalculation; // stop the calculation if specified
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Move getResultDirect() {
        return getMoveFor(this.gameState, this.playerID, this.maxDepth);
    }

    public void run() {
        result = getMoveFor(this.gameState, this.playerID, this.maxDepth);
    }

    private Move getMoveFor(GameState gameState, int playerID, int maxDepth) {
        ArrayList<Move> moves = gameState.getPossibleMoves(playerID);

        float max = -Float.MAX_VALUE;
        Move maxMove = null;

        for (Move move : moves) {
            float[] balance = evaluateSubtree(new GameState(gameState, move), playerAfter(playerID), maxDepth, 1, max);
            if(balance[playerID] > max){
                max = balance[playerID];
                maxMove = move;
            }
        }

        return maxMove;
    }

    private static final float[][] MIN_BALANCE = { 
        {-Float.MAX_VALUE, 0f, 0f, 0f},
        {0f, -Float.MAX_VALUE, 0f, 0f},
        {0f, 0f, -Float.MAX_VALUE, 0f},
        {0f, 0f, 0f, -Float.MAX_VALUE},
    };

    private float[] evaluateSubtree(GameState gameState, int playerID, int maxDepth, int depth, float max){
        int prevPlayerID = playerID-1 & 0b11;
        if(stop) {
            return MIN_BALANCE[prevPlayerID]; // if we abort we make it so that next previous player wont pick this move
        }

        if(gameState.isEndState()){
            return gameState.evaluateEndState();
        } else if(depth >= maxDepth) {
            return gameState.evaluate(evalWeights);
        } else {
            float[] bestBalance = MIN_BALANCE[playerID];

            for (Move move : gameState.getPossibleMoves(playerID)){
                float[] balance = evaluateSubtree(
                    new GameState(gameState, move), 
                    playerAfter(playerID), 
                    maxDepth, 
                    depth + 1, 
                    bestBalance[playerID]
                );

                // the best balance is the balance that is better for me
                if(balance[playerID] > bestBalance[playerID]) {
                    bestBalance = balance;
                }

                // see if we can do a cutoff (not guranteed to be correct)
                if(bestBalance[prevPlayerID] <= max) {
                    // prev player already has a better move, don't bother to calculate the rest of this subtree
                    break;
                }
            }

            return bestBalance;
        }
    }

    private static int playerAfter(int playerID) {
        return (playerID + 1) & 0b11;
    }
}