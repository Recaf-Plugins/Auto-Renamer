package me.coley.recaf.plugin.rename;

import me.coley.recaf.control.Controller;

import java.util.Set;

public class Processor {
	private final Controller controller;
	private final AutoRename plugin;

	public Processor(Controller controller, AutoRename plugin) {
		this.controller = controller;
		this.plugin = plugin;
	}

	public void analyze(Set<String> matchedNames) {

	}

	public void apply() {

	}
}
