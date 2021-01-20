/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.propertybased

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.MotionGroup
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.plugins.ideavim.VimTestCase

abstract class VimPropertyTest : VimTestCase() {
  protected fun moveCaretToRandomPlace(env: ImperativeCommand.Environment, editor: Editor) {
    val pos = env.generateValue(Generator.integers(0, editor.document.textLength - 1), "Put caret at position %s")
    MotionGroup.moveCaret(editor, editor.caretModel.currentCaret, pos)
  }

  protected fun reset(editor: Editor) {
    KeyHandler.getInstance().fullReset(editor)
    VimPlugin.getRegister().resetRegisters()
    editor.caretModel.runForEachCaret { it.moveToOffset(0) }

    CommandState.getInstance(editor).resetDigraph()
    VimPlugin.getSearch().resetState()
    VimPlugin.getChange().reset()
  }

}
