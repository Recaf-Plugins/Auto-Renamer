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

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main handler for creating new names and applying them.
 *
 * @author Matt Coley
 */
public class Processor {
	private final Map<String, String> mappings = new ConcurrentHashMap<>();
	private final ExecutorService service = Executors.newFixedThreadPool(getThreadCount());
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
		// Analyze each class
		// TODO: Do this in multiple phases instead
		//  - rename classes
		//  - rename fields
		//      - can utilize renamed class names
		//  - rename methods
		//      - can utilize renamed class/field names
		for (String name : matchedNames) {
			service.submit(() -> {
				ClassReader cr = controller.getWorkspace().getClassReader(name);
				if (cr == null) {
					Log.warn("AutoRenamer failed to read class from workspace: " + name);
					return;
				}
				ClassNode node = ClassUtil.getNode(cr, ClassReader.SKIP_FRAMES);
				analyzeClass(node);
			});
		}
	}

	private void analyzeClass(ClassNode node) {
		// Class name
		String oldClassName = node.name;
		String newClassName = generator.createClassName(node);
		if (newClassName != null) {
			mappings.put(oldClassName, newClassName);
		}
		// Field names
		for (FieldNode field : node.fields) {
			String oldFieldName = field.name;
			String newFieldName = generator.createFieldName(node, field);
			if (newFieldName != null) {
				mappings.put(oldClassName + "." + oldFieldName + " " + field.desc, newFieldName);
			}
		}
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

	private static int getThreadCount() {
		return Runtime.getRuntime().availableProcessors();
	}
}
