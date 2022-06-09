package com.maddyhome.idea.vim.key

import javax.swing.KeyStroke

/**
 * This class stacks ToKeys mappings
 * Previously, the mapping was directly sent to the [KeyHandler]. However, in this case it was impossible to
 *   pass the key to modal entry (for getChar function).
 * Original vim uses a typeahead buffer for that, but it's not implemented for IdeaVim and this mappingStack
 *   solves an issus of passing a keystroke to modal entry.
 *   However, some more advanced solution may be necessary in the future.
 */
class MappingStack {
  private val stack = ArrayDeque<List<KeyStroke>>()

  fun hasStroke(): Boolean {
    return stack.isNotEmpty() && stack.first().isNotEmpty()
  }

  fun getStroke(): KeyStroke {
    val keysList = stack.first()
    val keyStroke = keysList[0]
    stack.removeFirst()
    stack.addFirst(keysList.subList(1, keysList.size))
    return keyStroke
  }

  fun addKeys(keyStrokes: List<KeyStroke>) {
    stack.addFirst(keyStrokes)
  }

  fun removeFirst() {
    if (stack.isNotEmpty()) {
      stack.removeFirst()
    }
  }
}
