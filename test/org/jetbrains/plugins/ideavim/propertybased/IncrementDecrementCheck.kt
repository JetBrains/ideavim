/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.helper.StringHelper
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.ideavim.VimTestCase

class IncrementDecrementTest : VimPropertyTest() {
  fun testPlayingWithNumbers() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(numbers)
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(IncrementDecrementActions(editor)))
        } finally {
          reset(editor)
        }
      }
    }
  }
}

private class IncrementDecrementActions(private val editor: Editor) : ImperativeCommand {
  override fun performCommand(env: ImperativeCommand.Environment) {
    val generator = Generator.sampledFrom("<C-A>", "<C-X>").map { StringHelper.parseKeys(it).single() }
    val action = env.generateValue(generator, null)
    env.logMessage("Use command: ${StringHelper.toKeyNotation(action)}.")
    VimTestCase.typeText(listOf(action), editor, editor.project)

    IdeEventQueue.getInstance().flushQueue()
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }
}
