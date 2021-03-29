package me.coley.recaf.plugin.rename.namefilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Name filter where items that are named illegally <i>(reserved keywords, whitespace, etc)</i> fit the filter.
 *
 * @author Matt Coley
 */
public class IllegalNameFilter extends AbstractScopeFilter {
	private static final Set<String> KEYWORDS = new HashSet<>(
			Arrays.asList("_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
					"const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
					"finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
					"long", "native", "new", "null", "package", "private", "protected", "public",
					"record", "return", "short", "static", "static final", "strictfp", "super", "switch",
					"synchronized", "this", "throw", "throws", "transient", "true", "try", "undefined", "var", "void",
					"volatile", "while"));
	/**
	 * See: https://www.compart.com/en/unicode/category
	 */
	private static final int[] ALLOWED_TYPES = new int[] {
			Character.UPPERCASE_LETTER,
			Character.LOWERCASE_LETTER,
			Character.OTHER_LETTER,
			Character.DECIMAL_DIGIT_NUMBER,
			Character.CURRENCY_SYMBOL,
	};

	@Override
	public boolean computeMatch(String name) {
		String[] parts = name.split("/");
		// Match if any package or class name is a reserved keyword.
		for (String part : parts) {
			if (KEYWORDS.contains(part))
				return true;
		}
		// Check if any character does not belong to an allowed category
		for (char ch : name.toCharArray()) {
			if (ch == '/' || ch == '$' || ch == '_' || Arrays.binarySearch(ALLOWED_TYPES, Character.getType(ch)) >= 0) {
				continue;
			}
			return true;
		}
		// Default, valid name
		return false;
	}
}