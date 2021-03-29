package me.coley.recaf.plugin.rename.namefilter;

public class ShortNameFilter implements ScopeFilter {
	private final int length;

	public ShortNameFilter(int length) {
		this.length = length;
	}

	@Override
	public boolean matches(String name) {
		return name.length() <= length;
	}
}
