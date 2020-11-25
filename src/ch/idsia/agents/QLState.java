package ch.idsia.agents;

import ch.idsia.benchmark.mario.engine.sprites.Sprite;

public final class QLState {

    public static final int N_STATES = (1 << 15);
    // 前方2マスの縦何マスを取得するか
    public static final int WIDTH = 3;

    private static final int marioEgoRow = 9;
    private static final int marioEgoCol = 9;

    // false means empty
    public final byte[][] field; // Agent.levelScene
    public final byte[][] enemies; // Agent.enemies
    public final double x, y; // mario x, y coordinates within a block
    public final boolean onGround;
    public final boolean cliff;
    public final boolean ableToJump;

    // TODO ACTUALLY IMPLEMENT fromInt()
    private int pre = -1;

    public QLState(byte[][] field, byte[][] enemies, float[] marioFloatPos, boolean onGround, boolean cliff,
            boolean ableToJump) {
        this.field = new byte[19][19];
        for (int i = 0; i < 19; ++i) {
            this.field[i] = field[i].clone();
        }
        this.enemies = new byte[19][19];
        for (int i = 0; i < 19; ++i) {
            this.enemies[i] = enemies[i].clone();
        }
        this.x = marioFloatPos[0] / 16 - Math.floor(marioFloatPos[0] / 16);
        this.y = marioFloatPos[1] / 16 - Math.floor(marioFloatPos[1] / 16);
        this.onGround = onGround;
        this.cliff = cliff;
        this.ableToJump = ableToJump;
    }

    // public QLState(int pre) {
    // this.field = new byte[19][19];
    // this.enemies = new byte[19][19];
    // this.x = 0;
    // this.y = 0;
    // this.onGround = false;
    // this.cliff = false;
    // this.ableToJump = false;

    // this.pre = pre;
    // }

    public int toInt() {
        if (pre >= 0)
            return pre;

        int state = 0;
        for (int j = 0; j < WIDTH; ++j) {
            if (enemies[marioEgoRow + j - 1][marioEgoCol + 1] != Sprite.KIND_NONE)
                state += 1 << (j);
        }
        for (int j = 0; j < WIDTH; ++j) {
            if (field[marioEgoRow + j - 1][marioEgoCol + 1] != 0)
                state += 1 << (WIDTH + j);
        }
        for (int j = 0; j < WIDTH; ++j) {
            if (enemies[marioEgoRow + j - 1][marioEgoCol + 2] != Sprite.KIND_NONE)
                state += 1 << (2 * WIDTH + j);
        }
        for (int j = 0; j < WIDTH; ++j) {
            if (field[marioEgoRow + j - 1][marioEgoCol + 2] != 0)
                state += 1 << (3 * WIDTH + j);
        }
        state += onGround ? (1 << (4 * WIDTH)) : 0;
        state += cliff ? (1 << (4 * WIDTH + 1)) : 0;
        state += ableToJump ? (1 << (4 * WIDTH + 2)) : 0;
        return state;
    }

}
