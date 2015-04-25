package com.maddyhome.idea.vim.option;

import com.maddyhome.idea.vim.action.plugin.Plugin;

/**
 * @author dhleong
 */
public class PluginOption extends ToggleOption {
  /**
   * Creates the option
   *
   * @param name   The option's name
   */
  PluginOption(String name) {
    super(name, name, false);
  }

  @Override
  public void set() {
    super.set();
    Plugin.Registrar.activate(getName());
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("Plugins cannot yet be disabled at runtime");
  }
}
