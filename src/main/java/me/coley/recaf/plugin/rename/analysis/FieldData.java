package me.coley.recaf.plugin.rename.analysis;

import org.objectweb.asm.Type;

import java.util.List;

/**
 * Minimal Field data for analysis.
 *
 * @author Matt Coley
 * @author Aleksi Ermolaev
 */
public class FieldData {
	private final Type type;
	private final List<String> annotationReferences;

	/**
	 * @param type
	 * 		Field declared type.
	 * @param annotationReferences
	 * 		List of class references to annotations on the field.
	 */
	public FieldData(Type type, List<String> annotationReferences) {
		this.type = type;
		this.annotationReferences = annotationReferences;
	}

	/**
	 * @return List of class references to annotations on the field.
	 */
	public List<String> getAnnotationReferences() {
		return annotationReferences;
	}

	/**
	 * @return Field declared type.
	 */
	public Type getType() {
		return type;
	}
}
