package me.coley.recaf.plugin.rename.namegen;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Outline for generating new names for items.
 *
 * @author Matt Coley
 */
public interface NameStrategy {
	/**
	 * @param node Class to create a name for.
	 * @return Name for the class.
	 */
	String className(ClassNode node);

	/**
	 * @param owner Class that defines the field.
	 * @param field Field to create a name for.
	 * @return Name for the field.
	 */
	String fieldName(ClassNode owner, FieldNode field);

	/**
	 * @param owner Class that defines the field.
	 * @param method Method to create a name for.
	 * @return Name for the method.
	 */
	String methodName(ClassNode owner, MethodNode method);

	/**
	 * @param method Method that defines the variable.
	 * @param local Variable to create a name for.
	 * @return Name for the variable.
	 */
	String variable(MethodNode method, LocalVariableNode local);
}
