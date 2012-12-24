package com.maddyhome.idea.vim;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import org.jetbrains.annotations.NotNull;

/**
 * This action allows to reconfigure base parent keymap for the Vim keymap
 * @author oleg
 */
public class VimReconfigureKeymapAction extends AnAction implements DumbAware {
  @Override
  public void update(@NotNull final AnActionEvent e) {
    e.getPresentation().setEnabled(VimPlugin.isEnabled());
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
      VimKeyMapUtil.reconfigureParentKeymap(e.getData(PlatformDataKeys.PROJECT));
  }
}
