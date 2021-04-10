package me.coley.recaf.plugin.rename.analysis;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.Log;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.tribuo.Example;
import org.tribuo.classification.Label;
import org.tribuo.impl.ArrayExample;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class visitor to collect known type references.
 *
 * @author Matt Coley
 */
public class ClassDataCollector extends ClassVisitor {
	public static final String COL_N_UI = Classification.UI.columnName();
	public static final String COL_N_IO = Classification.IO.columnName();
	public static final String COL_N_NET = Classification.NETWORKING.columnName();
	public static final String COL_N_SECURITY = Classification.SECURITY.columnName();
	public static final String COL_N_BYTECODE = Classification.BYTECODE.columnName();
	public static final String COL_N_NATIVE = Classification.NATIVE.columnName();
	public static final String COL_N_DATABASE = Classification.DATABASE.columnName();
	public static final String COL_N_DISTRIBUTED = Classification.DISTRIBUTED.columnName();
	public static final String[] LABELS = new String[]{
			COL_N_UI, COL_N_IO, COL_N_NET, COL_N_SECURITY, COL_N_BYTECODE,
			COL_N_NATIVE, COL_N_DATABASE, COL_N_DISTRIBUTED
	};
	private static final Set<String> uncategorizedClasses = new HashSet<>();
	private final double[] counts = new double[LABELS.length];
	private final Map<String, Classification> packageLookup;

	public ClassDataCollector(Map<String, Classification> packageLookup) {
		super(Recaf.ASM_VERSION);
		this.packageLookup = packageLookup;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// Add type references for interfaces
		for (String itf : interfaces) {
			Type type = Type.getObjectType(itf);
			onTypeReference(type, 40);
		}
		// Add type references for parent type
		if (superName != null) {
			Type type = Type.getObjectType(superName);
			onTypeReference(type, 50);
		}
	}

	@Override
	public FieldVisitor visitField(int acc, String name, String desc, String s, Object v) {
		Type type = Type.getType(desc);
		onTypeReference(type, 5);
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc, String s, String[] e) {
		// Boost for native flags being defined
		if (AccessFlag.isNative(acc))
			counts[Classification.NATIVE.ordinal()] += 15;
		// Create a visitor that will collect method data
		return new MethodDataCollector();
	}

	/**
	 * Add a type reference with the default weight, {@code 1.0}.
	 *
	 * @param type
	 * 		Type visited.
	 */
	private void onTypeReference(Type type) {
		onTypeReference(type, 1);
	}

	/**
	 * Add a type reference with a given weight.
	 *
	 * @param type
	 * 		Type visited.
	 * @param weight
	 * 		Weight to add.
	 */
	private void onTypeReference(Type type, double weight) {
		// Skip non-objects
		if (type.getSort() < Type.OBJECT)
			return;
		String internalName = type.getInternalName();
		int category = categorize(internalName);
		if (category >= 0) {
			counts[category] += weight;
		} else if (!uncategorizedClasses.contains(internalName)) {
			uncategorizedClasses.add(internalName);
			Log.warn("Type reference '{}' has no categorization!", internalName);
		}
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Index in {@link #counts}/{@link #LABELS} the type correlates to.
	 */
	private int categorize(String internalName) {
		int lastPkgIdx = internalName.lastIndexOf('/');
		// Skip if no package
		if (lastPkgIdx == -1)
			return -1;
		// Lookup package to known type.
		String packageName = internalName.substring(0, lastPkgIdx);
		Classification classification = packageLookup.get(packageName);
		if (classification != null)
			return classification.ordinal();
		return -1;
	}

	/**
	 * @return Build the example from the label/label-counts.
	 */
	public Example<Label> build() {

		return new ArrayExample<>(null, LABELS, normalize(counts));
	}

	public static double[] normalize(double[] in) {
		int len = in.length;
		double total = Arrays.stream(in).sum();
		double[] normalized = new double[len];
		for (int i = 0; i < len; i++) {
			normalized[i] = in[i] / total;
		}
		return normalized;
	}

	private class MethodDataCollector extends MethodVisitor {
		public MethodDataCollector() {
			super(Recaf.ASM_VERSION);
		}

		@Override
		public void visitFieldInsn(int op, String owner, String name, String desc) {
			onTypeReference(Type.getObjectType(owner));
		}

		@Override
		public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
			onTypeReference(Type.getObjectType(owner));
		}

		@Override
		public void visitTypeInsn(int op, String type) {
			super.visitTypeInsn(op, type);
			// Handle array types (which are not given as the expected internal format)
			if (type.charAt(0) == '[') {
				Type arrType = Type.getType(type);
				while (arrType.getSort() == Type.ARRAY)
					arrType = arrType.getElementType();
				// If it's a reference type, set the type string and continue
				if (arrType.getSort() == Type.OBJECT)
					type = arrType.getInternalName();
				else
					return;
			}
			// Record the type (used in new XYX / instanceof XYZ)
			onTypeReference(Type.getObjectType(type));
		}
	}
}
