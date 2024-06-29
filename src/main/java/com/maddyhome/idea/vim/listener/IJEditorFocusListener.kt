/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorKind
import com.maddyhome.idea.vim.KeyHandlerStateResetter
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.newapi.ij

/**
 * This listener is similar to the one we introduce in vim-engine to handle focus change,
 * However, in IJ we would like to start editing in some editors in INSERT mode (e.g., consoles)
 * It is different to we had previously. Now we go to INSERT mode not only when we focus on the console the first time, but every time.
 * Going to INSERT on every focus is easier to implement and more consistent (behavior is always the same, you don't have to remember if you are focusing a console the first time or not)
 */
class IJEditorFocusListener : KeyHandlerStateResetter() {
  override fun focusGained(editor: VimEditor) {
    // We add Vim bindings to all opened editors, including editors used as UI controls rather than just project file
    // editors. This includes editors used as part of the UI, such as the VCS commit message, or used as read-only
    // viewers for text output, such as log files in run configurations or the Git Console tab. And editors are used for
    // interactive stdin/stdout for console-based run configurations.
    // We want to provide an intuitive experience for working with these additional editors, so we automatically switch
    // to INSERT mode if they are interactive editors. Recognising these can be a bit tricky.
    // These additional interactive editors are not file-based, but must have a writable document. However, log output
    // documents are also writable (the IDE is writing new content as it becomes available) just not user-editable. So
    // we must also check that the editor is not in read-only "viewer" mode (this includes "rendered" mode, which is
    // read-only and also hides the caret).
    // Furthermore, interactive stdin/stdout console output in run configurations is hosted in a read-only editor, but
    // it can still be edited. The `ConsoleViewImpl` class installs a typing handler that ignores the editor's
    // `isViewer` property and allows typing if the associated process (if any) is still running. We can get the
    // editor's console view and check this ourselves, but we have to wait until the editor has finished initialising
    // before it's available in user data.
    // Finally, we have a special check for diff windows. If we compare against clipboard, we get a diff editor that is
    // not file based, is writable, and not a viewer, but we don't want to treat this as an interactive editor.
    // Note that we need a similar check in `VimEditor.isWritable` to allow Escape to work to exit insert mode. We need
    // to know that a read-only editor that is hosting a console view with a running process can be treated as writable.
    val switchToInsertMode = Runnable {
      val context: ExecutionContext = injector.executionContextManager.getEditorExecutionContext(editor)
      VimPlugin.getChange().insertBeforeCursor(editor, context)
    }
    val ijEditor = editor.ij
    if (!ijEditor.isViewer &&
      !EditorHelper.isFileEditor(ijEditor) &&
      ijEditor.document.isWritable &&
      !ijEditor.inInsertMode && ijEditor.editorKind != EditorKind.DIFF
    ) {
      switchToInsertMode.run()
    }
    ApplicationManager.getApplication().invokeLater {
      if (ijEditor.isDisposed) return@invokeLater
      val consoleView: ConsoleViewImpl? = ijEditor.getUserData(ConsoleViewImpl.CONSOLE_VIEW_IN_EDITOR_VIEW)
      if (consoleView != null && consoleView.isRunning && !ijEditor.inInsertMode) {
        switchToInsertMode.run()
      }
    }
    super.focusGained(editor)
  }
}