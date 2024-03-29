package ch.idsia.agents;

import java.util.*;

import ch.idsia.benchmark.mario.engine.sprites.Sprite;

public final class DQLStateAction {

	// public final static int N_DIM = QLState.N_DIM + QLAction.N_DIM;

	private final DQLState state;
	private final DQLAction action;

	public DQLStateAction(DQLState state, DQLAction action) {
		this.state = state;
		this.action = action;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DQLStateAction) {
			DQLStateAction key = (DQLStateAction) obj;
			return this.state.toInt() == key.state.toInt() && this.action == key.action;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (state.toInt() >> 4) + action.toInt();
	}

	public DQLState getState() {
		return state;
	}

	public DQLAction getAction() {
		return action;
	}

	// public double[] toVector() {
	// double[] vec = new double[N_DIM];

	// int startX = 8, startY = 6;
	// for (int i = 0; i < RANGE; ++i) {
	// for (int j = 0; j < RANGE; ++j) {
	// vec[i * RANGE + j] = (state.field[startY + i][startX + j] != 0) ? 1 : 0;
	// }
	// }
	// for (int i = 0; i < RANGE; ++i) {
	// for (int j = 0; j < RANGE; ++j) {
	// vec[RANGE * RANGE + i * RANGE + j] = (state.enemies[startY + i][startX + j]
	// != Sprite.KIND_NONE) ? 1
	// : 0;
	// }
	// }
	// vec[RANGE * RANGE * 2] = state.onGround ? 1 : 0;
	// vec[RANGE * RANGE * 2 + 1] = state.cliff ? 1 : 0;
	// vec[RANGE * RANGE * 2 + 2] = state.ableToJump ? 1 : 0;
	// vec[RANGE * RANGE * 2 + 3] = state.x;
	// vec[RANGE * RANGE * 2 + 4] = state.y;

	// // JUMP, SPEED, RIGHT, LEFT, DOWN
	// vec[N_STATE_DIM] = (action == 0 || (action > 4 && action < 11)) ? 1 : 0;
	// vec[N_STATE_DIM + 1] = (action == 1 || action == 5 || action == 9 || action
	// == 10) ? 1 : 0;
	// vec[N_STATE_DIM + 2] = (action == 2 || action == 6 || action == 9) ? 1 : 0;
	// vec[N_STATE_DIM + 3] = (action == 3 || action == 7 || action == 10) ? 1 : 0;
	// vec[N_STATE_DIM + 4] = (action == 4 || action == 8) ? 1 : 0;

	// return vec;
	// }
}
