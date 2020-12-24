package org.jetbrains.plugins.ideavim.propertybased

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.key.CommandNode
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.ideavim.VimTestCase

class YankDeletePropertyTest : VimPropertyTest() {
  fun testYankDelete() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(loremText)
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(YankDeleteActions(editor)))
        } finally {
          reset(editor)
        }
      }
    }
  }
}

private class YankDeleteActions(private val editor: Editor) : ImperativeCommand {
  override fun performCommand(env: ImperativeCommand.Environment) {
    val key = env.generateValue(Generator.sampledFrom(keysList), null)

    env.logMessage("Use command: $key")
    VimTestCase.typeText(parseKeys(key), editor, editor.project)

    IdeEventQueue.getInstance().flushQueue()
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }
}

private val keysList =
  arrayListOf("v", "V", "<C-V>", "h", "j", "k", "l", "w", "e", "b", "y", "Y", "_", "d", "D", "c", "C", "p", "P")
