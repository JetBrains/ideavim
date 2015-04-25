package com.maddyhome.idea.vim.action.plugin;

import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.plugin.surround.SurroundPlugin;
import com.maddyhome.idea.vim.group.KeyGroup;
import com.maddyhome.idea.vim.option.Options;

import java.util.ArrayList;
import java.util.List;

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

    /** Probably, just registerActions(String) is fine */
    @Deprecated
    public static void registerActions() {
      final KeyGroup parser = VimPlugin.getKey();
      for (Plugin plugin : plugins) {
        if (Options.getInstance().isSet(plugin.getOptionName())) {
          plugin.registerActions(parser);
        }
      }
    }

    public static Iterable<String> getOptionNames() {
      List<String> optionNames = new ArrayList<String>();
      for (Plugin plugin : plugins) {
        optionNames.add(plugin.getOptionName());
      }
      return optionNames;
    }

    public static void registerActions(String name) {
      // TODO put these in a map
      final KeyGroup parser = VimPlugin.getKey();
      for (Plugin plugin : plugins) {
        if (name.equals(plugin.getOptionName())) {
          plugin.registerActions(parser);
          return;
        }
      }
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
