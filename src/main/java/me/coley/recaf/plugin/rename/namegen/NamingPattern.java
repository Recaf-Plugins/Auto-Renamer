package me.coley.recaf.plugin.rename.namegen;

import me.coley.recaf.util.StringUtil;

/**
 * Types of naming patterns.
 *
 * @author Matt Coley
 */
public enum NamingPattern {
	INTELLIGENT,
	SOURCE_FILE,
	SIMPLE;

	@Override
	public String toString() {
		switch (this) {
			case INTELLIGENT:
				return "Intelligent";
			case SOURCE_FILE:
				return "Match source file";
			case SIMPLE:
				return "Simple";
			default:
				return StringUtil.toString(this);
		}
	}

	public NameStrategy createStrategy() {
		switch (this) {
			case INTELLIGENT:
				return new IntelligentStrategy();
			case SOURCE_FILE:
				return new SourceFileStrategy();
			case SIMPLE:
				return new SimpleStrategy();
			default:
				throw new UnsupportedOperationException("Unsupported naming pattern: " + name());
		}
	}
}
