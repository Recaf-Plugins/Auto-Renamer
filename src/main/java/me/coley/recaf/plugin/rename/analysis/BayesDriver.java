package me.coley.recaf.plugin.rename.analysis;

import me.coley.recaf.plugin.rename.analysis.util.ClasspathUtils;
import me.coley.recaf.plugin.rename.analysis.util.ReaderUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BayesDriver {
	private final Bayes<String, String> bayes = new Bayes<>();
	private final Map<String, String> classToTag = new HashMap<>();

	private boolean setup;

	public Map<String, String> getClassToTag() {
		return classToTag;
	}

	public void populate(Workspace workspace) {
		Map<String, ClassData> classToData = new HashMap<>();
		workspace.getPrimaryClassReaders().forEach(reader -> {
			try {
				DataVisitor dv = new DataVisitor(workspace);
				reader.accept(dv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
				classToData.put(reader.getClassName(), dv.getData());
			} catch (Exception ex) {
				Log.error(ex, "BayesDriver, parsing '{}' failed", reader.getClassName());
			}
		});
		classToData.values().forEach(cd -> {
			List<String> references = new ArrayList<>();
			// Collect tag frequency
			cd.getHierarchy().forEach(name -> {
				String tag = Tags.from(name);
				references.add(tag);
			});
			cd.getFields().forEach(fd -> {
				if (fd.getType().getSort() == Type.OBJECT) {
					String tag = Tags.from(fd.getType().getInternalName());
					references.add(tag);
				}
				for (String anno : fd.getAnnotationReferences()) {
					String tag = Tags.from(anno);
					references.add(tag);
				}
			});
			cd.getMethods().forEach(md -> {
				if (md.isNative()) {
					references.add("NativeInterop");
					return;
				}
				if (md.getReturnType().getSort() == Type.OBJECT) {
					String tag = Tags.from(md.getReturnType().getInternalName());
					references.add(tag);
				}
				md.getReferences().forEach(mref -> {
					String tag = Tags.from(mref);
					references.add(tag);
				});
			});
			while (references.contains("Misc"))
				references.remove("Misc");
			if (references.isEmpty())
				return;
			String tagg = bayes.classify(references.toArray(new String[0]));
			classToTag.put(cd.getName(), tagg);
		});
	}

	public void setup() throws IOException {
		if (!setup) {
			setup = true;
			bayes.setFallback("Misc");
			bayes.setThreshold(-1);
			//
			Tags.setup();
			// bayes.setThreshold(0.01f);
			String training = ClasspathUtils.getClasspathText("training-config.csv");
			ReaderUtil.readText(training, (line, lineNo) -> {
				if (line.startsWith("#"))
					return;
				String[] split = line.split(",");
				int weight = Integer.parseInt(split[0]);
				String category = split[1];
				String[] attributes = split[2].split(":");
				bayes.train(category, attributes, weight);
			});
		}
	}
}
