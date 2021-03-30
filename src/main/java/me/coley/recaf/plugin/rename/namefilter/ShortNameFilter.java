package me.coley.recaf.plugin.rename.namefilter;

/**
 * Name filter where items at or below a certain length match.
 *
 * @author Matt Coley
 */
public class ShortNameFilter implements ScopeFilter {
	private final int length;

	/**
	 * @param length
	 * 		Maximum length for what to consider as a short name.
	 */
	public ShortNameFilter(int length) {
		this.length = length;
	}

	@Override
	public boolean matches(String name) {
		return name.length() <= length;
	}
}
