package me.coley.recaf.plugin.rename.namegen;

import me.coley.recaf.control.Controller;
import me.coley.recaf.graph.inheritance.HierarchyGraph;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
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
	private final Map<String, Boolean> definesMethodCache = new ConcurrentHashMap<>();
	private final Map<String, String> classNameCache = new ConcurrentHashMap<>();
	private final Map<String, String> fieldNameCache = new ConcurrentHashMap<>();
	private final Map<String, String> methodNameCache = new ConcurrentHashMap<>();
	private final Set<String> warnedDupeNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Controller controller;
	private final HierarchyGraph graph;

	protected AbstractNameStrategy(Controller controller) {
		this.controller = controller;
		graph = controller.getWorkspace().getHierarchyGraph();
	}

	/**
	 * @param owner
	 * 		Class with hierarchy to check.
	 * @param method
	 * 		Method definition.
	 *
	 * @return {@code null} when no parent class has a mapping for the method.
	 * Otherwise result is the reserved mapping.
	 */
	protected String getParentMethodMappedName(ClassNode owner, MethodNode method) {
		Set<String> owners = graph.getHierarchyNames(owner.name);
		for (String className : owners) {
			// Skip parameter class
			if (className.equals(owner.name))
				continue;
			// Check if the class in the hierarchy contains the method
			if (classDefinesMethod(className, method)) {
				// Check if we have already mapped the method
				String methodKey = methodKey(className, method.name, method.desc);
				String mappedName = getMethodMapping(methodKey);
				if (mappedName != null)
					return mappedName;
			}
		}
		// No associated parent
		return null;
	}

	/***
	 * Check if a class defines the given method.
	 * @param className Name of class to check.
	 * @param method Method definition to check.
	 * @return {@code true} when it does. {@code false} otherwise.
	 */
	protected boolean classDefinesMethod(String className, MethodNode method) {
		String methodKey = methodKey(className, method.name, method.desc);
		// Check if we've already computed if the method has been defined in the class orn ot.
		// Need to use boxed type for nullability.
		Boolean cached = definesMethodCache.get(methodKey);
		if (cached == null) {
			ClassReader reader = controller.getWorkspace().getClassReader(className);
			cached = ClassUtil.containsMethod(reader, method.name, method.desc);
			definesMethodCache.put(methodKey, cached);
		}
		return cached;
	}

	/**
	 * Check if the method is an override of a library method.
	 *
	 * @param owner
	 * 		Class defining method.
	 * @param method
	 * 		Method definition.
	 *
	 * @return {@code true} if the method is a library method.
	 */
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

	/**
	 * Get a parent name from the class if any parents exist.
	 *
	 * @param node
	 * 		Class to look at parents of.
	 *
	 * @return Name of the class's parent or {@code null} if no parent type.
	 */
	protected String getParentName(ClassNode node) {
		return NameUtils.getParentName(classNameCache, node);
	}

	/**
	 * Get the current mapping of the given class name.
	 *
	 * @param name
	 * 		Class to map.
	 *
	 * @return Current mapped name, or itself if no mapping exists.
	 */
	protected String getCurrentClassName(String name) {
		return classNameCache.getOrDefault(name, name);
	}

	/**
	 * @param key
	 * 		Field mapping key.
	 *
	 * @return Current mapped name.
	 */
	protected String getFieldMapping(String key) {
		return fieldNameCache.get(key);
	}

	/**
	 * @param key
	 * 		Method mapping key.
	 *
	 * @return Current mapped name.
	 */
	protected String getMethodMapping(String key) {
		return methodNameCache.get(key);
	}

	/**
	 * Check if a given name is a key for current mappings.
	 *
	 * @param name
	 * 		Name to check.
	 *
	 * @return {@code true} when a mapping exists.
	 */
	protected boolean hasClassMapping(String name) {
		return classNameCache.containsKey(name);
	}

	/**
	 * Register the class mapping and ensure it isn't a duplicate entry.
	 *
	 * @param key
	 * 		Original class name.
	 * @param name
	 * 		New class name.
	 *
	 * @return Unique de-duplicated new class name.
	 */
	protected String addClassMapping(String key, String name) {
		// Prevent duplicates
		int counter = 1;
		String uniqueName = name;
		boolean dupe = false;
		while (classNameCache.containsValue(uniqueName)) {
			uniqueName = name + (counter++);
			dupe = true;
		}
		classNameCache.put(key, uniqueName);
		// Warn about dupes
		if (dupe && !warnedDupeNames.contains(name)) {
			Log.warn("Automatically mapped class '{}' -> '{}' " +
					"but the generated name is already used! Using '{}'", key, name, uniqueName);
			warnedDupeNames.add(name);
		}
		return uniqueName;
	}

	/**
	 * Register the field mapping and ensure it isn't a duplicate entry.
	 *
	 * @param key
	 * 		Field key value.
	 * @param name
	 * 		New field name.
	 *
	 * @return Unique de-duplicated new field name.
	 */
	protected String addFieldMapping(String key, String name) {
		// Prevent duplicates
		int counter = 1;
		String uniqueName = name;
		boolean dupe = false;
		while (fieldNameCache.containsValue(uniqueName)) {
			uniqueName = name + (counter++);
			dupe = true;
		}
		fieldNameCache.put(key, uniqueName);
		// Warn about dupes
		if (dupe && !warnedDupeNames.contains(name)) {
			Log.warn("Automatically mapped field '{}' -> '{}' " +
					"but the generated name already used! Using '{}'", key, name, uniqueName);
			warnedDupeNames.add(name);
		}
		return uniqueName;
	}

	/**
	 * Register the method mapping and ensure it isn't a duplicate entry.
	 *
	 * @param key
	 * 		Method key value.
	 * @param name
	 * 		New method name.
	 *
	 * @return Unique de-duplicated new method name.
	 */
	protected String addMethodMapping(String key, String name) {
		// Prevent duplicates
		int counter = 1;
		String uniqueName = name;
		boolean dupe = false;
		while (methodNameCache.containsValue(uniqueName)) {
			uniqueName = name + (counter++);
			dupe = true;
		}
		methodNameCache.put(key, uniqueName);
		// Warn about dupes
		if (dupe && !warnedDupeNames.contains(name)) {
			Log.warn("Automatically mapped method '{}' -> '{}' " +
					"but the generated name already used! Using '{}'", key, name, uniqueName);
			warnedDupeNames.add(name);
		}
		return uniqueName;
	}

	/**
	 * @return The workspace to pull class info from.
	 */
	protected Workspace getWorkspace() {
		return controller.getWorkspace();
	}


	/**
	 * Map a class + field pair definition to a pattern to use for lookups.
	 *
	 * @param owner
	 * 		Class.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field type.
	 *
	 * @return Key for lookups.
	 */
	protected static String fieldKey(String owner, String name, String desc) {
		return owner + "." + name + " " + desc;
	}

	/**
	 * Map a class + method pair definition to a pattern to use for lookups.
	 *
	 * @param owner
	 * 		Class.
	 * @param method
	 * 		Method.
	 *
	 * @return Key for lookups.
	 */
	protected static String methodKey(ClassNode owner, MethodNode method) {
		return methodKey(owner.name, method.name, method.desc);
	}

	/**
	 * Map a class + method pair definition to a pattern to use for lookups.
	 *
	 * @param owner
	 * 		Class name.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method type.
	 *
	 * @return Key for lookups.
	 */
	protected static String methodKey(String owner, String name, String desc) {
		return owner + "." + name + desc;
	}
}
