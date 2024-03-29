package ch.idsia.agents;

public final class QLStateAction {

	public final QLState state;
	public final int action;

	public QLStateAction(QLState state, int action) {
		this.state = state;
		this.action = action;
	}

	public QLStateAction(long from) {
		this.state = new QLState(from % QLState.N_STATES);
		this.action = (int) (from / QLState.N_STATES);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof QLStateAction) {
			QLStateAction key = (QLStateAction) obj;
			return this.state.toLong() == key.state.toLong() && this.action == key.action;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (int) ((state.toLong() >> 16) + action);
	}

	public QLStateAction clone() {
		return new QLStateAction(state.clone(), action);
	}

	public long toLong() {
		return QLState.N_STATES * (long) action + state.toLong();
	}
}
