package com.maddyhome.idea.vim;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;

/**
 * This class is used to handle the Vim Plugin enabled/disabled toggle. This is most likely used as a menu option
 * but could also be used as a toolbar item.
 */
public class VimPluginToggleAction extends ToggleAction implements DumbAware {
  /**
   * Indicates if the toggle is on or off
   *
   * @param event The event that triggered the action
   * @return true if the toggle is on, false if off
   */
  public boolean isSelected(AnActionEvent event) {
    return VimPlugin.isEnabled();
  }

  /**
   * Specifies whether the toggle should be on or off
   *
   * @param event The event that triggered the action
   * @param b     The new state - true is on, false is off
   */
  public void setSelected(AnActionEvent event, boolean b) {
    VimPlugin.setEnabled(b);
  }
}
