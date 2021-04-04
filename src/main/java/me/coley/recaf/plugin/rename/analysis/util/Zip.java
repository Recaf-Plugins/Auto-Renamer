package me.coley.recaf.plugin.rename.analysis.util;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility for compressing data.
 *
 * @author Matt Coley
 */
public class Zip {
	private static final int LEVEL = 9;

	/**
	 * @param data
	 * 		Data to compress.
	 *
	 * @return Compressed data.
	 *
	 * @throws IOException
	 * 		Failed to create zip data.
	 */
	public static byte[] compress(byte[] data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);
		zos.setLevel(LEVEL);
		zos.putNextEntry(new ZipEntry(""));
		zos.write(data);
		zos.closeEntry();
		zos.close();
		return baos.toByteArray();
	}

	/**
	 * @param compressed
	 * 		Compressed data.
	 *
	 * @return Uncompressed data.
	 *
	 * @throws IOException
	 * 		Failed to read zip data.
	 */
	public static byte[] decompress(byte[] compressed) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
		ZipInputStream zis = new ZipInputStream(bais);
		zis.getNextEntry();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(zis, baos);
		return baos.toByteArray();
	}
}
