package saf.core.ui;

import org.java.plugin.Plugin;
import org.java.plugin.PluginClassLoader;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import saf.core.runtime.PluginDefinitionException;
import saf.core.ui.actions.ISAFAction;
import saf.core.ui.help.Help;
import saf.core.ui.dock.Perspective;
import saf.core.ui.dock.Location;
import saf.core.ui.dock.DockingFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * @author Nick Collier
 * @version $Revision: 1.9 $ $Date: 2006/06/01 16:29:06 $
 */
public class UIPlugin extends Plugin {

	private static final String ACTIONS_ID = "ui.Actions";
	private static final String MENUS_ID = "ui.Menus";
	private static final String PERSPECTIVES_ID = "ui.Perspectives";
	private static final String HELP_ID = "ui.Help";
	private static final String STATUS_BAR_ID = "ui.StatusBar";

	private static final String ACTION_ID = "actionID";
	private static final String LABEL = "label";
	private static final String MENU_PATH = "menuID";
	private static final String TOOL_PATH = "groupID";
	private static final String TOOLTIP = "tooltip";
	private static final String CLASS_NAME = "class";
	private static final String ICON_PATH = "icon";
	private static final String COMMAND = "command";

	private static final String MENU_ID = "menuID";
	private static final String MENU_LABEL_ID = "label";
	private static final String MENU_PARENT_ID = "parentID";

	private static final String VIEW_GROUP_ID = "groupID";
	private static final String VIEW_GROUP_LOCATION = "location";
	private static final String VIEW_GROUP_PARENT = "parent";
	private static final String VIEW_GROUP_FILL = "fillPercentage";

	private static final String PERSPECTIVE_ID = "perspectiveID";
	private static final String PERSPECTIVE_NAME_ID = "name";
	private static final String VIEW_GROUPS_ID = "viewGroup";


	private Map<String, Location> locationMap = new HashMap<String, Location>();
	private boolean viewRootFound = false;
  private DockingFactory dockingFactory;


  protected void doStart() throws Exception {

	}

	protected void doStop() throws Exception {

	}

	public void initialize() throws PluginDefinitionException {
    dockingFactory = GUICreatorDelegate.getInstance().getDockingFactory();
    fillLocationMap();

		// process action specs
		ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getDescriptor().getId(), ACTIONS_ID);
		ExtPointProcessor processor = new ActionProcessor();
		processor.iterate(this, extPoint);

		//  process menu spec
		MenuTreeDescriptor mtDescriptor = new MenuTreeDescriptor();
		extPoint = getManager().getRegistry().getExtensionPoint(getDescriptor().getId(), MENUS_ID);
		processor = new MenuProcessor(mtDescriptor);
		processor.iterate(this, extPoint);
		GUICreatorDelegate.getInstance().setMenuTreeDescriptor(mtDescriptor);

		// process status field specs
		extPoint = getManager().getRegistry().getExtensionPoint(getDescriptor().getId(), STATUS_BAR_ID);
		StatusFieldProcessor stProcessor = new StatusFieldProcessor();
		stProcessor.iterate(this, extPoint);
		GUICreatorDelegate.getInstance().setStatusBarDescriptor(stProcessor.getDescriptor());

		// process perspectives
		List<Perspective> perspectives = new ArrayList<Perspective>();
		extPoint = getManager().getRegistry().getExtensionPoint(getDescriptor().getId(), PERSPECTIVES_ID);
		processor = new PerspectiveProcessor(perspectives);
		processor.iterate(this, extPoint);
		if (perspectives.size() == 0) {
			throw new PluginDefinitionException("At least one perspective must be defined");
		}

		GUICreatorDelegate.getInstance().setPerspectives(perspectives);
		Extension ext = (Extension) extPoint.getConnectedExtensions().iterator().next();
		AppPreferences prefs = new AppPreferences(ext.getDeclaringPluginDescriptor().getId(), perspectives);
		GUICreatorDelegate.getInstance().setApplicationPrefs(prefs);

