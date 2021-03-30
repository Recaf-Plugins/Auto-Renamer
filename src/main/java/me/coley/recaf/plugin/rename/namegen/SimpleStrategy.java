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
public class SimpleStrategy extends AbstractNameStrategy {
	private int classIndex = 1;
	private int fieldIndex = 1;
	private int methodIndex = 1;

	public SimpleStrategy(Controller controller) {
		super(controller);
	}

	@Override
	public boolean allowMultiThread() {
		// Because we are incrementing fields/methods in order we cannot have
		// two classes incrementing the same index.
		// This would cause both classes to appear to skip indices.
		return false;
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
		// Do not rename methods that belong/inherit from library classes
		if (isLibrary(owner, method)) {
			return null;
		}
		// Yield the name used by the rest of the method hierarchy
		String parentMapped = getParentMethodMappedName(owner, method);
		if (parentMapped != null) {
			return parentMapped;
		}
		// Create a new name
		return "method" + (methodIndex++);
	}

	@Override
	public String variable(MethodNode method, LocalVariableNode local) {
		return "local" + local.index;
	}
}
