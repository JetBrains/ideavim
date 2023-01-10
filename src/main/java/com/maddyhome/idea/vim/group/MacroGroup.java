/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.PotemkinProgress;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.key.KeyStack;
import com.maddyhome.idea.vim.macro.VimMacroBase;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Used to handle playback of macros
 */
public class MacroGroup extends VimMacroBase {
  private static final Logger logger = Logger.getInstance(MacroGroup.class.getName());

  /**
   * This puts a single keystroke at the end of the event queue for playback
   */
  @Override
  public void playbackKeys(@NotNull final VimEditor editor,
                           @NotNull final ExecutionContext context, final int cnt,
                           final int total) {
    Project project = ((IjVimEditor)editor).getEditor().getProject();
    KeyStack keyStack = KeyHandler.getInstance().getKeyStack();
    if (!keyStack.hasStroke() || cnt >= total) {
      logger.debug("done");
      keyStack.removeFirst();

      return;
    }

    PotemkinProgress potemkinProgress =
      new PotemkinProgress(MessageHelper.message("progress.title.macro.execution"), project, null,
                           MessageHelper.message("stop"));
    potemkinProgress.setIndeterminate(false);
    potemkinProgress.setFraction(0);
    potemkinProgress.runInSwingThread(() -> {
      // Handle one keystroke then queue up the next key
      for (int i = 0; i < total; ++i) {
        potemkinProgress.setFraction((double)(i + 1) / total);
        while (keyStack.hasStroke()) {
          KeyStroke key = keyStack.feedStroke();
          try {
            potemkinProgress.checkCanceled();
          }
          catch (ProcessCanceledException e) {
            return;
          }
          ProgressManager.getInstance().executeNonCancelableSection(() -> {
            KeyHandler.getInstance().handleKey(editor, key, context);
          });
        }
        keyStack.resetFirst();
      }
      keyStack.removeFirst();
    });
  }
}
