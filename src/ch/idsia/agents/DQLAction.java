package ch.idsia.agents;

import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.Environment;

public final class DQLAction {

    public static final int N_ACTIONS = 5;
    public static final int N_DIM = 12;
    // J：ジャンプ S：ファイア R：右 L：左 D：下
    /*
     * enum Action{ J, S, R, L, D, JS, JR, JL, JD, JSR, JSL, NONE, }
     */

    public final boolean action[];
    public final int n;

    public DQLAction(int n) {
        action = new boolean[Environment.numberOfKeys];
        action[Mario.KEY_JUMP] = (n == 0 || (n > 4 && n < 11));
        action[Mario.KEY_SPEED] = (n == 1 || n == 5 || n == 9 || n == 10);
        action[Mario.KEY_RIGHT] = (n == 2 || n == 6 || n == 9);
        action[Mario.KEY_LEFT] = (n == 3 || n == 7 || n == 10);
        action[Mario.KEY_DOWN] = (n == 4 || n == 8);
        action[Mario.KEY_UP] = false;
        this.n = n;
    }

    public double[] toVector() {
        double[] vec = new double[N_DIM];
        vec[n] = 1;
        return vec;
    }

    public int toInt() {
        return n;
    }

}
