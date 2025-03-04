/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.propertybased

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.plugins.ideavim.VimTestCase

abstract class VimPropertyTestBase : VimTestCase() {
  protected fun moveCaretToRandomPlace(env: ImperativeCommand.Environment, editor: Editor) {
    val pos = env.generateValue(Generator.integers(0, editor.document.textLength - 1), "Put caret at position %s")
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        editor.caretModel.currentCaret.vim.moveToOffset(pos)
      }
    }
  }

  protected fun reset(editor: Editor) {
    val keyState = KeyHandler.getInstance().keyHandlerState
    keyState.mappingState.resetMappingSequence()
    VimPlugin.getKey().resetKeyMappings()

    KeyHandler.getInstance().fullReset(editor.vim)
    VimPlugin.getRegister().resetRegisters()
    editor.caretModel.runForEachCaret { it.moveToOffset(0) }

    VimPlugin.getSearch().resetState()
    VimPlugin.getChange().reset()
  }

  protected fun configureByJavaText(content: String) = configureByText(JavaFileType.INSTANCE, content)
}
