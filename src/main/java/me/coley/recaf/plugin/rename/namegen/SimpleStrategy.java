package me.coley.recaf.plugin.rename.namegen;

import me.coley.recaf.control.Controller;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A naming strategy that yields a basic pattern of incrementing numbers. IE, Class1, Class2, etc.
 *
 * @author Matt Coley
 */
public class SimpleStrategy implements NameStrategy {
	private final Controller controller;
	private int classIndex = 1;
	private int fieldIndex = 1;
	private int methodIndex= 1;

	public SimpleStrategy(Controller controller) {
		this.controller = controller;
	}

	@Override
	public String className(ClassNode node) {
		return "Class" + (classIndex++);
	}

	@Override
	public String fieldName(ClassNode owner, FieldNode field) {
		return "field" + (fieldIndex++);
	}

	@Override
	public String methodName(ClassNode owner, MethodNode method) {
		// TODO: Pass in controller context and ensure methods in a hierarchy don't get fucked up.
		return "method" + (methodIndex++);
	}

	@Override
	public String variable(MethodNode method, LocalVariableNode local) {
		return "local" + local.index;
	}
}
