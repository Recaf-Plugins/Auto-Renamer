package me.coley.recaf.plugin.rename.namegen;

import me.coley.recaf.control.Controller;
import me.coley.recaf.graph.inheritance.HierarchyGraph;
import me.coley.recaf.util.ClassUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common base for name strategies with some caching capabilities.
 *
 * @author Matt Coley
 */
public abstract class AbstractNameStrategy implements NameStrategy {
	private final Map<String, Boolean> isLibraryMethodCache = new ConcurrentHashMap<>();
	private final Map<String, String> methodNameCache = new ConcurrentHashMap<>();
	private final Controller controller;
	private final HierarchyGraph graph;

	protected AbstractNameStrategy(Controller controller) {
		this.controller = controller;
		graph = controller.getWorkspace().getHierarchyGraph();
	}

	protected void putMethodMapping(String methodKey, String mapped) {
		methodNameCache.put(methodKey, mapped);
	}

	protected String getParentMethodMappedName(ClassNode owner, MethodNode method) {
		Set<String> owners = graph.getHierarchyNames(owner.name);
		for (String className : owners) {
			// Skip parameter class
			if (className.equals(owner.name))
				continue;
			// Check if the class in the hierarchy contains the method
			ClassReader reader = controller.getWorkspace().getClassReader(className);
			if (ClassUtil.containsMethod(reader, method.name, method.desc)) {
				// Check if we have already mapped the method
				String methodKey = methodKey(reader.getClassName(), method.name, method.desc);
				String mappedName = methodNameCache.get(methodKey);
				if (mappedName != null)
					return mappedName;
			}
		}
		// No associated parent
		return null;
	}

	protected boolean isLibrary(ClassNode owner, MethodNode method) {
		String methodKey = methodKey(owner, method);
		// Check if we've already computed if the method is a library one or not.
		// Need to use boxed type for nullability.
		Boolean cached = isLibraryMethodCache.get(methodKey);
		if (cached == null) {
			cached = graph.isLibrary(owner.name, method.name, method.desc);
			isLibraryMethodCache.put(methodKey, cached);
		}
		return cached;
	}

	private static String methodKey(ClassNode owner, MethodNode method) {
		return methodKey(owner.name, method.name, method.desc);
	}

	private static String methodKey(String owner, String name, String desc) {
		return owner + "." + name + desc;
	}
}
