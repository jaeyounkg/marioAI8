package ch.idsia.agents;

import java.util.*;

public final class MCStateActionPair {

	private final MCState state;
	private final int action;

	public MCStateActionPair(MCState state, int action) {
		this.state = state;
		this.action = action;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MCStateActionPair) {
			MCStateActionPair key = (MCStateActionPair) obj;
			return this.state.toInt() == key.state.toInt() && this.action == key.action;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return state.toInt() / 16 + action;
	}

	public MCState getState() {
		return state;
	}

	public int getAction() {
		return action;
	}
}
