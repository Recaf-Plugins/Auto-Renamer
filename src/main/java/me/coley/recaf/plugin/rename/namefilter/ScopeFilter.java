package me.coley.recaf.plugin.rename.namefilter;

/**
 * Outline for matching names.
 *
 * @author Matt Coley
 */
public interface ScopeFilter {
	/**
	 * @param name Name to check.
	 * @return {@code true} when the name fits the filter.
	 */
	boolean matches(String name);
}
