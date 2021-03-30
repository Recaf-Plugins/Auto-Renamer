package me.coley.recaf.plugin.rename;

import me.coley.recaf.control.Controller;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Main handler for creating new names and applying them.
 *
 * @author Matt Coley
 */
public class Processor {
	private final Map<String, String> mappings = new ConcurrentHashMap<>();
	private final Controller controller;
	private final AutoRename plugin;
	private final NameGenerator generator;

	/**
	 * @param controller
	 * 		Controller with workspace to pull classes from.
	 * @param plugin
	 * 		Plugin with config values.
	 */
	public Processor(Controller controller, AutoRename plugin) {
		this.controller = controller;
		this.plugin = plugin;
		// Configure name generator
		String packageName = plugin.keepPackageLayout ? null : AutoRename.FLAT_PACKAGE_NAME;
		generator = new NameGenerator(controller, plugin, packageName);
	}

	/**
	 * Analyze the given classes and create new names for them and their members.
	 *
	 * @param matchedNames
	 * 		Set of class names to analyze.
	 */
	public void analyze(Set<String> matchedNames) {
		// Reset mappings
		mappings.clear();
		// Analyze each class in separate phases
		// Phase 0: Prepare class nodes
		Set<ClassNode> nodes = collectNodes(matchedNames);
		// Phase 1: Create mappings for class names
		//  - following phases can use these names to enrich their naming logic
		pooled("Analyze: Class names", service -> {
			for (ClassNode node : nodes) {
				service.submit(() -> analyzeClass(node));
			}
		});
		// Phase 2: Create mappings for field names
		//  - methods can now use class and field names to enrich their naming logic
		pooled("Analyze: Field names", service -> {
			for (ClassNode node : nodes) {
				service.submit(() -> analyzeFields(node));
			}
		});
		// Phase 3: Create mappings for method names
		pooled("Analyze: Method names", service -> {
			for (ClassNode node : nodes) {
				service.submit(() -> analyzeMethods(node));
			}
		});
	}

	/**
	 * @param matchedNames Names of classes to collect.
	 * @return Set of nodes from the given names.
	 */
	private Set<ClassNode> collectNodes(Set<String> matchedNames) {
		Set<ClassNode> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
		pooled("Collect-Nodes", service -> {
			for (String name : matchedNames) {
				service.submit(() -> {
					ClassReader cr = controller.getWorkspace().getClassReader(name);
					if (cr == null) {
						Log.warn("AutoRenamer failed to read class from workspace: " + name);
						return;
					}
					ClassNode node = ClassUtil.getNode(cr, ClassReader.SKIP_FRAMES);
					nodes.add(node);
				});
			}
		});
		return nodes;
	}

	/**
	 * Generate mapping for class.
	 * @param node Class to rename.
	 */
	private void analyzeClass(ClassNode node) {
		// Class name
		String oldClassName = node.name;
		String newClassName = generator.createClassName(node);
		if (newClassName != null) {
			mappings.put(oldClassName, newClassName);
		}
	}

	/**
	 * Generate mappings for field names.
	 * @param node Class with fields to rename.
	 */
	private void analyzeFields(ClassNode node) {
		// Class name
		String oldClassName = node.name;
		// Field names
		for (FieldNode field : node.fields) {
			String oldFieldName = field.name;
			String newFieldName = generator.createFieldName(node, field);
			if (newFieldName != null) {
				mappings.put(oldClassName + "." + oldFieldName + " " + field.desc, newFieldName);
			}
		}
	}

	/**
	 * Generate mappings for method names.
	 * @param node Class with methods to rename.
	 */
	private void analyzeMethods(ClassNode node) {
		// Class name
		String oldClassName = node.name;
		// Method names
		for (MethodNode method : node.methods) {
			String oldMethodName = method.name;
			String newMethodName = generator.createMethodName(node, method);
			if (newMethodName != null) {
				mappings.put(oldClassName + "." + oldMethodName + method.desc, newMethodName);
			}
			// Method variable names
			if (!plugin.pruneDebugInfo) {
				for (LocalVariableNode local : method.localVariables) {
					String newLocalName = generator.createVariableName(method, local);
					// Locals do not get globally mapped, so we handle renaming them locally here
					if (newLocalName != null) {
						local.name = newLocalName;
					}
				}
			}
		}
	}

	/**
	 * Applies the mappings created from {@link #analyze(Set) the analysis phase}
	 * to the primary resource of the workspace
	 */
	public void apply() {
		SortedMap<String, String> sortedMappings = new TreeMap<>(mappings);
		Mappings mapper = new Mappings(controller.getWorkspace());
		mapper.setCheckFieldHierarchy(true);
		mapper.setCheckMethodHierarchy(true);
		if (plugin.pruneDebugInfo) {
			mapper.setClearDebugInfo(true);
		}
		mapper.setMappings(sortedMappings);
		mapper.accept(controller.getWorkspace().getPrimary());
	}

	/**
	 * Run a task that utilizes {@link ExecutorService} for parallel execution.
	 * Pooled
	 *
	 * @param phaseName Task name.
	 * @param task Task to run.
	 */
	private void pooled(String phaseName, Consumer<ExecutorService> task) {
		try {
			long start = System.currentTimeMillis();
			Log.info("AutoRename Processing: Task '{}' starting", phaseName);
			ExecutorService service;
			if (generator.allowMultiThread()) {
				service = Executors.newFixedThreadPool(getThreadCount());
			} else {
				service = Executors.newSingleThreadExecutor();
			}
			task.accept(service);
			service.shutdown();
			service.awaitTermination(plugin.phaseTimeout, TimeUnit.SECONDS);
			Log.info("AutoRename Processing: Task '{}' completed in {}ms", phaseName, (System.currentTimeMillis() - start));
		} catch (Throwable t) {
			Log.error(t, "Failed processor phase '{}', reason: {}", phaseName, t.getMessage());
		}
	}

	private static int getThreadCount() {
		return Runtime.getRuntime().availableProcessors();
	}
}
