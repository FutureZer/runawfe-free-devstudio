package ru.runa.gpd.algorithms.cycles;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import ru.runa.gpd.lang.model.EndState;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.lang.model.MultiTaskState;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.lang.model.TaskState;
import ru.runa.gpd.lang.model.Timer;
import ru.runa.gpd.lang.model.bpmn.*;

class CycleNode implements Comparable<CycleNode> {
	
	private int id;
	private Node source;
	private List<CycleNode> targets = new ArrayList<>();
	
	private final List<Class<?>> instantNodes = Arrays.asList(new Class<?>[] {
		StartState.class,
		BusinessRule.class,
		ScriptTask.class,
		ExclusiveGateway.class,
		ParallelGateway.class,
		Timer.class,
		ThrowEventNode.class,
		EndTokenState.class,
		EndState.class,
	});
	private final List<Class<?>> stopNodes = Arrays.asList(new Class<?>[] {
		TaskState.class,
		MultiTaskState.class,
		CatchEventNode.class,
	});
	
	public CycleNode(Node source, int index) {
		id = index;
		this.source = source;
	}
	
	public int getId() {
		return id;
	}
	
	public Node getSource() {
		return source;
	}
	
	public void addTarget(CycleNode node) {
		targets.add(node);
	}
	
	public List<CycleNode> getTargets() {
		return targets;
	}
	
	public NodeClassification getClassification() {
		if (instantNodes.contains(source.getClass())) return NodeClassification.INSTANT;
		else if (stopNodes.contains(source.getClass())) return NodeClassification.STOP;
		return NodeClassification.NOT_DEFINED;
	}

	@Override
	public int compareTo(CycleNode o) {
		return id - o.id;
	}
}
