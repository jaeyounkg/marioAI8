
package ch.idsia.agents.controllers;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;

/**
 * Basically a forward moving agent that only stops when it's going to fall
 */

public class MainTask2Agent extends BasicMarioAIAgent implements Agent {
    int trueJumpCounter = 0;
    int trueSpeedCounter = 0;

    int state = 0;
    int stateCounter = 0;
    static final int STATE_BEGINNING = 0; // falling from the sky in the beginning
    static final int STATE_NORMAL = 1; // including "normal" jumping
    static final int STATE_GETTING_READY_FOR_JUMPING = 2;
    static final int STATE_JUMPING = 3; // jumping over a hole, trying to reach as far as possible

    public MainTask2Agent() {
        super("MainTask2Agent");
        reset();
    }

    public void reset() {
        action = new boolean[Environment.numberOfKeys];
    }

    public boolean[] getAction() {
        if (state == STATE_BEGINNING) {
            if (stateCounter >= 8) {
                changeStateTo(STATE_NORMAL);
            }
        } else if (state == STATE_NORMAL) {
            action[Mario.KEY_RIGHT] = true;
            action[Mario.KEY_LEFT] = false;
            if (isObstacle(marioEgoRow, marioEgoCol + 1)) {
                action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
            }

            if (isGoingToFall()) {
                changeStateTo(STATE_GETTING_READY_FOR_JUMPING);
            }
        } else if (state == STATE_GETTING_READY_FOR_JUMPING) {
            if (isMarioOnGround) {
                changeStateTo(STATE_JUMPING);
                action[Mario.KEY_JUMP] = false;
            } else {
                if (stateCounter < 3) {
                    action[Mario.KEY_RIGHT] = false;
                    action[Mario.KEY_LEFT] = true;
                    action[Mario.KEY_JUMP] = false;
                } else {
                    action[Mario.KEY_RIGHT] = false;
                    action[Mario.KEY_LEFT] = false;
                    action[Mario.KEY_JUMP] = false;
                }
            }
        } else if (state == STATE_JUMPING) {
            action[Mario.KEY_RIGHT] = true;
            action[Mario.KEY_LEFT] = false;
            action[Mario.KEY_JUMP] = true;

            if (stateCounter > 10 && isMarioOnGround) {
                changeStateTo(STATE_NORMAL);
            }
        }
        stateCounter++;
        return action;
    }

    private void changeStateTo(int toState) {
        state = toState;
        stateCounter = 0;
    }

    private boolean isObstacle(int r, int c) {
        return getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.BRICK
                || getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH
                || getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.FLOWER_POT_OR_CANNON
                || getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.LADDER;
    }

    private boolean isGoingToFall() {
        for (int i = marioEgoRow; i < receptiveFieldHeight; i++) {
            if (getReceptiveFieldCellValue(i, marioEgoCol + 1) != 0) {
                return false;
            }
        }
        return true;
    }

}
