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
  private val stack = ArrayDeque<Frame>()

  fun hasStroke(): Boolean {
    return stack.isNotEmpty() && stack.first().hasStroke()
  }

  fun feedStroke(): KeyStroke {
    return stack.first().feed()
  }

  fun addKeys(keyStrokes: List<KeyStroke>) {
    stack.addFirst(Frame(keyStrokes))
  }

  fun removeFirst() {
    if (stack.isNotEmpty()) {
      stack.removeFirst()
    }
  }
}

private class Frame(
  val keys: List<KeyStroke>,
  var pointer: Int = 0,
) {
  fun hasStroke(): Boolean {
    return pointer < keys.size
  }

  fun feed(): KeyStroke {
    val key = keys[pointer]
    pointer += 1
    return key
  }
}