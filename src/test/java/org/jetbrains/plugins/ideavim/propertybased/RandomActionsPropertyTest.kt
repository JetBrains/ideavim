/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.propertybased

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.key.CommandNode
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.propertybased.samples.javaText
import org.jetbrains.plugins.ideavim.propertybased.samples.loremText
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Property based tests based of JetCheck framework
 *
 * See the log if this test fails, it contains the instructions on how to reproduce the test.
 */
class RandomActionsPropertyTest : VimPropertyTestBase() {
  fun testRandomActions() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(text)
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(AvailableActions(editor)))
        } finally {
          reset(editor)
        }
      }
    }
  }

  fun testRandomActionsOnLoremIpsum() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(loremText)
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(AvailableActions(editor)))
        } finally {
          reset(editor)
        }
      }
    }
  }

  fun testRandomActionsOnJavaCode() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByJavaText(javaText)
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(AvailableActions(editor)))
        } finally {
          reset(editor)
        }
      }
    }
  }

  companion object {
    private val text = """
              ${c}I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
    """.trimIndent()
  }
}

private class AvailableActions(private val editor: Editor) : ImperativeCommand {
  override fun performCommand(env: ImperativeCommand.Environment) {
    val currentNode = editor.vim.vimStateMachine.commandBuilder.getCurrentTrie()

    val possibleKeys = currentNode.keys.toList().sortedBy { injector.parser.toKeyNotation(it) }
    val keyGenerator = Generator.integers(0, possibleKeys.lastIndex)
      .suchThat { injector.parser.toKeyNotation(possibleKeys[it]) !in stinkyKeysList }
      .map { possibleKeys[it] }

    val usedKey = env.generateValue(keyGenerator, null)
    val node = currentNode[usedKey]

    env.logMessage("Use command: ${injector.parser.toKeyNotation(usedKey)}. ${if (node is CommandNode) "Action: ${node.actionHolder.ij.actionId}" else ""}")
    VimTestCase.typeText(listOf(usedKey), editor, editor.project)

    IdeEventQueue.getInstance().flushQueue()
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }
}

private val stinkyKeysList = arrayListOf(
  "K", "u", "H", "<C-Y>",
  injector.parser.toKeyNotation(KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0)), "L", "!", "<C-D>", "z", "<C-W>",
  "g", "<C-U>",

  // Temporally disabled due to issues in the platform
  "<C-V>", "<C-Q>",

  "<C-]>",

  // Next / previous method fails because of vfs sync
  "]"
)
