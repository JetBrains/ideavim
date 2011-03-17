package com.maddyhome.idea.vim;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;

/**
 * This action allows to reconfigure base parent keymap for the Vim keymap
 * @author oleg
 */
public class VimReconfigureKeymapAction extends AnAction implements DumbAware {
  @Override
  public void update(final AnActionEvent e) {
    e.getPresentation().setEnabled(VimPlugin.isEnabled());
  }

  @Override
  public void actionPerformed(final AnActionEvent e) {
    VimKeyMapUtil.reconfigureParentKeymap();
  }
}
