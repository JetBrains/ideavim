/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.propertybased

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.keyStroke
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.propertybased.samples.loremText
import org.junit.jupiter.api.Test

class YankDeletePropertyTest : VimPropertyTestBase() {
  @Test
  fun testYankDelete() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(loremText)
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(YankDeleteActions(editor)))
        } finally {
          ApplicationManager.getApplication().invokeAndWait {
            reset(editor)
          }
        }
      }
    }
  }
}

private class YankDeleteActions(private val editor: Editor) : ImperativeCommand {
  override fun performCommand(env: ImperativeCommand.Environment) {
    val key = env.generateValue(Generator.sampledFrom(keysList), null)

    env.logMessage("Use command: $key")
    VimTestCase.typeText(injector.parser.parseKeys(key), editor, editor.project)

    ApplicationManager.getApplication().invokeAndWait {
      IdeEventQueue.getInstance().flushQueue()
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }
}

private val keysList =
  arrayListOf("v", "V", "<C-V>", "h", "j", "k", "l", "w", "e", "b", "y", "Y", "_", "d", "D", "c", "C", "p", "P")
