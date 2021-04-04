package me.coley.recaf.plugin.rename.analysis.util;

import org.apache.commons.io.IOUtils;

import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility for retrieving files from the classpath.
 *
 * @author Matt Coley
 */
public class ClasspathUtils {
	/**
	 * @param file
	 * 		Relative file name in the classpath.
	 *
	 * @return Text file contents.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be found on the classpath.
	 */
	public static String getClasspathText(String file) throws IOException{
		return new String(getClasspathFile(file), UTF_8);
	}

	/**
	 * @param file
	 * 		Relative file name in the classpath.
	 *
	 * @return Raw file contents.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be found on the classpath.
	 */
	public static byte[] getClasspathFile(String file) throws IOException {
		try (InputStream in = ClasspathUtils.class.getResourceAsStream("/" + file)) {
			return IOUtils.toByteArray(in);
		}
	}
}
