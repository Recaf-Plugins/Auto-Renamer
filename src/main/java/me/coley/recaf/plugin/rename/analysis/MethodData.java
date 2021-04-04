package me.coley.recaf.plugin.rename.analysis;

import org.objectweb.asm.Type;

import java.util.List;

/**
 * Minimal method data for analysis.
 *
 * @author Matt Coley
 * @author Aleksi Ermolaev
 */
public class MethodData {
	private final Type retType;
	private final List<String> references;
	private final int size;
	private boolean isNative;

	/**
	 * @param retType
	 * 		Method return type.
	 * @param references
	 * 		List of class references in order of appearance in the method code.
	 * @param size
	 * 		Length of method.
	 */
	public MethodData(Type retType, List<String> references, int size) {
		this.retType = retType;
		this.references = references;
		this.size = size;
	}

	/**
	 * @return List of class references in order of appearance in the method code.
	 */
	public List<String> getReferences() {
		return references;
	}

	/**
	 * @return Method return type.
	 */
	public Type getReturnType() {
		return retType;
	}

	/**
	 * @return Length of method.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @return If method is a stub for native callback. Should be tagged as native then.
	 */
	public boolean isNative() {
		return isNative;
	}

	/**
	 * Marks method as native.
	 */
	public void markNative() {
		isNative = true;
	}
}
