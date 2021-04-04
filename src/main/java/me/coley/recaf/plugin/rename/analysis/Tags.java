package me.coley.recaf.plugin.rename.analysis;

import me.coley.recaf.plugin.rename.analysis.util.ClasspathUtils;
import me.coley.recaf.plugin.rename.analysis.util.ReaderUtil;
import me.coley.recaf.plugin.rename.analysis.util.Zip;
import me.coley.recaf.util.Log;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Manages tags.
 *
 * @author Matt Coley
 * @author Aleksi Ermolaev
 */
public class Tags {
	private static final int BASE_WEIGHT = 100;
	private static final List<String> tags = new ArrayList<>();
	private static final Map<String, String> refernceToTag = new HashMap<>();
	private static final Map<String, String> packageToTag = new HashMap<>();
	private static final Map<String, Integer> tagWeights = new HashMap<>();
	static {
		// TODO: This can probably be replaced by tf x idf if we change to the assignment-1 approach
		//
		// Some tagged items will be more prevalent than others...
		// So we need to penalize over-represented items.
		// This will allow less common items to be favored if up against a more common tag.
		tagWeights.put("FileIO",        BASE_WEIGHT - 50);
		tagWeights.put("Networking",    BASE_WEIGHT - 20);
		tagWeights.put("Database",      BASE_WEIGHT);
		tagWeights.put("BigData",       BASE_WEIGHT + 50);
		tagWeights.put("Distributed",   BASE_WEIGHT - 10);
		tagWeights.put("GUI",           BASE_WEIGHT - 40);
		tagWeights.put("CLI",           BASE_WEIGHT + 100);
		tagWeights.put("Security",      BASE_WEIGHT + 50);
		tagWeights.put("Android",       BASE_WEIGHT);
		tagWeights.put("Bytecode",      BASE_WEIGHT + 30);
		tagWeights.put("NativeInterop", BASE_WEIGHT + 500); // TODO: Is this really rare? Or am I doing something wrong?
		tagWeights.put("Suspicious",    BASE_WEIGHT + 330); // requires a MASSIVE boost due to rarity
		tagWeights.put("Misc",          BASE_WEIGHT - 99);
	}

	/**
	 * @param ref
	 * 		Class name.
	 * @param tag
	 * 		Tag for class.
	 */
	public static void putRef(String ref, String tag) {
		if (ref == null || tag == null)
			return;
		refernceToTag.put(ref, tag);
	}

	/**
	 * @param pack
	 * 		Package name.
	 * @param tag
	 * 		Tag for package.
	 */
	public static void putPackage(String pack, String tag) {
		if (pack == null || tag == null)
			return;
		packageToTag.put(pack, tag);
	}

	/**
	 * @return Number of reference-to-tags.
	 */
	public static int referenceSize() {
		return refernceToTag.size();
	}

