package ch.idsia.agents;

import java.util.*;

public final class QLStateActionPair {

	private final QLState state;
	private final int action;

	public QLStateActionPair(QLState state, int action) {
		this.state = state;
		this.action = action;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof QLStateActionPair) {
			QLStateActionPair key = (QLStateActionPair) obj;
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
