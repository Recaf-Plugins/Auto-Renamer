package me.coley.recaf.plugin.rename.namefilter;

import java.util.HashSet;
import java.util.Set;

/**
 * Common base for filters with result caching to prevent unnecessary duplicate checks.
 *
 * @author Matt Coley
 */
public abstract class AbstractScopeFilter implements ScopeFilter {
	private final Set<String> visitedMatches = new HashSet<>();
	private final Set<String> visitedNonMatches = new HashSet<>();

	@Override
	public boolean matches(String name) {
		// Check for existing result
		if (visitedMatches.contains(name))
			return true;
		else if (visitedNonMatches.contains(name))
			return false;
		// Compute and store result
		boolean result = computeMatch(name);
		if (result)
			visitedMatches.add(name);
		else
			visitedNonMatches.add(name);
		return result;
	}

	protected abstract boolean computeMatch(String name);
}
