import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import lenz.htw.sawhian.Move;

class AITrainer {
    final static int GENERATION_SIZE = 100; // number of players per generation
    final static int GENERATIONS = 100; // number of generations
    final static int[] OFFSETS = { 1, 3, 5, 7, 11 }; // games that are played for each player, using players from the list offset by the corresponding number * player id
    final static float SURVIVER_RATIO = 0.33f; // number of players relative to generation size who get carried into the next generation
    final static float MUTATION_CHANCE = 0.05f; // chance of a mutation occuring per value per player
    final static float MUTATION_DRIVE = 0.5f; // maximum absolute value change when a mutation occurs

    public static void main(String[] args) {
        System.out.println("Running game");

        // set up trainer, tuples, random and logging
        AITrainer trainer = new AITrainer();
        float[][] playerTuples = new float[GENERATION_SIZE][GameState.EVAL_TUPLE_SIZE];
        Random rand = new Random();
        Logger logger = Logger.getLogger(AITrainer.class.getName());
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler("AITrainer.log", true);
        } catch (SecurityException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        logger.addHandler(fileHandler);

        logger.info("Start training " + "(v0.1)");

        // initialize with random values
        for(int i = 0; i < GENERATION_SIZE; i++) {
            for(int t = 0; t < GameState.EVAL_TUPLE_SIZE; t++) {
                playerTuples[i][t] = rand.nextFloat() * 2f - 1f;
            }
        }

        int generation = 0;
        while(generation < GENERATIONS) {
            logger.info("Starting on generation " + generation);
            // some variables to keep track of
            int[] wins = new int[GENERATION_SIZE]; // win tally
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
                    // determine the winner and note a win for that player
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
            String str = "Wins: ";
            for (int win : wins) {
                str += win + ", ";
            }
            str += "\n";
            str += "Draws: " + draws + "\n";
            str += "Played games: " + games + "\n";
            
            // recombine & mutate

            // create ordered list of player ids by number of wins 
            int[] ids = 
                IntStream.range(0, GENERATION_SIZE).boxed()
                .sorted((i1, i2) -> Integer.compare(wins[i2], wins[i1])) // sort descending by number of wins
                .mapToInt(i -> (int)i).toArray();
            ;
            
            str += "Best tuple: " + Arrays.toString(playerTuples[ids[0]]) + "\n";
            logger.info(str);

            // preserve 25% and recombine rest
            List<float[]> newTuples = new ArrayList<>();
            int cutoff = (int)(GENERATION_SIZE * SURVIVER_RATIO);

            for(int i = 0; i < GENERATION_SIZE; i++) {
                if(i < cutoff) {
                    // player goes to next generation unchanged
                    newTuples.add(playerTuples[ids[i]]);
                } else {
                    // create a child of two random parents
                    float[] tuple = new float[GameState.EVAL_TUPLE_SIZE];
                    int p1 = rand.nextInt(cutoff);
                    int p2 = rand.nextInt(cutoff);
                    // choose a random parent's value for each value
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

    // runs a test game with different players and returns final game state
    private GameState runTestGame(float[][] playerTuples) {
        GameState gameState = new GameState();
        int playerID = 0;
        int passCount = 0;
        while(true) {
            float[] evalWeights = playerTuples[playerID];
            GameTreeEvaluator gte = new GameTreeEvaluator(gameState, playerID, 2, evalWeights);
            Move move = gte.getResultDirect();

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