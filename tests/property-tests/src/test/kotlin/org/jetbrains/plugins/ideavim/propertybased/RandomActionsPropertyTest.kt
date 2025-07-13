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
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.key
import com.maddyhome.idea.vim.helper.vimKeyStroke
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.propertybased.samples.javaText
import org.jetbrains.plugins.ideavim.propertybased.samples.loremText
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Property based tests based of JetCheck framework
 *
 * See the log if this test fails, it contains the instructions on how to reproduce the test.
 */
class RandomActionsPropertyTest : VimPropertyTestBase() {
  @Test
  fun testRandomActions() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(text)
        ApplicationManager.getApplication().invokeAndWait {
          KeyHandler.getInstance().fullReset(editor.vim)
        }
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(AvailableActions(editor)))
        } finally {
          ApplicationManager.getApplication().invokeAndWait {
            reset(editor)
          }
        }
      }
    }
  }

  @Test
  fun testRandomActionsOnLoremIpsum() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByText(loremText)
        ApplicationManager.getApplication().invokeAndWait {
          KeyHandler.getInstance().fullReset(editor.vim)
        }
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(AvailableActions(editor)))
        } finally {
          ApplicationManager.getApplication().invokeAndWait {
            reset(editor)
          }
        }
      }
    }
  }

  @Test
  fun testRandomActionsOnJavaCode() {
    PropertyChecker.checkScenarios {
      ImperativeCommand { env ->
        val editor = configureByJavaText(javaText)
        ApplicationManager.getApplication().invokeAndWait {
          KeyHandler.getInstance().fullReset(editor.vim)
        }
        try {
          moveCaretToRandomPlace(env, editor)
          env.executeCommands(Generator.sampledFrom(AvailableActions(editor)))
        } finally {
          ApplicationManager.getApplication().invokeAndWait {
            reset(editor)
          }
        }
      }
    }
  }

  companion object {
    private val text = """
              ${c}Lorem ipsum dolor sit amet,
              consectetur adipiscing elit
              Sed in orci mauris.
              Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
  }
}

private class AvailableActions(private val editor: Editor) : ImperativeCommand {
  override fun performCommand(env: ImperativeCommand.Environment) {
    val trie = KeyHandler.getInstance().keyHandlerState.commandBuilder.getCurrentTrie()
    val currentKeys = KeyHandler.getInstance().keyHandlerState.commandBuilder.getCurrentCommandKeys()

    // Note: esc is always an option
    val possibleKeys: List<VimKeyStroke> = buildList {
      add(esc)
      trie.getTrieNode(currentKeys)?.visit { stroke, _ -> add(stroke) }
    }.sortedBy { injector.parser.toKeyNotation(it) }

//    println("Keys: ${possibleKeys.joinToString(", ")}")
    val keyGenerator = Generator.integers(0, possibleKeys.lastIndex)
      .suchThat { injector.parser.toKeyNotation(possibleKeys[it]) !in stinkyKeysList }
      .map { possibleKeys[it] }

    val usedKey = env.generateValue(keyGenerator, null)
    val node = trie.getTrieNode(currentKeys + usedKey)
    env.logMessage("Use command: ${injector.parser.toKeyNotation(currentKeys + usedKey)}. ${if (node?.data != null) "Action: ${node.data!!.actionId}" else ""}")
    VimTestCase.typeText(listOf(usedKey), editor, editor.project)

    ApplicationManager.getApplication().invokeAndWait {
      IdeEventQueue.getInstance().flushQueue()
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }

  private val esc = key("<Esc>")
}

private val stinkyKeysList = arrayListOf(
  "K", "u", "H", "<C-Y>",
  injector.parser.toKeyNotation(KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0).vimKeyStroke), "L", "!", "<C-D>", "z", "<C-W>",
  "g", "<C-U>",

  // Temporally disabled due to issues in the platform
  "<C-V>", "<C-Q>",

  "<C-]>",

  // Next / previous method fails because of vfs sync
  "]",

  // Doesn't work with test implementation of splitters
  "<C-Pageup>", "<C-Pagedown>", "<C-N>",
)