	/**
	 * @param consumer
	 * 		Action to run on each reference-to-tag.
	 */
	public static void forEachRef(BiConsumer<String, String> consumer) {
		refernceToTag.forEach(consumer);
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Tag to apply. Defaults to {@code "Misc"}.
	 */
	public static String from(String name) {
		if (name == null)
			throw new IllegalArgumentException("Cant get tag from null");
		String tag = refernceToTag.get(name);
		if(tag == null)
			for(Map.Entry<String, String> e : packageToTag.entrySet()) {
				if(name.startsWith(e.getKey())) {
					tag = e.getValue();
					break;
				}
			}
		if(tag == null)
			tag = "Misc";
		return tag;
	}

	/**
	 * @param tag
	 * 		Name of tag.
	 *
	 * @return Tag exists.
	 */
	public static boolean has(String tag) {
		return tags.contains(tag);
	}

	/**
	 * @return Existing tags.
	 */
	public static List<String> getTags() {
		return tags;
	}

	/**
	 * @param tag
	 * 		Name of tag.
	 *
	 * @return Tag weight.
	 */
	public static int weight(String tag) {
		return tagWeights.getOrDefault(tag, BASE_WEIGHT);
	}

	/**
	 * Setup reading class-to-tag and package-to-tag mappings.
	 *
	 * @return {@code true} if successfully read all mappings.
	 */
	public static boolean setup() {
		try {
			// Read tags
			loadTags();
			// Read references
			loadMaven();
			loadPackages();
			// Success
			return true;
		} catch(IllegalStateException ex) {
			// Log the cause, which we control
			Log.error(ex.getCause(), "Failed to setup tag pool, cause: {}", ex.getMessage());
			// Failure
			return false;
		}
	}

	private static void loadTags() {
		try {
			String txt = ClasspathUtils.getClasspathText("tags.txt");
			String[] lines = txt.split("[\n\r]");
			for(String line : lines) {
				// skip comment lines
				if(line.startsWith("#"))
					continue;
				// get first word
				String[] parts = line.split("\\s+");
				if(parts.length > 0) {
					String key = parts[0];
					if(!key.isEmpty())
						tags.add(key);
				}
			}
			// Sort
			Collections.sort(tags);
		} catch(IOException ex) {
			throw new IllegalStateException("Failed reading tags", ex);
		}
	}

	private static void loadMaven() {
		try {
			String csv = new String(Zip.decompress(ClasspathUtils.getClasspathFile("mvnref-to-tag.compressed")));
			ReaderUtil.readText(csv, (line, lineNo) -> {
				String[] split = line.split(",");
				String name = split[0];
				String tag = split[1];
				putRef(name, tag);
			});
		} catch(Exception ex) {
			throw new IllegalStateException("Failed parsing maven references", ex);
		}
	}

	private static void loadPackages() {
		// Tag entire packages in from the JDK/non-single-use-libraries
		// FileIO
		putPackage("com/sun/nio/", "FileIO");
		putPackage("java/io/", "FileIO");
		putPackage("java/nio/", "FileIO");
		putPackage("java/util/jar/", "FileIO");
		putPackage("java/util/zip/", "FileIO");
		putPackage("javax/imageio/", "FileIO");
		putPackage("javax/smartcardio/", "FileIO");
		putPackage("com/google/common/io/", "FileIO");
		// Networking
		putPackage("com/sun/net/", "Networking");
		putPackage("java/net/", "Networking");
		putPackage("java/rmi/", "Networking");
		putPackage("javax/net/", "Networking");
		putPackage("javax/print/", "Networking");
		putPackage("javax/rmi/", "Networking");
		putPackage("jdk/net/", "Networking");
		putPackage("sun/rmi/", "Networking");
		putPackage("com/google/common/net/", "Networking");
		// Database
		putPackage("java/sql/", "Database");
		putPackage("javax/sql/", "Database");
		// BigData
		//  - Handled by maven
		// Distributed
		putPackage("java/util/concurrent/", "Distributed");
		putPackage("com/google/common/util/concurrent/", "Distributed");
		// GUI
		putPackage("com/sun/glass/", "GUI");
		putPackage("com/sun/javafx/", "GUI");
		putPackage("com/sun/media/", "GUI");
		putPackage("com/sun/prism/", "GUI");
		putPackage("com/sun/awt/", "GUI");
		putPackage("com/sun/image/", "GUI");
		putPackage("com/sun/java/swing/", "GUI");
		putPackage("com/sun/swing/", "GUI");
		putPackage("sun/applet/", "GUI");
		putPackage("java/applet/", "GUI");
		putPackage("java/awt/", "GUI");
		putPackage("javax/accessibility/", "GUI");
		putPackage("javax/sound/", "GUI");
		putPackage("javax/swing/", "GUI");
		putPackage("javafx/", "GUI");
		// CLI
		//  - Handled by maven
		// Security
		putPackage("javax/crypto/", "Security");
		putPackage("sun/security/", "Security");
		putPackage("com/sun/security/", "Security");
		putPackage("java/security/", "Security");
		putPackage("javax/security/", "Security");
		// Android
		putPackage("com/google/android/", "Android");
		putPackage("android/", "Android");
		putPackage("androidx/", "Android");
		putPackage("dalvik/", "Android");
		// NativeInterop
		//  - <native> access modifier
		putPackage("com/sun/jna/", "NativeInterop");
		putPackage("org/jnativehook/", "NativeInterop");
		// Bytecode
		putPackage("me/itzsomebody/radon/asm/", "Bytecode");
		putPackage("net/contra/jmd/", "Bytecode");
	}
}