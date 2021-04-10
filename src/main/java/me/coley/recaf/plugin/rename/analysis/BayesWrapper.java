package me.coley.recaf.plugin.rename.analysis;

import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.Log;
import org.objectweb.asm.tree.ClassNode;
import org.tribuo.Dataset;
import org.tribuo.Example;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.mnb.MultinomialNaiveBayesModel;
import org.tribuo.classification.mnb.MultinomialNaiveBayesTrainer;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.impl.ArrayExample;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Wrapper around Tribuo's Naive Bayes model.
 *
 * @author Matt Coley
 */
public class BayesWrapper {
	private static final MultinomialNaiveBayesTrainer bayesTrainer = new MultinomialNaiveBayesTrainer(0.75);
	private static MultinomialNaiveBayesModel bayesModel;
	private static Map<String, Classification> packageLookup;

	/**
	 * Quick and dirty testing. Requires the {@code pom.xml} be modified
	 * to have {@code Recaf} set to {@code compile} instead of {@code provided}
	 *
	 * @param args
	 * 		Unused.
	 */
	public static void main(String[] args) {
		try {
			init();
			// Inputs
			String[] labels = ClassDataCollector.LABELS;
			double[] counts = new double[labels.length];
			counts[Classification.BYTECODE.ordinal()] = 16;
			counts[Classification.IO.ordinal()] = 30;
			// Normalize input and classify
			double[] normalized = ClassDataCollector.normalize(counts);
			Prediction<Label> prediction = getPrediction(new ArrayExample<>(null, labels, normalized));
			System.out.println(prettyPrint(prediction));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @param prediction
	 * 		Item to print.
	 *
	 * @return Multi-line string with predictions sorted in asccending order.
	 */
	private static String prettyPrint(Prediction<Label> prediction) {
		SortedSet<Label> sorted = new TreeSet<>(Comparator.comparingDouble(Label::getScore));
		sorted.addAll(prediction.getOutputScores().values());
		StringBuilder sb = new StringBuilder();
		for (Label label : sorted) {
			String labelName = label.getLabel();
			String percent = NumberFormat.getPercentInstance().format(label.getScore());
			sb.append(labelName).append(" - ").append(percent).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Initialize the trainer and create a naive bayes model from the training file.
	 *
	 * @throws Exception
	 * 		When the training failed, probably due to an issue reading the training file off the classpath.
	 */
	public static void init() throws Exception {
		if (bayesModel == null) {
			bayesModel = (MultinomialNaiveBayesModel) bayesTrainer.train(loadDataSet());
			packageLookup = loadPackageLookup();
		}
	}

	/**
	 * @param node
	 * 		Class to transform into a model input.
	 *
	 * @return Model input example instance.
	 */
	public static Example<Label> createClassDataExample(ClassNode node) {
		ClassDataCollector visitor = new ClassDataCollector(packageLookup);
		node.accept(visitor);
		return visitor.build();
	}

	/**
	 * @param example
	 * 		See {@link #createClassDataExample(ClassNode)}.
	 *
	 * @return Prediction of the class's use case, wrapped in {@link Prediction}.
	 */
	public static Prediction<Label> getPrediction(Example<Label> example) {
		return bayesModel.predict(example);
	}


	/**
	 * @param classificationThreshold
	 * 		The threshold that must be met for a classification to be made.
	 * 		If not met, we re-label it as {@link Classification#MISC}.
	 * @param example
	 * 		See {@link #createClassDataExample(ClassNode)}.
	 *
	 * @return Prediction of the class's use case.
	 */
	public static Classification getPredictedClassification(double classificationThreshold, Example<Label> example) {
		Label label = getPrediction(example).getOutput();
		if (label.getScore() <= classificationThreshold) {
			return Classification.MISC;
		}
		String labelName = label.getLabel();
		try {
			return Classification.valueOf(labelName.toUpperCase());
		} catch (Exception ex) {
			ex.printStackTrace();
			return Classification.MISC;
		}
	}

	/**
	 * @return Dataset from {@code training.csv} in the classpath.
	 *
	 * @throws Exception
	 * 		When classpath IO decides to ruin your day.
	 */
	private static Dataset<Label> loadDataSet() throws Exception {
		LabelFactory labelFactory = new LabelFactory();
		CSVLoader<Label> loader = new CSVLoader<>(',', labelFactory);
		return new MutableDataset<>(loader.loadDataSource(
				copyToTempFile(BayesWrapper.class.getResource("/training.csv"), "csv"), "type"));
	}

	/**
	 * @return Map of package names to what their classes are responsible for in classification.
	 *
	 * @throws Exception
	 * 		When classpath IO decides to ruin your day.
	 */
	private static Map<String, Classification> loadPackageLookup() throws Exception {
		Map<String, Classification> map = new HashMap<>();
		addJavaPackages(map);
		addMiscPackages(map);
		return map;
	}

	/**
	 * Add packages from {@code packages.csv} from the classpath into the given map.
	 *
	 * @param map
	 * 		Map to populate.
	 *
	 * @throws Exception
	 * 		When classpath IO decides to ruin your day.
	 */
	private static void addMiscPackages(Map<String, Classification> map) throws Exception {
		Path path = copyToTempFile(BayesWrapper.class.getResource("/packages.csv"), "csv");
		List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		for (String line : lines) {
			int comma = line.indexOf(',');
			String packageName = line.substring(0, comma);
			String className = line.substring(comma + 1).toUpperCase();
			try {
				Classification classification = Classification.valueOf(className);
				map.put(packageName, classification);
			} catch (Exception ex) {
				Log.warn("Skipping unknown classified package: {} -> {}", packageName, className);
			}
		}
	}

	/**
	 * Supply default java packages to the map.
	 *
	 * @param map
	 * 		Map to populate.
	 */
	private static void addJavaPackages(Map<String, Classification> map) {
		map.put("java/applet", Classification.UI);
		map.put("java/awt", Classification.UI);
		map.put("java/awt/color", Classification.UI);
		map.put("java/awt/datatransfer", Classification.UI);
		map.put("java/awt/dnd", Classification.UI);
		map.put("java/awt/event", Classification.UI);
		map.put("java/awt/font", Classification.UI);
		map.put("java/awt/geom", Classification.UI);
		map.put("java/awt/im", Classification.UI);
		map.put("java/awt/im/spi", Classification.UI);
		map.put("java/awt/image", Classification.UI);
		map.put("java/awt/image/renderable", Classification.UI);
		map.put("java/awt/print", Classification.UI);
		map.put("java/io", Classification.IO);
		map.put("java/lang/instrument", Classification.BYTECODE);
		map.put("java/net", Classification.NETWORKING);
		map.put("java/nio", Classification.IO);
		map.put("java/nio/channels", Classification.IO);
		map.put("java/nio/channels/spi", Classification.IO);
		map.put("java/nio/charset", Classification.IO);
		map.put("java/nio/charset/spi", Classification.IO);
		map.put("java/nio/file", Classification.IO);
		map.put("java/nio/file/attribute", Classification.IO);
		map.put("java/nio/file/spi", Classification.IO);
		map.put("java/rmi", Classification.NETWORKING);
		map.put("java/rmi/activation", Classification.NETWORKING);
		map.put("java/rmi/dgc", Classification.NETWORKING);
		map.put("java/rmi/registry", Classification.NETWORKING);
		map.put("java/rmi/server", Classification.NETWORKING);
		map.put("java/security", Classification.SECURITY);
		map.put("java/security/acl", Classification.SECURITY);
		map.put("java/security/cert", Classification.SECURITY);
		map.put("java/security/interfaces", Classification.SECURITY);
		map.put("java/security/spec", Classification.SECURITY);
		map.put("java/sql", Classification.IO);
		map.put("java/util/jar", Classification.IO);
		map.put("java/util/logging", Classification.IO);
		map.put("java/util/prefs", Classification.IO);
		map.put("java/util/stream", Classification.IO);
		map.put("java/util/zip", Classification.IO);
		map.put("javax/crypto", Classification.SECURITY);
		map.put("javax/crypto/interfaces", Classification.SECURITY);
		map.put("javax/crypto/spec", Classification.SECURITY);
		map.put("javax/imageio", Classification.UI);
		map.put("javax/imageio/event", Classification.UI);
		map.put("javax/imageio/metadata", Classification.UI);
		map.put("javax/imageio/plugins/bmp", Classification.UI);
		map.put("javax/imageio/plugins/jpeg", Classification.UI);
		map.put("javax/imageio/spi", Classification.UI);
		map.put("javax/imageio/stream", Classification.UI);
		map.put("javax/management/remote", Classification.NETWORKING);
		map.put("javax/management/remote/rmi", Classification.NETWORKING);
		map.put("javax/net", Classification.NETWORKING);
		map.put("javax/net/ssl", Classification.SECURITY);
		map.put("javax/rmi", Classification.NETWORKING);
		map.put("javax/rmi/CORBA", Classification.NETWORKING);
		map.put("javax/rmi/ssl", Classification.SECURITY);
		map.put("javax/security/auth", Classification.SECURITY);
		map.put("javax/security/auth/callback", Classification.SECURITY);
		map.put("javax/security/auth/kerberos", Classification.SECURITY);
		map.put("javax/security/auth/login", Classification.SECURITY);
		map.put("javax/security/auth/spi", Classification.SECURITY);
		map.put("javax/security/auth/x500", Classification.SECURITY);
		map.put("javax/security/cert", Classification.SECURITY);
		map.put("javax/security/sasl", Classification.SECURITY);
		map.put("javax/sound/midi", Classification.UI);
		map.put("javax/sound/midi/spi", Classification.UI);
		map.put("javax/sound/sampled", Classification.UI);
		map.put("javax/sound/sampled/spi", Classification.UI);
		map.put("javax/sql", Classification.IO);
		map.put("javax/sql/rowset", Classification.IO);
		map.put("javax/sql/rowset/serial", Classification.IO);
		map.put("javax/sql/rowset/spi", Classification.IO);
		map.put("javax/swing", Classification.UI);
		map.put("javax/swing/border", Classification.UI);
		map.put("javax/swing/colorchooser", Classification.UI);
		map.put("javax/swing/event", Classification.UI);
		map.put("javax/swing/filechooser", Classification.UI);
		map.put("javax/swing/plaf", Classification.UI);
		map.put("javax/swing/plaf/basic", Classification.UI);
		map.put("javax/swing/plaf/metal", Classification.UI);
		map.put("javax/swing/plaf/multi", Classification.UI);
		map.put("javax/swing/plaf/nimbus", Classification.UI);
		map.put("javax/swing/plaf/synth", Classification.UI);
		map.put("javax/swing/table", Classification.UI);
		map.put("javax/swing/text", Classification.UI);
		map.put("javax/swing/text/html", Classification.UI);
		map.put("javax/swing/text/html/parser", Classification.UI);
		map.put("javax/swing/text/rtf", Classification.UI);
		map.put("javax/swing/tree", Classification.UI);
		map.put("javax/swing/undo", Classification.UI);
		map.put("javax/xml/crypto", Classification.SECURITY);
		map.put("javax/xml/crypto/dom", Classification.SECURITY);
		map.put("javax/xml/crypto/dsig", Classification.SECURITY);
		map.put("javax/xml/crypto/dsig/dom", Classification.SECURITY);
		map.put("javax/xml/crypto/dsig/keyinfo", Classification.SECURITY);
		map.put("javax/xml/crypto/dsig/spec", Classification.SECURITY);
	}

	private static Path copyToTempFile(URL url, String suffix) throws IOException {
		// TODO: This method is a temporary hack until I can figure out why the plugin jar
		//      cannot read its own resources...
		Path tempFile = Files.createTempFile(null, suffix);
		try (OutputStream out = Files.newOutputStream(tempFile)) {
			IOUtil.transfer(url, out);
		}
		tempFile.toFile().deleteOnExit();
		return tempFile;
	}
}
