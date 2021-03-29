package me.coley.recaf.plugin.rename.namegen;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

public interface NameStrategy {
	String className(ClassNode node);

	String fieldName(ClassNode owner, FieldNode field);

	String methodName(ClassNode owner, MethodNode method);

	String variable(MethodNode method, LocalVariableNode local);
}
