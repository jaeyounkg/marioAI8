package ch.idsia.agents;

import ch.idsia.benchmark.mario.engine.sprites.Sprite;

public final class QLState {

    public static final int HEIGHT = 3;
    public static final int WIDTH = 3;
    public final static int RANGE_START_X = 10;
    public final static int RANGE_START_Y = 8;
    public static final int N_STATES = (1 << (HEIGHT * WIDTH * 2 + 3));

    // false means empty
    // public final byte[][] field; // Agent.levelScene
    // public final byte[][] enemies; // Agent.enemies
    // public final boolean onGround;
    // public final boolean cliff;
    // public final boolean ableToJump;

    private final long asLong;

    public QLState(byte[][] field, byte[][] enemies, boolean onGround, boolean cliff, boolean ableToJump) {
        // this.field = new byte[19][19];
        // for (int i = 0; i < 19; ++i) {
        // this.field[i] = field[i].clone();
        // }
        // this.enemies = new byte[19][19];
        // for (int i = 0; i < 19; ++i) {
        // this.enemies[i] = enemies[i].clone();
        // }
        // this.onGround = onGround;
        // this.cliff = cliff;
        // this.ableToJump = ableToJump;

        int val = 0;
        int startX = RANGE_START_X, startY = RANGE_START_Y;
        for (int i = 0; i < HEIGHT; ++i) {
            for (int j = 0; j < WIDTH; ++j) {
                if (field[startY + i][startX + j] != 0) {
                    val += (1 << (i * WIDTH + j));
                }
            }
        }
        for (int i = 0; i < HEIGHT; ++i) {
            for (int j = 0; j < WIDTH; ++j) {
                if (enemies[startY + i][startX + j] != Sprite.KIND_NONE) {
                    val += (1 << (HEIGHT * WIDTH + i * WIDTH + j));
                }
            }
        }

        val += onGround ? (1 << (HEIGHT * WIDTH * 2)) : 0;
        val += cliff ? (1 << (HEIGHT * WIDTH * 2 + 1)) : 0;
        val += ableToJump ? (1 << (HEIGHT * WIDTH * 2 + 2)) : 0;
        asLong = val;
    }

    public QLState(long asLong) {
        // this.field = new byte[19][19];
        // this.enemies = new byte[19][19];
        // this.onGround = false;
        // this.cliff = false;
        // this.ableToJump = false;

        this.asLong = asLong;
    }

    public QLState clone() {
        QLState clone = new QLState(this.asLong);
        return clone;
    }

    public long toLong() {
        return asLong;
    }

}
