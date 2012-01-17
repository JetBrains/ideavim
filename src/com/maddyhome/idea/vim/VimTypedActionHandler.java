package com.maddyhome.idea.vim;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * This handler accepts all regular keystrokes and passes them on to the Vim Key handler
 */
public class VimTypedActionHandler implements TypedActionHandler {
  /**
   * Creates an instance of the key handler
   *
   * @param origHandler The original key handler
   */
  public VimTypedActionHandler(TypedActionHandler origHandler) {
    this.origHandler = origHandler;
    handler = KeyHandler.getInstance();
    handler.setOriginalHandler(origHandler);
  }

  /**
   * Gives the original key handler
   *
   * @return The original key handler
   */
  public TypedActionHandler getOriginalTypedHandler() {
    return origHandler;
  }

  /**
   * All characters typed into an editor will get sent to this handler. Only letters, numbers, and punctuation
   * are sent here. Keys like Tab, Enter, Home, Backspace, etc. and all Control-Letter etc. argType keys are not
   * sent by Idea to this handler.
   *
   * @param editor    The editor the character was typed into
   * @param charTyped The character that was typed
   * @param context   The data context
   */
  public void execute(@NotNull final Editor editor, final char charTyped, @NotNull final DataContext context) {
    // If the plugin is disabled we simply resend the character to the original handler
    if (!VimPlugin.isEnabled()) {
      origHandler.execute(editor, charTyped, context);
      return;
    }
    // In case if keystrokes go to lookup, we use original handler
    final LookupEx lookup = LookupManager.getActiveLookup(editor);
    if (lookup != null && lookup.isFocused()) {
      origHandler.execute(editor, charTyped, context);
      return;
    }

    try {
      handler.handleKey(editor, KeyStroke.getKeyStroke(charTyped), context);
    }
    catch (Throwable e) {
      logger.error(e);
    }
  }

  private TypedActionHandler origHandler;
  private KeyHandler handler;

  private static Logger logger = Logger.getInstance(VimTypedActionHandler.class.getName());
}
