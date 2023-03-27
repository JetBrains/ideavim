/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.maddyhome.idea.vim.group.KeyGroup;
import com.maddyhome.idea.vim.handler.ActionBeanClass;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.key.MappingOwner;
import com.maddyhome.idea.vim.newapi.IjVimActionsInitiator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class RegisterActions {

  public static final ExtensionPointName<ActionBeanClass> VIM_ACTIONS_EP =
    ExtensionPointName.create("IdeaVIM.vimAction");

  /**
   * Register all the key/action mappings for the plugin.
   */
  public static void registerActions() {
    registerVimCommandActions();
    registerEmptyShortcuts();
    registerEpListener();
  }

  private static void registerEpListener() {
    // IdeaVim doesn't support contribution to VIM_ACTIONS_EP extension point, so technically we can skip this update,
    //   but let's support dynamic plugins in a more classic way and reload actions on every EP change.
    VIM_ACTIONS_EP.addChangeListener(() -> {
      unregisterActions();
      registerActions();
    }, VimPlugin.getInstance());
  }

  public static @Nullable
  EditorActionHandlerBase findAction(@NotNull String id) {
    return VIM_ACTIONS_EP.getExtensionList(ApplicationManager.getApplication()).stream()
      .filter(vimActionBean -> vimActionBean.getActionId().equals(id)).findFirst().map(ActionBeanClass::getInstance)
      .orElse(null);
  }

  public static @NotNull
  EditorActionHandlerBase findActionOrDie(@NotNull String id) {
    EditorActionHandlerBase action = findAction(id);
    if (action == null) throw new RuntimeException("Action " + id + " is not registered");
    return action;
  }

  public static void unregisterActions() {
    KeyGroup keyGroup = VimPlugin.getKeyIfCreated();
    if (keyGroup != null) {
      keyGroup.unregisterCommandActions();
    }
  }

  private static void registerVimCommandActions() {
    KeyGroup parser = VimPlugin.getKey();
    VIM_ACTIONS_EP.getExtensionList(ApplicationManager.getApplication()).stream().map(IjVimActionsInitiator::new)
      .forEach(parser::registerCommandAction);
  }

  private static void registerEmptyShortcuts() {
    final KeyGroup parser = VimPlugin.getKey();

    // The {char1} <BS> {char2} shortcut is handled directly by KeyHandler#handleKey, so doesn't have an action. But we
    // still need to register the shortcut, to make sure the editor doesn't swallow it.
    parser
      .registerShortcutWithoutAction(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), MappingOwner.IdeaVim.System.INSTANCE);
  }
}
