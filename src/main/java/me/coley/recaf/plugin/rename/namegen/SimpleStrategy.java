package me.coley.recaf.plugin.rename.namegen;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

public class SimpleStrategy implements NameStrategy {
	private int classIndex = 1;
	private int fieldIndex = 1;
	private int methodIndex= 1;

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
