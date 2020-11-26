package ch.idsia.agents;

import java.util.*;

import ch.idsia.agents.QLStateAction;

public class QLQFunction extends HashMap<QLStateAction, Double> {

    public QLQFunction clone() {
        QLQFunction clone = new QLQFunction();
        for (Entry<QLStateAction, Double> entry : this.entrySet()) {
            clone.put(entry.getKey().clone(), entry.getValue());
        }
        return clone;
    }
}
