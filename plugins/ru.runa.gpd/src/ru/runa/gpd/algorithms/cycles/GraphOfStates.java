package ru.runa.gpd.algorithms.cycles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.lang.model.bpmn.ParallelGateway;

public class GraphOfStates {

	private List<CycleNode> nodes;
	private List<CycleNode> cycle;
	private GraphState root;
	private List<GraphState> initilizedStates = new ArrayList<>();
	
	
	public GraphOfStates(List<CycleNode> nodes) {
		this.nodes = nodes;
		List<CycleNode> startState = new ArrayList<>();
		for (CycleNode node : nodes) if (node.getSource() instanceof StartState) {
			startState.add(node);
			break;
		}
		root = new GraphState(startState, nodes.size());
		root.setStops(0);
		initilizedStates.add(root);
	}
	
	public void createTree() {
		createTree(root);
	}
	
	private void createTree(GraphState currentState) {
		
		// Get all nodes with breakpoint
		List<CycleNode> sources = new ArrayList<>();
		for (int i = 0; i < nodes.size(); ++i) {
			int breackPointCount = currentState.getState().get(i);
			while (breackPointCount > 0) {
				sources.add(nodes.get(i));
				breackPointCount--;
			}
		}
		
		// Find all combination of next states
		List<List<CycleNode>> recievers = new ArrayList<>();
		List<List<CycleNode>> toRemove = new ArrayList<>();
		List<List<CycleNode>> toAdd = new ArrayList<>();
		List<CycleNode> ignoredSources = new ArrayList<>();
		recievers.add(new ArrayList<>());
		
		for (CycleNode source : sources) {
			if (ignoredSources.contains(source)) continue;
			// When source is parallel all states will be in one branch
			if (source.getSource() instanceof ParallelGateway) {
				for (List<CycleNode> reciever : recievers) reciever.addAll(source.getTargets());
			} else {
				for (CycleNode target : source.getTargets()) {
					for (List<CycleNode> reciever : recievers) {
						List<CycleNode> updated = cloneNodes(reciever);
						toAdd.add(updated);
						// When target is parallel check that it can be reached from current state and update by rules
						if (target instanceof ParallelCycleNode) {
							if (((ParallelCycleNode) target).canBeReached(sources)) {
								updated.add(target);
								ignoredSources.addAll(((ParallelCycleNode) target).getParents());
							} else {
								updated.add(source);
							}
						} else {
							// When nothing is parallel make all possible combinations of next iteration
							updated.add(target);
						}
						toRemove.add(reciever);
					}
				}
				recievers.removeAll(toRemove);
				recievers.addAll(toAdd);
				toAdd.clear();
			}
		}
		
		for (List<CycleNode> reciever : recievers) if (reciever.isEmpty()) toRemove.add(reciever);
		recievers.removeAll(toRemove);
		
		List<GraphState> states = new ArrayList<>();
		for (List<CycleNode> reciever : recievers) {
			List<GraphState> nextStates = createNextStates(reciever, currentState);
			states.addAll(nextStates);
		}
		
		for (GraphState state : states) {
			int containedIndex = initilizedStates.indexOf(state);
			if (containedIndex != -1) {
				if (isDeadlocked(state, currentState)) return;
				state = initilizedStates.get(containedIndex);
			}
			else initilizedStates.add(state);
			currentState.addNextState(state);
			if (containedIndex == -1) createTree(state);
		}
	}
	
	private boolean isDeadlocked(GraphState nextState, GraphState previousState) {
		if (!nextState.equals(previousState)) return false;
		boolean deadlock = true;
		for (int i = 0; i < nodes.size(); ++i) {
			if (nextState.getState().get(i) > 0) {
				for (CycleNode target : nodes.get(i).getTargets()) deadlock &= target instanceof ParallelCycleNode;
			}
		}
		return deadlock;
	}
	
