package ru.runa.gpd.algorithms.cycles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphState implements Comparable {
	
	private List<Integer> state;
	private Set<GraphState> nextStates = new HashSet<>();
	private boolean wasVisitedOnce = false;
	private int weight = 0;
	private int stops = 0;
	
	public GraphState(List<CycleNode> containBreakpoint, int size) {
		state = new ArrayList<>();
		for (int i = 0; i < size; ++i) state.add(0);
		for (CycleNode node : containBreakpoint) {
			if (weight == 0 && node.getClassification() != NodeClassification.INSTANT) this.weight = 1;
			state.set(node.getId(), state.get(node.getId()) + 1);
		}
	}
	
	public void setStops(int previousStops) {
		if (!wasVisitedOnce) stops = previousStops + weight;
		wasVisitedOnce = true;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public int getStops() {
		return stops;
	}
	
	public List<Integer> getState() {
		return state;
	}
	
	public void addNextState(GraphState next) {
		nextStates.add(next);
	}
	
	public Set<GraphState> getNextStates() {
		return nextStates;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GraphState)) return false;
		GraphState obj = (GraphState)o;
		if (this == o) return true;
		if (state.size() != obj.state.size()) return false;
		boolean result = true;
		for (int i = 0; i < state.size(); ++i) result &= state.get(i).equals(obj.state.get(i));
		return result;
		
	}

	@Override
	public int compareTo(Object o) {
		return stops - ((GraphState)o).stops;
	}
}

