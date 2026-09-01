/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector

/**
 * Expands `%` together with its `:p`, `:h`, `:t`, `:r` and `:e` modifiers.
 *
 * `%` on its own is the *buffer name*, not a full path. Vim keeps that name relative to the working directory while
 * the file is below it, and only `:p` forces the full path (see `:help cmdline-special` and
 * `:help filename-modifiers`). IdeaVim has no `:cd`, so the buffer name comes from
 * [com.maddyhome.idea.vim.api.VimFile.bufferName] - the same value `:file` and the `%` register report, i.e. relative
 * to the file's content root, and absolute for a file outside every root.
 */
object PathCompletionArgumentParser {

  private val knownModifiers = listOf(AbsolutePath(), HeadDirectory(), TailFilename(), RootFilename(), ExtensionOnly())

  fun expandPercent(argumentPrefix: String, editor: VimEditor): String {
    val (expanded, consumed) = expandPercentPrefix(argumentPrefix, editor) ?: return argumentPrefix
    return expanded + argumentPrefix.substring(consumed)
  }

  /**
   * Expands the `%` that starts [argument], ignoring whatever follows the modifiers.
   *
   * @return the expanded name and the number of characters of [argument] it was built from, so a caller scanning a
   *   longer string knows where to resume. Null if the buffer has no name, which is Vim's E499.
   */
  internal fun expandPercentPrefix(argument: String, editor: VimEditor): Pair<String, Int>? {
    val (modifiers, suffix) = parse(argument)
    var resultPath = injector.file.bufferName(editor) ?: return null
    for (modifier in modifiers) {
      resultPath = modifier.complete(resultPath, editor)
    }
    return resultPath to (argument.length - suffix.length)
  }

  internal fun parse(argument: String): Pair<List<PathCompletion>, String> {
    val list = mutableListOf<PathCompletion>()

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

/**
 * `:p` — make the file name a full path.
 *
 * Vim requires `:p` to be the first modifier, so the name reaching this point is always the unmodified buffer name.
 * Rather than resolve a relative name against a working directory IdeaVim does not track, ask the platform for the
 * buffer's full path directly.
 */
class AbsolutePath : PathCompletion {
  override fun complete(path: String, editor: VimEditor): String {
    return injector.file.fullPathBufferName(editor) ?: path
  }

  override fun modifier(): String = "p"
}

class HeadDirectory : PathCompletion {
  override fun complete(path: String, editor: VimEditor): String {
    val head = path.substringBeforeLast('/', "")
    return when {
      head.isNotEmpty() -> head
      // Vim keeps the root separator for "/foo.txt" and turns any other empty head into "."
      path.startsWith('/') -> "/"
      else -> "."
    }
  }

  override fun modifier(): String = "h"
}

class TailFilename : PathCompletion {
  override fun complete(path: String, editor: VimEditor): String {
    return path.substringAfterLast('/', path)
  }

  override fun modifier(): String = "t"
}

class RootFilename : PathCompletion {
  override fun complete(path: String, editor: VimEditor): String = path.substringBeforeLast('.')
  override fun modifier(): String = "r"
}

class ExtensionOnly : PathCompletion {
  override fun complete(path: String, editor: VimEditor): String = path.substringAfterLast('.', "")
  override fun modifier(): String = "e"
}
