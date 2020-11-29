package ch.idsia.agents;

import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;

public final class QLState {

    public final static long FIELD_HEIGHT = 5;
    public final static long FIELD_WIDTH = 5;
    public final static int FIELD_RANGE_START_X = 9;
    public final static int FIELD_RANGE_START_Y = 7;
    public final static long ENEMIES_HEIGHT = 3;
    public final static long ENEMIES_WIDTH = 3;
    public final static int ENEMIES_RANGE_START_X = 9;
    public final static int ENEMIES_RANGE_START_Y = 8;
    private final static long N_DIM = (FIELD_HEIGHT * FIELD_WIDTH // field size
            + ENEMIES_HEIGHT * ENEMIES_WIDTH * 2 // field size for enemy information
            + 2 // マリオの位置情報
            + 2 // マリオの状態情報
    );
    public final static long N_STATES = (1L << N_DIM);

    // specific information about mario state, only available when
    // informationAvailable == true
    public final float marioX, marioY;
    public final boolean onGround;
    public final boolean ableToJump;
    public final boolean informationAvailable;

    private final long asLong;

    public QLState(byte[][] field, byte[][] enemies, float[] marioFloatPos, boolean onGround, boolean ableToJump) {

        this.marioX = marioFloatPos[0];
        this.marioY = marioFloatPos[1];
        this.onGround = onGround;
        this.ableToJump = ableToJump;
        this.informationAvailable = true;

        long val = 0;
        int startX = FIELD_RANGE_START_X, startY = FIELD_RANGE_START_Y;
        for (int i = 0; i < FIELD_HEIGHT; ++i) {
            for (int j = 0; j < FIELD_WIDTH; ++j) {
                switch (field[startY + i][startX + j]) {
                    case GeneralizerLevelScene.COIN_ANIM:
                    case GeneralizerLevelScene.LADDER:
                    case GeneralizerLevelScene.PRINCESS:
                    case 0:
                        break;
                    default:
                        val += (1L << (i * FIELD_WIDTH + j));
                }
            }
        }

        startX = ENEMIES_RANGE_START_X;
        startY = ENEMIES_RANGE_START_Y;
        for (int i = 0; i < ENEMIES_HEIGHT; ++i) {
            for (int j = 0; j < ENEMIES_WIDTH; ++j) {
                long mult = 0;
                switch (enemies[startY + i][startX + j]) {
                    case Sprite.KIND_GOOMBA:
                    case Sprite.KIND_RED_KOOPA:
                    case Sprite.KIND_GREEN_KOOPA:
                    case Sprite.KIND_BULLET_BILL:
                    case Sprite.KIND_SHELL:
                    case Sprite.KIND_FIRE_FLOWER:
                    case Sprite.KIND_ENEMY_FLOWER:
                        mult = 1;
                        break;
                    case Sprite.KIND_WAVE_GOOMBA:
                    case Sprite.KIND_GOOMBA_WINGED:
                    case Sprite.KIND_RED_KOOPA_WINGED:
                    case Sprite.KIND_GREEN_KOOPA_WINGED:
                        mult = 2;
                        break;
                    case Sprite.KIND_SPIKY:
                    case Sprite.KIND_SPIKY_WINGED:
                        mult = 3;
                        break;
                    default:
                }

                val += mult * (1L << (FIELD_HEIGHT * FIELD_WIDTH + 2 * i * ENEMIES_WIDTH + 2 * j));
            }
        }

        final long nFieldDim = FIELD_HEIGHT * FIELD_WIDTH + ENEMIES_HEIGHT * ENEMIES_WIDTH * 2;
        final double x = marioFloatPos[0], y = marioFloatPos[1];
        // 0 1 2 3
        final int nX = (int) Math.round(Math.floor((x / 16.0 - Math.floor(x / 16.0)) * 4));
        final int nY = (int) Math.round(Math.floor((y / 16.0 - Math.floor(y / 16.0)) * 4));
        // val += (nX < 2) ? (1L << (nFieldDim)) : 0;
        // val += (nY < 2) ? (1L << (nFieldDim + 1)) : 0;

        val += onGround ? (1L << (nFieldDim + 2)) : 0;
        val += ableToJump ? (1L << (nFieldDim + 3)) : 0;
        asLong = val;
    }

    public QLState(long asLong) {
        this.marioX = 0;
        this.marioY = 0;
        this.onGround = false;
        this.ableToJump = false;
        this.informationAvailable = false;

        this.asLong = asLong;
    }

    public QLState clone() {
        QLState clone = new QLState(this.asLong);
        return clone;
    }

    public long toLong() {
        return asLong;
    }

    public void assertInformationIsAvailable() {
        if (!informationAvailable) {
            System.err.println("something went wrong");
            throw new AssertionError();
        }
    }

}
