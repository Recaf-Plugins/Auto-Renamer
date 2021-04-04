package me.coley.recaf.plugin.rename.analysis.util;

import me.coley.recaf.util.Log;

import java.io.*;
import java.util.function.BiConsumer;

/**
 * File reading utilities.
 *
 * @author Matt Coley
 */
public class ReaderUtil {
	/**
	 * @param file
	 * 		File name in the classpath to read from.
	 * @param lineConsumer
	 * 		Action to execute per-line.
	 *
	 * @throws IOException
	 * 		Thrown when the file cannot be read.
	 */
	public static void readFile(String file, BiConsumer<String, Integer> lineConsumer) throws IOException {
		read(new StringReader(ClasspathUtils.getClasspathText(file)), lineConsumer);
	}

	/**
	 * @param text
	 * 		Text to read.
	 * @param lineConsumer
	 * 		Action to execute per-line.
	 */
	public static void readText(String text, BiConsumer<String, Integer> lineConsumer) {
		read(new StringReader(text), lineConsumer);
	}

	private static void read(StringReader reader, BiConsumer<String, Integer> lineConsumer) {
		// Line data
		int lineNo = 0;
		String line = null;
		// Read line-by-line and close automatically
		try(BufferedReader br = new BufferedReader(reader)) {
			while((line = br.readLine()) != null) {
				// Run action on line
				lineConsumer.accept(line, lineNo);
				// Increment line
				lineNo++;
			}
		} catch(IOException ex) {
			Log.error(ex, "Failed to read lines from text");
		} catch(NullPointerException | IndexOutOfBoundsException ex) {
			Log.error(ex, "Unexpected format in text, caused error!\nLine[{}]:{}", lineNo, line);
		}
	}
}
