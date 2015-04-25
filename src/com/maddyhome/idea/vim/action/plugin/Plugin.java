package com.maddyhome.idea.vim.action.plugin;

import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.plugin.surround.SurroundPlugin;
import com.maddyhome.idea.vim.group.KeyGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dhleong
 */
public interface Plugin {

  class Registrar {
    // register plugins here
    // Is there a more idiomatic way of doing this?
    static final Plugin[] plugins = new Plugin[] {
      new SurroundPlugin()
    };

    // store the plugins in a map for easy access
    static final Map<String, Plugin> idsToPlugins =
      new HashMap<String, Plugin>();
    static {
      for (Plugin plugin : plugins) {
        final Plugin conflict = idsToPlugins.put(plugin.getOptionName(), plugin);
        if (conflict != null) {
          throw new IllegalArgumentException(
            "Plugin " + plugin + " has a name that conflicts with " + conflict);
        }
      }
    }

    /**
     * Get a list of all known plugin names
     * @return An iterable with the names of set-able plugins
     */
    public static Iterable<String> getOptionNames() {
      return idsToPlugins.keySet();
    }

    /**
     * Activate the plugin with the given name
     * @param name The name of the plugin, as returned by getOptionName()
     */
    public static void activate(final String name) {
      final Plugin plugin = idsToPlugins.get(name);
      if (plugin == null) {
        throw new IllegalArgumentException("No such plugin: " + name);
      }

      final KeyGroup parser = VimPlugin.getKey();
      plugin.registerActions(parser);
    }
  }

  /**
   * Since plugins are built into IdeaVim, they are
   *  enabled with options. For example, the Surround
   *  plugin is called <code>surround</code>, so you
   *  would enable it with `set surround` in your .ideavimrc
   *
   * @return The name of the option
   *  used to enable this plugin
   */
  String getOptionName();

  /**
   * Hook called if your plugin is enabled
   *
   * @param parser The KeyGroup on which to register actions
   */
  void registerActions(final KeyGroup parser);
}