		// process help sets
		extPoint = getManager().getRegistry().getExtensionPoint(getDescriptor().getId(), HELP_ID);
		processor = new HelpSetProcessor();
		processor.iterate(this, extPoint);
		GUICreatorDelegate.getInstance().setHelp(((HelpSetProcessor) processor).getHelp());
	}

	private void fillLocationMap() {
		locationMap.put("north", Location.NORTH);
		locationMap.put("south", Location.SOUTH);
		locationMap.put("east", Location.EAST);
		locationMap.put("west", Location.WEST);
	}

	private boolean subParamExists(String name, Extension.Parameter param) {
		return param.getSubParameter(name) != null;
	}

	private String getSubParamValueAsString(String name, Extension.Parameter param) {
		if (subParamExists(name, param)) return param.getSubParameter(name).valueAsString();
		return null;
	}

	private float getSubParamValueAsFloat(String name, Extension.Parameter param) {
		if (subParamExists(name, param)) return param.getSubParameter(name).valueAsNumber().floatValue();
		return Float.NaN;
	}


	Help processHelpSet(Extension.Parameter param, Help help) throws PluginDefinitionException {
		String hsFile = param.valueAsString();
		PluginClassLoader pluginClassLoader = getManager().getPluginClassLoader(param.getDeclaringPluginDescriptor());
		URL helpSet = pluginClassLoader.getResource(hsFile);
		try {
			if (helpSet == null) {
				File file = new File(hsFile);
				if (!file.exists()) throw new PluginDefinitionException("Help Set file '" + hsFile + "' does not exist");
				helpSet = file.toURL();
			}
			if (help == null)
				help = new Help(pluginClassLoader, helpSet);
			else
				help.addHelpSet(pluginClassLoader, helpSet);
			return help;
		} catch (IOException ex) {
			throw new PluginDefinitionException(ex);
		}
	}

	void processStatusFieldSpec(Extension.Parameter param, StatusBarDescriptor statusDescriptor)
					throws PluginDefinitionException {
		if (param.getId().equals("StatusField")) {
			String name = getSubParamValueAsString("name", param);
			float fillPercentage = getSubParamValueAsFloat("fillPercentage", param);
			try {
				statusDescriptor.addField(name, fillPercentage);
			} catch (IllegalArgumentException ex) {
				throw new PluginDefinitionException(ex);
			}
		}
	}

	void processPerspective(Extension.Parameter param, List<Perspective> perspectives) throws PluginDefinitionException {
		String id = getSubParamValueAsString(PERSPECTIVE_ID, param);
		String name = getSubParamValueAsString(PERSPECTIVE_NAME_ID, param);
		Perspective perspective = dockingFactory.createPerspective(id, name);
		viewRootFound = false;
		for (Iterator iter = param.getSubParameters(VIEW_GROUPS_ID).iterator(); iter.hasNext();) {
			processViewGroup((Extension.Parameter) iter.next(), perspective);
		}
		if (!viewRootFound)
			throw new PluginDefinitionException("Perspective '" + id + "' does not contain a root view group");
		perspectives.add(perspective);
	}

	private void processViewGroup(Extension.Parameter param, Perspective perspective) throws PluginDefinitionException {
		String id = param.getSubParameter(VIEW_GROUP_ID).valueAsString();
		String parent = getSubParamValueAsString(VIEW_GROUP_PARENT, param);
		if (parent == null) {
			if (viewRootFound) {
				String message = "Multiple view group root groups found";
				throw new PluginDefinitionException(message);
			}
			perspective.createRootGroup(id);
			viewRootFound = true;
		} else {
			String location = getSubParamValueAsString(VIEW_GROUP_LOCATION, param);
			if (location == null) {
				String message = "View group '" + id + "' is missing a location parameter";
				throw new PluginDefinitionException(message);
			}
			if (!locationMap.containsKey(location)) {
				String message = "View group '" + id + "' has an invalid location value; must be one of" +
								" north, south, east, or west";
				throw new PluginDefinitionException(message);
			}

			float fill = getSubParamValueAsFloat(VIEW_GROUP_FILL, param);
			// default to .5 if no fill value
			if (Float.isNaN(fill)) fill = .5f;
			fill = fill > 1 ? .9f : fill < 0 ? .1f : fill;
			perspective.createGroup(id, locationMap.get(location), parent, fill);
		}
	}

	void processMenuSpec(Extension.Parameter param, MenuTreeDescriptor mtDescriptor) {
		if (param.getId().equals("menuSpec")) {
			String id = param.getSubParameter(MENU_ID).valueAsString();
			String label = param.getSubParameter(MENU_LABEL_ID).valueAsString();

			String parentID = null;
			if (param.getSubParameter(MENU_PARENT_ID) != null) {
				parentID = param.getSubParameter(MENU_PARENT_ID).valueAsString();
			}

			mtDescriptor.addMenu(id, label, parentID);
		}
	}


	void processActionSpec(Extension.Parameter param) throws PluginDefinitionException {
		if (param.getId().equals("separator")) {
			String menuID = null;
			String toolbarID = null;
			if (param.getSubParameter(MENU_PATH) != null)
				menuID = param.getSubParameter(MENU_PATH).valueAsString();
			if (param.getSubParameter(TOOL_PATH) != null)
				toolbarID = param.getSubParameter(TOOL_PATH).valueAsString();
			GUICreatorDelegate.getInstance().addBarItemDescriptor(new SeparatorDescriptor(menuID, toolbarID));
		} else {
			PluginClassLoader pluginClassLoader = getManager().getPluginClassLoader(param.getDeclaringPluginDescriptor());
			ActionDescriptor descriptor;
			String id = param.getSubParameter(ACTION_ID).valueAsString();
			if (id.equals(GUIConstants.EXIT_ACTION)) {
				descriptor = new ExitActionDescriptor(id);
			} else {
				String className = param.getSubParameter(CLASS_NAME).valueAsString();
				//System.out.println("className = " + className);
				try {
					Class clazz = Class.forName(className, true, pluginClassLoader);
					ISAFAction action = (ISAFAction) clazz.newInstance();
					descriptor = new ActionDescriptor(id, action);
				} catch (ClassCastException e) {
					throw new PluginDefinitionException("ActionSpec class '" + className + "'in '" + id + "' must implement ISAFAction", e);
				} catch (ClassNotFoundException e) {
					throw new PluginDefinitionException("Unable to create class '" + className + "' in ActionSpec '" + id + "'", e);
				} catch (NoClassDefFoundError e) {
					throw new PluginDefinitionException("Unable to create class '" + className + "' in ActionSpec '" + id + "'", e);
				} catch (IllegalAccessException e) {
					throw new PluginDefinitionException("Unable to create class '" + className + "' in ActionSpec '" + id + "'", e);
				} catch (InstantiationException e) {
					throw new PluginDefinitionException("Unable to create class '" + className + "' in ActionSpec '" + id + "'", e);
				}
			}

			GUICreatorDelegate.getInstance().addBarItemDescriptor(descriptor);

			if (param.getSubParameter(ICON_PATH) != null) {
				String iconPath = param.getSubParameter(ICON_PATH).valueAsString();
				Icon icon;
				if (iconPath != null) {
					URL iconURL = pluginClassLoader.getResource(iconPath);
					icon = new ImageIcon(iconURL);
					descriptor.setIcon(icon);
				}
			}

			if (param.getSubParameter(LABEL) != null)
				descriptor.setLabel(param.getSubParameter(LABEL).valueAsString());
			if (param.getSubParameter(MENU_PATH) != null)
				descriptor.setMenuID(param.getSubParameter(MENU_PATH).valueAsString());
			if (param.getSubParameter(TOOL_PATH) != null)
				descriptor.setToolbarGroupID(param.getSubParameter(TOOL_PATH).valueAsString());
			if (param.getSubParameter(TOOLTIP) != null)
				descriptor.setTooltip(param.getSubParameter(TOOLTIP).valueAsString());
      if (param.getSubParameter(COMMAND) != null)
        descriptor.setActionCommand(param.getSubParameter(COMMAND).valueAsString());


    }
	}
}
