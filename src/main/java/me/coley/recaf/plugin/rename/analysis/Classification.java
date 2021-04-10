package me.coley.recaf.plugin.rename.analysis;

import me.coley.recaf.plugin.rename.namegen.NameUtils;

public enum Classification {
	UI,
	IO,
	NETWORKING,
	SECURITY,
	BYTECODE,
	NATIVE,
	DATABASE,
	DISTRIBUTED,
	MISC;

	public String columnName() {
		switch (this) {
			case UI:
				return "nUI";
			case IO:
				return "nIO";
			case NETWORKING:
				return "nNet";
			case SECURITY:
				return "nSec";
			case BYTECODE:
				return "nByte";
			case NATIVE:
				return "nNative";
			case DATABASE:
				return "nDb";
			case DISTRIBUTED:
				return "nDist";
			default:
				// Unused
				return "nMisc";
		}
	}

	@Override
	public String toString() {
		if (this == MISC)
			return "Obj";
		else if (this == UI || this == IO)
			return name();
		return NameUtils.capitalize(name().toLowerCase());
	}
}
