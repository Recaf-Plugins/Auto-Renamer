package me.coley.recaf.plugin.rename.namegen;

import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

/**
 * Common string utils for class names.
 *
 * @author Matt Coley
 */
public class NameUtils {
	/**
	 * Capitalize the first letter of the name.
	 *
	 * @param name
	 * 		Name to modify.
	 *
	 * @return Capitalized name.
	 */
	public static String capitalize(String name) {
		if (name.length() == 1)
			return name.toUpperCase();
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * Lower case the first letter of the name.
	 *
	 * @param name
	 * 		Name to modify.
	 *
	 * @return Lower-cased name, assuming rest matches camel-case.
	 */
	public static String camel(String name) {
		if (name.length() == 1)
			return name.toLowerCase();
		return name.substring(0, 1).toLowerCase() + name.substring(1);
	}

	/**
	 * Package-stripped version of {@link #getParentName(Map, ClassNode)}.
	 *
	 * @param classMappings
	 * 		Mappings to pull latest mappings from.
	 * @param node
	 * 		Class to search.
	 *
	 * @return Parent name of class, or {@code null} if none found other than {@code java/lang/Object}.
	 */
	public static String getSimpleParentName(Map<String, String> classMappings, ClassNode node) {
		String name = getParentName(classMappings, node);
		if (name != null)
			name = name.substring(name.lastIndexOf('/') + 1);
		return name;
	}

	/**
	 * Find the name of class's parent, or first parent if it has multiple interfaces.
	 *
	 * @param classMappings
	 * 		Mappings to pull latest mappings from.
	 * @param node
	 * 		Class to search.
	 *
	 * @return Parent name of class, or {@code null} if none found other than {@code java/lang/Object}.
	 */
	public static String getParentName(Map<String, String> classMappings, ClassNode node) {
		// Use direct parent name
		if (hasSuperType(node)) {
			String superName = node.superName;
			if (superName != null && classMappings.containsKey(superName)) {
				superName = classMappings.get(superName);
			}
			return superName;
		}
		// Use first interface name
		if (!node.interfaces.isEmpty()) {
			String itf = node.interfaces.get(0);
			if (itf != null && classMappings.containsKey(itf)) {
				itf = classMappings.get(itf);
			}
			return itf;
		}
		// No parent
		return null;
	}

	private static boolean hasSuperType(ClassNode node) {
		return !node.superName.equals("java/lang/Object");
	}
}
