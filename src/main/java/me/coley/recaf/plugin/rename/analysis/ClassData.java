package me.coley.recaf.plugin.rename.analysis;

import java.util.List;
import java.util.Objects;

/**
 * Minimal class data for analysis.
 *
 * @author Matt Coley
 * @author Aleksi Ermolaev
 */
public class ClassData {
	private final String name;
	private final List<String> hierarchy;
	private final List<FieldData> fields;
	private final List<MethodData> methods;

	/**
	 * @param name
	 * 		Class name.
	 * @param hierarchy
	 * 		All parents of this class.
	 * @param fields
	 * 		Declared type of fields &amp; all types referenced by annotations on the fields.
	 * @param methods
	 * 		Declared return type of methods &amp; all types referenced by the method in order.
	 */
	public ClassData(String name, List<String> hierarchy, List<FieldData> fields, List<MethodData> methods) {
		this.name = name;
		hierarchy.removeIf(Objects::isNull);
		this.hierarchy = hierarchy;
		this.fields = fields;
		this.methods = methods;
	}

	/**
	 * @return Class name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return All parents of this class.
	 */
	public List<String> getHierarchy() {
		return hierarchy;
	}

	/**
	 * Declared type of fields &amp; all types referenced by annotations on the fields.
	 *
	 * @return Field wrappers.
	 */
	public List<FieldData> getFields() {
		return fields;
	}

	/**
	 * Declared return type of methods &amp; all types referenced by the method in order.
	 *
	 * @return Method wrappers.
	 */
	public List<MethodData> getMethods() {
		return methods;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ClassData)
			return name.equals(((ClassData) other).name);
		return false;
	}
}
