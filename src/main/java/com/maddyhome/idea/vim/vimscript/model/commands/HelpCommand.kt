/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * @author vlan
 * see "h :help"
 */
@ExCommand(
  command = "h[elp]",
  barSeparates = false,
  description = "Open the IdeaVim help in a read-only window and jump to the tag {subject}, e.g. dd, v_d, i_CTRL-W or :s. Without {subject}, show the start of the help.",
)
internal data class HelpCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val subject = argument.trim()
    val line = if (subject.isEmpty()) 0 else HelpFile.findTag(subject) ?: throw exExceptionMessage("E149", subject)
    injector.virtualBufferGroup.openHelp(context, editor, HelpFile.text, line)
    return ExecutionResult.Success
  }
}

/**
 * IdeaVim's help file, generated from the descriptions of the commands by `./gradlew generateHelp`.
 *
 * It follows the format of Vim's help files: a `*tag*` marks the place `:help tag` jumps to.
 */
internal object HelpFile {
  private const val RESOURCE = "/help/ideavim.txt"

  val text: String by lazy {
    HelpFile::class.java.getResourceAsStream(RESOURCE)?.use { it.reader().readText() } ?: ""
  }

  /** The line of each tag, in the order the tags appear in the file */
  private val tags: Map<String, Int> by lazy {
    val tags = LinkedHashMap<String, Int>()
    text.lines().forEachIndexed { index, line ->
      for (match in TAG.findAll(line)) tags.putIfAbsent(match.groupValues[1], index)
    }
    tags
  }

  private val TAG = Regex("""(?<=^|\s)\*([^*\s|]+)\*(?=\s|$)""")

  /**
   * Finds the line of the tag that matches [subject] best, or null when nothing matches.
   *
   * Vim ranks every tag matching [subject] as a pattern (`:help {subject}`). This takes a simpler approach that gives
   * the same result for the usual cases: an exact match, then the Ex command of that name (`:help help` finds
   * `:help`), then a match ignoring case, then the shortest tag that starts with [subject].
   *
   * IdeaVim has no separate commands for a doubled operator like `dd` or `gUU`, the operator handles it, so these
   * lead to the operator itself.
   */
  fun findTag(subject: String): Int? {
    val operator = subject.dropLast(1)
    if (subject.length > 1 && subject.last() == operator.last() && subject !in tags) {
      tags[operator]?.let { return it }
    }
    for (candidate in candidates(subject)) {
      tags[candidate]?.let { return it }
      if (!candidate.startsWith(":")) tags[":$candidate"]?.let { return it }
    }
    for (candidate in candidates(subject)) {
      tags.entries.firstOrNull { it.key.equals(candidate, ignoreCase = true) }?.let { return it.value }
    }
    for (candidate in candidates(subject)) {
      tags.entries.filter { it.key.startsWith(candidate) }.minByOrNull { it.key.length }?.let { return it.value }
    }
    return null
  }

  /**
   * The ways to read [subject] as a tag. The user can write keys like they are written in a mapping (`<C-W>h`) or in
   * caret notation (`^Wh`) and both mean `CTRL-W_h`. But `^` can also be just a key, like in `g^`, so the subject is
   * tried without the caret notation too.
   */
  private fun candidates(subject: String): List<String> {
    val prefix = MODE_PREFIX.find(subject)?.value?.takeIf { it.length < subject.length } ?: ""
    val keys = subject.substring(prefix.length)
    val caretNotation = CARET_NOTATION.replace(keys) { "<C-${it.groupValues[1]}>" }
    return listOf(prefix + keysToTag(caretNotation), prefix + keysToTag(keys)).distinct()
  }

  private val MODE_PREFIX = Regex("^[a-z]_")
  private val CARET_NOTATION = Regex("""\^(\S)""")

  /**
   * Converts keys to a tag the way Vim's help names them: `<C-W>h` becomes `CTRL-W_h` and `g*` becomes `gstar`.
   *
   * Mirrors `keyToTag` in the script that generates the help file.
   */
  internal fun keysToTag(keys: String): String {
    val parts = mutableListOf<String>()
    var i = 0
    while (i < keys.length) {
      val close = if (keys[i] == '<') keys.indexOf('>', i + 1) else -1
      if (close > i + 1) {
        parts.add(keys.substring(i, close + 1))
        i = close + 1
      } else {
        parts.add(keys[i].toString())
        i++
      }
    }
    return buildString {
      var previousIsCtrl = false
      for (part in parts) {
        val ctrl = ctrlKey(part)
        if (isNotEmpty() && (ctrl != null || previousIsCtrl)) append('_')
        append(ctrl ?: part.replace("*", "star").replace("|", "bar"))
        previousIsCtrl = ctrl != null
      }
    }
  }

  private fun ctrlKey(part: String): String? {
    if (part.length != 5 || !part.startsWith("<C-", ignoreCase = true) || !part.endsWith('>')) return null
    return "CTRL-" + part[3].uppercaseChar()
  }
}
