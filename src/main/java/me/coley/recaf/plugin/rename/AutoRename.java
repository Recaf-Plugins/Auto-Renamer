package me.coley.recaf.plugin.rename;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import me.coley.recaf.config.Conf;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.rename.namefilter.NamingScope;
import me.coley.recaf.plugin.rename.namegen.NamingPattern;
import me.coley.recaf.ui.ContextBuilder;
import me.coley.recaf.ui.controls.ActionMenuItem;
import me.coley.recaf.ui.controls.NumberSlider;
import me.coley.recaf.workspace.JavaResource;
import org.plugface.core.annotations.Plugin;
import me.coley.recaf.plugin.api.*;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A plugin that adds context menus to decompile a class, a package, or the entire program all at once.
 * The results are bundled as a ZIP file and placed at a requested location.
 *
 * @author Matt Coley
 */
@Plugin(name = "Auto Renamer")
public class AutoRename implements StartupPlugin, ContextMenuInjectorPlugin, ConfigurablePlugin {
	public static final String FLAT_PACKAGE_NAME = "renamed/";
	private static final String KEEP_P_STRUCT = "Keep package layout";
	private static final String NAME_PATTERN = "Naming pattern";
	private static final String NAME_SCOPE = "Naming scope";
	private static final String SHORT_CUTOFF = "Short name cutoff";
	private static final String PRUNE_DEBUG = "Remove debug info";
	private Controller controller;

	@Conf(value = NAME_PATTERN, noTranslate = true)
	public NamingPattern namingPattern = NamingPattern.INTELLIGENT;

	@Conf(value = NAME_SCOPE, noTranslate = true)
	public NamingScope namingScope = NamingScope.ALL;

	@Conf(value = SHORT_CUTOFF, noTranslate = true)
	public int cutoffNameLen;

	@Conf(value = KEEP_P_STRUCT, noTranslate = true)
	public boolean keepPackageLayout = true;

	@Conf(value = PRUNE_DEBUG, noTranslate = true)
	public boolean pruneDebugInfo;

	@Override
	public String getVersion() {
		return "1.0.0";
	}

	@Override
	public String getDescription() {
		return "Allows classes and packages to be renamed automatically";
	}

	@Override
	public void onStart(Controller controller) {
		this.controller = controller;
	}

	@Override
	public void addFieldEditors(Map<String, Function<FieldWrapper, Node>> editors) {
		editors.put(SHORT_CUTOFF, field -> new NumberSlider<Integer>((GuiController) controller, field, 1, 30, 1));
	}

	@Override
	public void forPackage(ContextBuilder builder, ContextMenu menu, String name) {
		menu.getItems().add(new ActionMenuItem("Auto rename classes",
				() -> rename(Pattern.quote(name) + "/.*", builder.getResource())));
	}

	@Override
	public void forClass(ContextBuilder builder, ContextMenu menu, String name) {
		menu.getItems().add(new ActionMenuItem("Auto rename class",
				() -> rename(Pattern.quote(name), builder.getResource())));
	}

	@Override
	public void forResourceRoot(ContextBuilder builder, ContextMenu menu, JavaResource resource) {
		menu.getItems().add(new ActionMenuItem("Auto rename all",
				() -> rename(".*", resource)));
	}

	private void rename(String namePattern, JavaResource resource) {
		Set<String> matchedNames = resource.getClasses().keySet().stream()
				.filter(name -> name.matches(namePattern))
				.collect(Collectors.toSet());
		Processor processor = new Processor(controller, this);
		processor.analyze(matchedNames);
		processor.apply();
	}

	@Override
	public String getConfigTabTitle() {
		return "Auto Renamer";
	}
}