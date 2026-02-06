/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.utils.agent

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.keyboard
import ui.pages.idea
import ui.utils.invokeActionJs
import java.awt.event.KeyEvent

/**
 * Translates [AgentAction]s from Claude into Remote Robot calls against the running IDE.
 */
class ActionExecutor(private val remoteRobot: RemoteRobot) {

  fun execute(action: AgentAction): String {
    return when (action.type) {
      "keyboard" -> executeKeyboard(action.params)
      "invoke_action" -> executeInvokeAction(action.params)
      "click_text" -> executeClickText(action.params)
      "wait" -> executeWait(action.params)
      "vim_command" -> executeVimCommand(action.params)
      "escape" -> executeEscape()
      else -> "Unknown action type: ${action.type}"
    }
  }

  private fun executeKeyboard(params: Map<String, String>): String {
    val keys = params["keys"] ?: return "Missing 'keys' parameter"

    // Handle special key combos like "ctrl+BACK_SLASH"
    if (keys.contains("+")) {
      val parts = keys.lowercase().split("+")
      remoteRobot.idea {
        keyboard {
          when {
            parts.containsAll(listOf("ctrl", "back_slash")) || parts.containsAll(listOf("control", "back_slash")) -> {
              hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_BACK_SLASH)
            }

            parts.containsAll(listOf("meta", "back_slash")) || parts.containsAll(listOf("cmd", "back_slash")) -> {
              hotKey(KeyEvent.VK_META, KeyEvent.VK_BACK_SLASH)
            }

            parts.containsAll(listOf("ctrl", "shift")) -> {
              val key = parts.first { it != "ctrl" && it != "shift" }
              hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, keyEventCode(key))
            }

            parts.contains("ctrl") -> {
              val key = parts.first { it != "ctrl" }
              hotKey(KeyEvent.VK_CONTROL, keyEventCode(key))
            }

            parts.contains("meta") || parts.contains("cmd") -> {
              val modifier = if (parts.contains("meta")) "meta" else "cmd"
              val key = parts.first { it != modifier }
              hotKey(KeyEvent.VK_META, keyEventCode(key))
            }

            else -> enterText(keys)
          }
        }
      }
      return "Pressed key combo: $keys"
    }

    remoteRobot.idea {
      keyboard { enterText(keys) }
    }
    return "Typed: $keys"
  }

  private fun executeInvokeAction(params: Map<String, String>): String {
    val actionId = params["action_id"] ?: return "Missing 'action_id' parameter"
    remoteRobot.invokeActionJs(actionId)
    return "Invoked action: $actionId"
  }

  private fun executeClickText(params: Map<String, String>): String {
    val text = params["text"] ?: return "Missing 'text' parameter"
    remoteRobot.idea {
      findText(text).click()
    }
    return "Clicked text: $text"
  }

  private fun executeWait(params: Map<String, String>): String {
    val durationMs = params["duration_ms"]?.toLongOrNull() ?: 500L
    Thread.sleep(durationMs)
    return "Waited ${durationMs}ms"
  }

  private fun executeVimCommand(params: Map<String, String>): String {
    val command = params["command"] ?: return "Missing 'command' parameter"
    remoteRobot.idea {
      keyboard {
        escape()
        enterText(":$command")
        enter()
      }
    }
    return "Executed vim command: :$command"
  }

  private fun executeEscape(): String {
    remoteRobot.idea {
      keyboard { escape() }
    }
    return "Pressed Escape"
  }

  private fun keyEventCode(key: String): Int {
    return when (key.uppercase()) {
      "A" -> KeyEvent.VK_A
      "B" -> KeyEvent.VK_B
      "C" -> KeyEvent.VK_C
      "D" -> KeyEvent.VK_D
      "E" -> KeyEvent.VK_E
      "F" -> KeyEvent.VK_F
      "G" -> KeyEvent.VK_G
      "H" -> KeyEvent.VK_H
      "I" -> KeyEvent.VK_I
      "J" -> KeyEvent.VK_J
      "K" -> KeyEvent.VK_K
      "L" -> KeyEvent.VK_L
      "M" -> KeyEvent.VK_M
      "N" -> KeyEvent.VK_N
      "O" -> KeyEvent.VK_O
      "P" -> KeyEvent.VK_P
      "Q" -> KeyEvent.VK_Q
      "R" -> KeyEvent.VK_R
      "S" -> KeyEvent.VK_S
      "T" -> KeyEvent.VK_T
      "U" -> KeyEvent.VK_U
      "V" -> KeyEvent.VK_V
      "W" -> KeyEvent.VK_W
      "X" -> KeyEvent.VK_X
      "Y" -> KeyEvent.VK_Y
      "Z" -> KeyEvent.VK_Z
      "ENTER" -> KeyEvent.VK_ENTER
      "ESCAPE", "ESC" -> KeyEvent.VK_ESCAPE
      "TAB" -> KeyEvent.VK_TAB
      "SPACE" -> KeyEvent.VK_SPACE
      "BACK_SLASH" -> KeyEvent.VK_BACK_SLASH
      "SLASH" -> KeyEvent.VK_SLASH
      else -> KeyEvent.VK_UNDEFINED
    }
  }
}
