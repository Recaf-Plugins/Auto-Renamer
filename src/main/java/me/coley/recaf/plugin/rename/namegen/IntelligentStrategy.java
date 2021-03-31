package me.coley.recaf.plugin.rename.namegen;

import me.coley.recaf.control.Controller;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.TypeUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A naming strategy that yields an intelligent pattern of renaming classes and their members.
 *
 * @author Matt Coley
 */
public class IntelligentStrategy extends AbstractNameStrategy {
	private final Map<String, String> fieldNameCache = new ConcurrentHashMap<>();

	protected IntelligentStrategy(Controller controller) {
		super(controller);
	}

	@Override
	public String className(ClassNode node) {
		// Do lookup check first since some calls may populate cached items for parent types
		if (hasClassMapping(node.name))
			return getCurrentName(node.name);
		// Check for parent name
		String baseName = getParentName(node);
		if (baseName == null) {
			// TODO: Analyze class structure and guess what its purpose is
		}
		// Ensure the parent name matches current mappings
		baseName = matchCurrentMappings(baseName);
		// Common prefix/suffix
		String prefix = "";
		String suffix = "";
		if (AccessFlag.isAnnotation(node.access))
			suffix = "Anno";
		else if (AccessFlag.isInterface(node.access))
			prefix = "I";
		else if (AccessFlag.isAbstract(node.access))
			prefix = "Abstract";
		else if (baseName != null && !baseName.contains("Impl"))
			suffix = "Impl";
		// Cleanup base name
		if (baseName != null) {
			if (baseName.endsWith(suffix)) {
				baseName = baseName.substring(0, baseName.length() - suffix.length());
			}
			if (!prefix.isEmpty() && baseName.startsWith(prefix)) {
				baseName = baseName.substring(prefix.length());
			}
			// Remove unused abstract
			if (!AccessFlag.isAbstract(node.access) && baseName.contains("Abstract")) {
				baseName = baseName.replace("Abstract", "");
			}
		}
		// Complete the name
		String middle = baseName == null ? "Obj" : baseName.substring(baseName.lastIndexOf('/') + 1);
		String mapped = prefix + middle + suffix;
		// Skip if output is redundant
		if (node.name.endsWith(mapped))
			return null;
		// Add mapping
		return addClassMapping(node.name, mapped);
	}

	@Override
	public String fieldName(ClassNode owner, FieldNode field) {
		Type type = Type.getType(field.desc);
		if (TypeUtil.isPrimitiveDesc(field.desc)) {
			String primType = NameUtils.capitalize(type.getClassName());
			return "f" + primType;
		}
		String internalName = matchCurrentMappings(type.getInternalName());
		String simple = internalName.substring(internalName.lastIndexOf('/') + 1);
		return "f" + NameUtils.capitalize(simple);
	}

	public Map<String, String> getFieldNameCache() {
		return fieldNameCache;
	}

	@Override
	public String methodName(ClassNode owner, MethodNode method) {
		// TODO: Analyze method name and create name based off of it
		//  - getter pattern
		//       L0
		//    		LINENUMBER X L0
		//    		ALOAD 0
		//    		GETFIELD owner.name : type
		//    		ARETURN
		//  - setter pattern
		//       L0
		//    		LINENUMBER X L0
		//    		ALOAD 0
		//          ALOAD 1
		//    		PUTFIELD owner.name : type
		//    		RETURN
		//  - anything else is named genericly
		return null;
	}

	@Override
	public String variable(MethodNode method, LocalVariableNode local) {
		if (TypeUtil.isPrimitiveDesc(local.desc)) {
			return local.desc.toLowerCase() + local.index;
		}
		Type type = Type.getType(local.desc);
		String internalName = matchCurrentMappings(type.getInternalName());
		String simple = internalName.substring(internalName.lastIndexOf('/') + 1);
		return NameUtils.camel(simple) + local.index;
	}

	private String matchCurrentMappings(String baseName) {
		if (baseName != null) {
			String currentMapping = null;
			if (hasClassMapping(baseName)) {
				// Map to existing mapping
				currentMapping = getCurrentName(baseName);
			} else if (getWorkspace().getPrimary().getClasses().containsKey(baseName)) {
				// No mapping, see what we would map it to if its in the primary workspace
				ClassNode baseClass = ClassUtil.getNode(getWorkspace().getClassReader(baseName), ClassReader.SKIP_CODE);
				currentMapping = className(baseClass);
			}
			// If a mapping was found, apply
			if (currentMapping != null) {
				baseName = currentMapping;
			}
		}
		return baseName;
	}
}