	private List<GraphState> createNextStates(List<CycleNode> recievers, GraphState previousState) {
		List<CycleNode> instant = new ArrayList<>();
		List<CycleNode> stop = new ArrayList<>();
		for (CycleNode reciever : recievers) {
			if (reciever.getClassification() == NodeClassification.INSTANT) {
				instant.add(reciever);
			} else {
				stop.add(reciever);
			}
		}
		List<GraphState> states = new ArrayList<>();
		if (instant.isEmpty()) states.add(new GraphState(stop, nodes.size()));
		else if (stop.isEmpty()) states.add(new GraphState(instant, nodes.size()));
		else {
			List<CycleNode> allRecievers = cloneNodes(instant);
			allRecievers.addAll(stop);
			states.add(new GraphState(instant, nodes.size()));
			states.add(new GraphState(allRecievers, nodes.size()));
		}
		return states;
	}
	
	private List<CycleNode> cloneNodes(List<CycleNode> toClone) {
		List<CycleNode> clone = new ArrayList<>();
		for (CycleNode node : toClone) clone.add(node);
		return clone;
	}
	
	private List<GraphState> cloneStates(List<GraphState> toClone) {
		List<GraphState> clone = new ArrayList<>();
		for (GraphState node : toClone) clone.add(node);
		return clone;
	}

	
	private Set<CycleNode> cloneSet(Set<CycleNode> toClone) {
		Set<CycleNode> clone = new HashSet<>();
		for (CycleNode node : toClone) clone.add(node);
		return clone;
	}
	
	public List<CycleNode> getInstantCycle() {
		cycle = initInstantCycle(new ArrayList<>(), root, 0);
		if (cycle != null) removeDeadEnds(cycle);
		return cycle;
	}

	
	private List<CycleNode> initInstantCycle(List<GraphState> visitedStates, GraphState current, int stops) {
		if (visitedStates.contains(current)) {
			if (current.getStops() >= stops) return findInstantCycle(visitedStates, current);
			else return null;
		} 
		visitedStates.add(current);
		Set<GraphState> nextStates = current.getNextStates();
		Deque<GraphState> invocations = new ArrayDeque<>();
		for (GraphState next : nextStates) {
			next.setStops(stops);
			if (next.getStops() > stops) invocations.addLast(next);
			else invocations.addFirst(next);
		}
		List<CycleNode> loop = null;
		while (!invocations.isEmpty() && loop == null) {
			GraphState next = invocations.pollFirst();
			List<GraphState> updatedVisited = cloneStates(visitedStates);
			loop = initInstantCycle(updatedVisited, next, stops + next.getWeight());
		}
		return loop;
	}
	
	private List<CycleNode> findInstantCycle(List<GraphState> visitedStates, GraphState lastRepeat) {
		List<CycleNode> loop = new ArrayList<>(); 
		for (int i = visitedStates.size() - 1; i >= 0; --i) {
			for (int j = 0; j < nodes.size(); ++j) if (visitedStates.get(i).getState().get(j) > 0) loop.add(nodes.get(j));
			if (visitedStates.get(i).equals(lastRepeat)) break;
		}
		return loop;
	}
	
	private void removeDeadEnds(List<CycleNode> potentialCycle) {
		List<CycleNode> toRemove = new ArrayList<>();
		for (CycleNode node : potentialCycle) {
			if (!canReachEveryone(potentialCycle, node)) toRemove.add(node);
		}
		potentialCycle.removeAll(toRemove);
	}
	
	private boolean canReachEveryone(List<CycleNode> cycle, CycleNode current) {
		Map<CycleNode, Boolean> isReached = new HashMap<>();
		for (CycleNode node : cycle) {
			isReached.put(node, false);
		}
		Deque<CycleNode> invocationList = new ArrayDeque<>();
		invocationList.add(current);
		while(!invocationList.isEmpty()) {
			CycleNode source = invocationList.pollFirst();
			isReached.put(source, true);
			for (CycleNode target : source.getTargets()) {
				if (isReached.keySet().contains(target) && !isReached.get(target)) {
					invocationList.add(target);
				}
			}
		}
		boolean isInCycle = true;
		for (CycleNode node : isReached.keySet()) {
			isInCycle &= isReached.get(node);
		}
		return isInCycle;
	}

}
