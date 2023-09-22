package ru.runa.gpd.algorithms.cycles;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.Transition;
import ru.runa.gpd.lang.model.bpmn.ExclusiveGateway;
import ru.runa.gpd.lang.model.bpmn.ParallelGateway;

public class CheckInstantCycleAlgorithm {
	
	private List<CycleNode> graph;
	private List<CycleNode> cycle;
	
	public CheckInstantCycleAlgorithm(List<Node> nodes, List<Transition> transitions) {
		this.graph = toCycleNodes(nodes, transitions);
		graph.sort(null);
	}
	
	private List<CycleNode> toCycleNodes(List<Node> nodes, List<Transition> transitions) {
		Map<Node, CycleNode> mapGraph = new HashMap<>();
		for (int i = 0; i < nodes.size(); ++i) {
			if (nodes.get(i) instanceof ParallelGateway) mapGraph.put(nodes.get(i), new ParallelCycleNode(nodes.get(i), i));
			else mapGraph.put(nodes.get(i), new CycleNode(nodes.get(i), i));
		}
		for (Transition transition: transitions) {
			CycleNode source = mapGraph.get(transition.getSource());
			CycleNode target = mapGraph.get(transition.getTarget());
			if (target instanceof ParallelCycleNode) {
				Node empty = new ExclusiveGateway();
				CycleNode emptyNode = new CycleNode(empty, mapGraph.size());
				mapGraph.put(empty, emptyNode);
				source.addTarget(emptyNode);
				emptyNode.addTarget(target);
				((ParallelCycleNode)target).addParent(emptyNode);
			} else {
				source.addTarget(target);
			}
		}
		return new ArrayList<>(mapGraph.values());
	}
	
	public void start() {
		GraphOfStates tree = new GraphOfStates(graph);
		tree.createTree();
		cycle = tree.getInstantCycle();
	}
	
	public boolean hasInstantCycle() {
		return cycle != null;
	}

	public String getCycleIds() {
		if (cycle != null) {
			StringBuilder message = new StringBuilder();
			for (CycleNode node: cycle) 
				if (node.getSource().getId() != null) message.append(node.getSource().getId() + ", ");
			message.replace(message.length() - 2, message.length() - 1, "");
			return message.toString();
		}
		return "";
	}
}
