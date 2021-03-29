package me.coley.recaf.plugin.rename.namefilter;

import me.coley.recaf.plugin.rename.AutoRename;
import me.coley.recaf.util.StringUtil;

/**
 * The scope defines what sorts of names will be targeted for renaming.
 *
 * @author Matt Coley
 */
public enum NamingScope {
	ALL,
	SHORT_NAMES,
	ILLEGAL_NAMES;

	@Override
	public String toString() {
		switch(this) {
			case ALL:
				return "All names";
			case SHORT_NAMES:
				return "Short names";
			case ILLEGAL_NAMES:
				return "Illegal names";
			default:
				return StringUtil.toString(this);
		}
	}

	/**
	 * @param plugin Plugin instance with config values to pull from
	 * @return A filter instance to match the current scope type.
	 */
	public ScopeFilter createFilter(AutoRename plugin) {
		switch(this) {
			case ALL:
				return name -> true;
			case SHORT_NAMES:
				return new ShortNameFilter(plugin.cutoffNameLen);
			case ILLEGAL_NAMES:
				return new IllegalNameFilter();
			default:
				throw new UnsupportedOperationException("Unsupported naming scope: " + name());
		}
	}
}
