package ch.idsia.agents;

import java.util.*;

public final class QLStateAction {

	public final QLState state;
	public final int action;

	public QLStateAction(QLState state, int action) {
		this.state = state;
		this.action = action;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof QLStateAction) {
			QLStateAction key = (QLStateAction) obj;
			return this.state.toInt() == key.state.toInt() && this.action == key.action;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (state.toInt() >> 4) + action;
	}

	public QLState getState() {
		return state;
	}

	public int getAction() {
		return action;
	}
}
