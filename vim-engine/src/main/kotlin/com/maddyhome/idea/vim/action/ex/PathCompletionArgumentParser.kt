/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.maddyhome.idea.vim.api.VimEditor

object PathCompletionArgumentParser {

  private val knownModifiers = listOf(AbsolutePath(), HeadDirectory())

  fun parse(argument: String): Pair<List<PathCompletion>, String> {
    val list = mutableListOf<PathCompletion>(AbsolutePath())
    if (argument == "%") return Pair(list, "")

    var pos = 1 // skip leading '%'
    while (pos < argument.length && argument[pos] == ':') {
      pos++ // skip ':'
      val modifierStart = pos
      while (pos < argument.length && argument[pos].isLetter()) pos++
      val modifierName = argument.substring(modifierStart, pos)
      val completion = knownModifiers.find { it.modifier() == modifierName }
      if (completion != null) {
        list.add(completion)
      } else {
        // Unknown modifier — stop and treat the rest (including the ':') as a literal suffix
        pos = modifierStart - 1
        break
      }
    }
    return Pair(list, argument.substring(pos))
  }
}

interface PathCompletion {
  fun complete(path: String, editor: VimEditor): String
  fun modifier(): String
}

class AbsolutePath : PathCompletion {
  override fun complete(path: String, editor: VimEditor): String {
    return editor.getVirtualFile()?.path ?: ""
  }

  override fun modifier(): String = "p"
}

class HeadDirectory : PathCompletion {
  override fun complete(path: String, editor: VimEditor): String {
    return path.substringBeforeLast('/', "")
  }

  override fun modifier(): String = "h"
}
