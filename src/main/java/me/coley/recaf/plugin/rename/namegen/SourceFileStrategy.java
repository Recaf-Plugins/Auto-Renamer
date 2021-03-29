package me.coley.recaf.plugin.rename.namegen;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

public class SourceFileStrategy implements NameStrategy {
	@Override
	public String className(ClassNode node) {
		return null;
	}

	@Override
	public String fieldName(ClassNode owner, FieldNode field) {
		return null;
	}

	@Override
	public String methodName(ClassNode owner, MethodNode method) {
		return null;
	}

	@Override
	public String variable(MethodNode method, LocalVariableNode local) {
		return null;
	}
}
