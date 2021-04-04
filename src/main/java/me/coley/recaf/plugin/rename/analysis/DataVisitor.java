package me.coley.recaf.plugin.rename.analysis;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ASM7;

/**
 * Reference extractor from a class.
 *
 * @author Matt Coley
 * @author Aleksi Ermolaev
 */
public class DataVisitor extends ClassVisitor {
	private final Workspace workspace;
	private String name;
	private final List<String> hierarchy = new ArrayList<>();
	private final List<FieldData> fields = new ArrayList<>();
	private final List<MethodData> methods = new ArrayList<>();
	private ClassData data;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public DataVisitor(Workspace workspace) {
		super(Recaf.ASM_VERSION);
		this.workspace = workspace;
	}

	@Override
	public void visit(int v, int acc, String name, String s, String parent, String[] interfaces) {
		this.name = name;
		// Record class hierarchy going upwards
		hierarchy.addAll(workspace.getHierarchyGraph().getAllParents(name)
				.collect(Collectors.toList()));
	}

	@Override
	public FieldVisitor visitField(int acc, String name, String desc, String s, Object v) {
		// Create a visitor that will collect field data
		return new DataFieldVisitor(desc);
	}

	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc, String s, String[] e) {
		// Create a visitor that will collect method data
		return new DataMethodVisitor(desc, acc);
	}

	@Override
	public void visitEnd() {
		// Finished the class parsing
		data = new ClassData(name, hierarchy, fields, methods);
	}

	/**
	 * @return Class information.
	 */
	public ClassData getData() {
		return data;
	}

	/**
	 * Field visitor that collects class references of the declared type and annotations.
	 */
	private class DataFieldVisitor extends FieldVisitor {
		private final String desc;
		private final List<String> annos = new ArrayList<>();

		private DataFieldVisitor(String desc) {
			super(Recaf.ASM_VERSION);
			this.desc = desc;
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			// Finished the field parsing.
			fields.add(new FieldData(Type.getType(desc), annos));
		}

		/// ============ DO NOT VISIT ANNOTATIONS =========== //

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean vis) {
			if (desc.startsWith("L") && desc.endsWith(";"))
				desc = desc.substring(1, desc.length() - 1);
			annos.add(desc);
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int ref, TypePath tp, String desc, boolean vis) {
			return null;
		}
	}

	/**
	 * Method visitor that collects class references of field/method calls.
	 */
	private class DataMethodVisitor extends MethodNode {
		private final String desc;
		private final int acc;
		private final List<String> references = new ArrayList<>();

		private DataMethodVisitor(String desc, int acc) {
			super(Recaf.ASM_VERSION);
			this.desc = desc;
			this.acc = acc;
		}

		@Override
		public void visitFieldInsn(int op, String owner, String name, String desc) {
			super.visitFieldInsn(op, owner, name, desc);
			// Record the declaring class of a field reference
			references.add(owner);
		}

		@Override
		public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
			super.visitMethodInsn(op, owner, name, desc, itf);
			// Record the declaring class of a method reference
			references.add(owner);
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
			references.add(type);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			// Finished the method parsing.
			Type retType = Type.getMethodType(desc).getReturnType();
			MethodData data = new MethodData(retType, references, instructions.size());
			if (AccessFlag.isNative(acc))
				data.markNative();
			methods.add(data);
		}

		/// ============ DO NOT VISIT ANNOTATIONS =========== //

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return null;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean vis) {
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int ref, TypePath tp, String desc, boolean vis) {
			return null;
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int param, String desc, boolean vis) {
			return null;
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int ref, TypePath tp, String desc, boolean vis) {
			return null;
		}
	}
}
