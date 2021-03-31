package me.coley.recaf.plugin.rename.namegen;

import me.coley.recaf.control.Controller;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A naming strategy that matches classes to the value of their {@code SourceFile} attribute.
 *
 * @author Matt Coley
 */
public class SourceFileStrategy extends AbstractNameStrategy {
	protected SourceFileStrategy(Controller controller) {
		super(controller);
	}

	@Override
	public String className(ClassNode node) {
		// Skip if the node is an inner class
		if (node.outerClass != null || node.outerMethod != null)
			return null;
		for (InnerClassNode inner : node.innerClasses) {
			if (inner.name.equals(node.name)) {
				return null;
			}
		}
		// Skip if there is no source or it does not contain a file name
		String sourceFile = node.sourceFile;
		if (sourceFile == null)
			return null;
		int dotIndex = sourceFile.indexOf('.');
		if (dotIndex == -1)
			return null;
		String baseName = sourceFile.substring(0, dotIndex);
		return addClassMapping(node.name, baseName);
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
