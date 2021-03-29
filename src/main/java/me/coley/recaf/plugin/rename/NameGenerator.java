package me.coley.recaf.plugin.rename;

import me.coley.recaf.control.Controller;
import me.coley.recaf.plugin.rename.namefilter.NamingScope;
import me.coley.recaf.plugin.rename.namefilter.ScopeFilter;
import me.coley.recaf.plugin.rename.namegen.NameStrategy;
import me.coley.recaf.plugin.rename.namegen.NamingPattern;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Name generator that delegates to the configured {@link NameStrategy} from the plugin's {@link NamingPattern} value.
 * Names that do not match the {@link ScopeFilter} from the plugin's {@link NamingScope} value.
 *
 * @author Matt Coley
 */
public class NameGenerator {
	private final ScopeFilter scopeFilter;
	private final NameStrategy namingStrategy;
	private final String packageOverride;

	/**
	 * @param controller
	 * 		Controller to pull classes from.
	 * @param plugin
	 * 		Plugin instance with config to pull.
	 * @param packageOverride
	 * 		Package name to put classes into. Must be {@code null} to keep existing package structures.
	 */
	public NameGenerator(Controller controller, AutoRename plugin, String packageOverride) {
		this.scopeFilter = plugin.namingScope.createFilter(plugin);
		this.namingStrategy = plugin.namingPattern.createStrategy(controller);
		this.packageOverride = packageOverride;
	}

	/**
	 * @param node
	 * 		Class to rename.
	 *
	 * @return New internal name, or {@code null} if the naming scope does not apply to the class.
	 */
	public String createClassName(ClassNode node) {
		// Skip if the current name does not match the target scope.
		String currentName = node.name;
		if (!scopeFilter.matches(currentName)) {
			return null;
		}
		// Create the new name for the class.
		String simpleName = namingStrategy.className(node);
		// Skip if the strategy failed to produce a name.
		if (simpleName == null) {
			return null;
		}
		// Put all renamed classes into the given package.
		if (packageOverride != null) {
			return packageOverride + simpleName;
		}
		// Place into existing package
		if (currentName.contains("/")) {
			String packageName = currentName.substring(0, currentName.lastIndexOf('/') + 1);
			return packageName + simpleName;
		}
		return simpleName;
	}

	/**
	 * @param owner
	 * 		Class defining the field.
	 * @param field
	 * 		Field to rename.
	 *
	 * @return New name, or {@code null} if the naming scope does not apply to the field.
	 */
	public String createFieldName(ClassNode owner, FieldNode field) {
		if (!scopeFilter.matches(field.name)) {
			return null;
		}
		return namingStrategy.fieldName(owner, field);
	}

	/**
	 * @param owner
	 * 		Class defining the method.
	 * @param method
	 * 		Method to rename.
	 *
	 * @return New name, or {@code null} if the naming scope does not apply to the method.
	 */
	public String createMethodName(ClassNode owner, MethodNode method) {
		if (!scopeFilter.matches(method.name)) {
			return null;
		}
		return namingStrategy.methodName(owner, method);
	}

	/**
	 * @param declaring
	 * 		Method declaring the variable.
	 * @param local
	 * 		Variable to rename.
	 *
	 * @return New name, or {@code null} if the naming scope does not apply to the variable.
	 */
	public String createVariableName(MethodNode declaring, LocalVariableNode local) {
		if (!scopeFilter.matches(local.name)) {
			return null;
		}
		return namingStrategy.variable(declaring, local);
	}
}
