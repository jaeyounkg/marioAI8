package ch.idsia.agents;

import ch.idsia.benchmark.mario.engine.sprites.Sprite;

public final class QLState {

    public static final long HEIGHT = 5;
    public static final long WIDTH = 4;
    public final static int RANGE_START_X = 9;
    public final static int RANGE_START_Y = 7;
    public static final long N_STATES = (1L << (HEIGHT * WIDTH * 2 // 周りの情報
            + 4 // マリオの位置情報
            + 3 // マリオの状態情報
    ));

    private final long asLong;

    public QLState(byte[][] field, byte[][] enemies, float[] marioFloatPos, boolean onGround, boolean cliff,
            boolean ableToJump) {

        long val = 0;
        int startX = RANGE_START_X, startY = RANGE_START_Y;
        for (int i = 0; i < HEIGHT; ++i) {
            for (int j = 0; j < WIDTH; ++j) {
                if (field[startY + i][startX + j] != 0) {
                    val += (1L << (i * WIDTH + j));
                }
            }
        }
        for (int i = 0; i < HEIGHT; ++i) {
            for (int j = 0; j < WIDTH; ++j) {
                if (enemies[startY + i][startX + j] != Sprite.KIND_NONE) {
                    val += (1L << (HEIGHT * WIDTH + i * WIDTH + j));
                }
            }
        }

        final long nFieldDim = HEIGHT * WIDTH * 2;
        final double x = marioFloatPos[0], y = marioFloatPos[1];
        // 0 1 2 3
        final int nX = (int) Math.round(Math.floor((x / 16.0 - Math.floor(x / 16.0)) * 4));
        final int nY = (int) Math.round(Math.floor((y / 16.0 - Math.floor(y / 16.0)) * 4));
        val += (nX < 2) ? (1L << (nFieldDim)) : 0;
        val += (nX % 2 == 1) ? (1L << (nFieldDim + 1)) : 0;
        val += (nY < 2) ? (1L << (nFieldDim + 2)) : 0;
        val += (nY % 2 == 1) ? (1L << (nFieldDim + 3)) : 0;

        val += onGround ? (1L << (nFieldDim + 4)) : 0;
        val += cliff ? (1L << (nFieldDim + 5)) : 0;
        val += ableToJump ? (1L << (nFieldDim + 6)) : 0;
        asLong = val;
    }

    public QLState(long asLong) {

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
