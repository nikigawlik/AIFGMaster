import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import lenz.htw.sawhian.Move;

class AITrainer {
    final static int GENERATION_SIZE = 100;
    final static int GENERATIONS = 100;
    final static int[] OFFSETS = {1,3,5,7,11};
    final static float SURVIVER_RATIO = 0.33f;
    final static float MUTATION_DRIVE = 0.5f;
    final static float MUTATION_CHANCE = 0.05f;

    public static void main(String[] args) {
        System.out.println("Running game");
        AITrainer trainer = new AITrainer();
        float[][] playerTuples = new float[GENERATION_SIZE][GameState.EVAL_TUPLE_SIZE];
        Random rand = new Random();

        // initialize with random values
        for(int i = 0; i < GENERATION_SIZE; i++) {
            for(int t = 0; t < GameState.EVAL_TUPLE_SIZE; t++) {
                playerTuples[i][t] = rand.nextFloat() * 2f - 1f;
            }
        }

        int generation = 0;
        while(generation < GENERATIONS) {
            System.out.println("Starting on generation " + generation);
            // test
            int[] wins = new int[GENERATION_SIZE];
            int draws = 0;
            int games = 0;
            // for every player...
            for(int p = 0; p < GENERATION_SIZE; p++) {
                // ... play multiple games
                for(int game = 0; game < OFFSETS.length; game++) {
                    // pick opponents using a fixed offset in list (not biased because list is shuffled)
                    int offset = OFFSETS[game];
                    float[][] gameTuples = new float[4][GameState.EVAL_TUPLE_SIZE];
                    for(int i = 0; i < 4; i++) {
                        gameTuples[i] = playerTuples[(p + i * offset) % GENERATION_SIZE];
                    }
                    games++;
                    GameState result = trainer.runTestGame(gameTuples);
                    int winner = result.getWinner();
                    if(winner >= 0) {
                        wins[(p + winner * offset) % GENERATION_SIZE]++; 
                    } else {
                        draws++;
                    }
                }
            }
            // show results in console
            System.out.print("Wins: ");
            for (int win : wins) {
                System.out.print(win + ", ");
            }
            System.out.println();
            System.out.println("Draws: " + draws);
            System.out.println("Played games: " + games);
            // recombine & mutate
            int[] ids = 
                IntStream.range(0, GENERATION_SIZE).boxed()
                .sorted((i1, i2) -> Integer.compare(wins[i2], wins[i1])) // sort descending by number of wins
                .mapToInt(i -> (int)i).toArray();
            ;
            
            System.out.println("Best tuple: " + Arrays.toString(playerTuples[ids[0]]));

            // preserve 25% and recombine rest
            List<float[]> newTuples = new ArrayList<>();
            int cutoff = (int)(GENERATION_SIZE * SURVIVER_RATIO);
            for(int i = 0; i < GENERATION_SIZE; i++) {
                if(i < cutoff) {
                    newTuples.add(playerTuples[ids[i]]);
                } else {
                    float[] tuple = new float[GameState.EVAL_TUPLE_SIZE];
                    int p1 = rand.nextInt(cutoff);
                    int p2 = rand.nextInt(cutoff);
                    for(int t = 0; t < GameState.EVAL_TUPLE_SIZE; t++) {
                        tuple[t] = playerTuples[ids[rand.nextBoolean()? p1 : p2]][t];
                        if(rand.nextFloat() < MUTATION_CHANCE) {
                            tuple[t] += (rand.nextFloat() * 2f - 1f) * MUTATION_DRIVE;
                        }
                    }
                    newTuples.add(tuple);
                }
            }
            Collections.shuffle(newTuples, rand);
            playerTuples = newTuples.toArray(playerTuples);
            
            generation++;
        }
    }

    public AITrainer() {
    }

    private int c = 0;

    private GameState runTestGame(float[][] playerTuples) {
        GameState gameState = new GameState();
        int playerID = 0;
        int passCount = 0;
        while(true) {
            float[] evalWeights = playerTuples[playerID];
            GameTreeEvaluator gte = new GameTreeEvaluator(gameState, playerID, 2, evalWeights);
            gte.setName("GTE" + c++);
            gte.start();
            Move move = gte.getResult(false);

            if(move != null) {
                gameState.performMove(move);
                passCount = 0;
            } else {
                passCount++;
                if(passCount == 4) {
                    // all players passed, gameover
                    break;
                }
            }

            playerID = (playerID + 1) & 0b11;
        }

        return gameState;
    }
}