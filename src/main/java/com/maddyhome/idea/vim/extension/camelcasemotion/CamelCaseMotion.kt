/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.camelcasemotion

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.VimInitApi
import com.intellij.vim.api.getVariable
import com.intellij.vim.api.scopes.TextObjectRange
import com.intellij.vim.api.scopes.TextObjectScope
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import com.maddyhome.idea.vim.state.mode.Mode

/**
 * Port of Ingo Karkat's CamelCaseMotion plugin.
 *
 * Defines `w`, `b`, `e`, `ge` style motions plus inner-"word" text objects that move/select by
 * CamelCase and underscore_notation boundaries rather than by whole words.
 *
 * Registered through the classic [VimExtension] interface (so it is enabled with `set
 * CamelCaseMotion` / `Plug 'CamelCaseMotion'`). Mappings are only created when the user sets
 * `g:camelcasemotion_key` (matching upstream); the `<Plug>` targets are always available.
 *
 * Architecture:
 *  - The `w`/`b`/`e`/`ge` motions are real [MotionActionHandler]s. This lets `e`/`ge` declare
 *    themselves [MotionType.INCLUSIVE] so they cover the landing character under an operator in both
 *    directions (`d,e`, `d,ge`) — something a plain caret move cannot express. `w`/`b` are
 *    [MotionType.EXCLUSIVE], like their Vim counterparts.
 *  - The inner objects are registered as text objects via [TextObjectScope.register], returning a
 *    range instead of scripting visual mode the way the original Vimscript had to.
 *  - Word membership honours the buffer's `'iskeyword'` option, so e.g. `set iskeyword+=$` makes `$`
 *    part of a word.
 *
 * See VIM-1984 for the tracking issue.
 */
internal class CamelCaseMotion : VimExtension {
  override fun getName(): String = "CamelCaseMotion"

  override fun init(initApi: VimInitApi) {
    val key: String? = initApi.getVariable<String>("g:camelcasemotion_key")

    for (motion in MOTIONS) {
      val plug = injector.parser.parseKeys("<Plug>CamelCaseMotion_$motion")
      VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, plug, owner, CamelMotionHandler(motion), false)
      if (key != null) {
        VimExtensionFacade.putKeyMappingIfMissing(
          MappingMode.NXO,
          injector.parser.parseKeys(key + motion),
          owner,
          plug,
          true
        )
      }
    }

    initApi.textObjects {
      for (motion in MOTIONS) {
        registerInnerObject(key, motion)
      }
    }
  }
}

private val MOTIONS: List<String> = listOf("w", "b", "e", "ge")

/** Whether a character counts as a "word" character, per the buffer's `'iskeyword'` option. */
private typealias KeywordTest = (Char) -> Boolean

private fun keywordTestFor(editor: VimEditor): KeywordTest = { ch -> KeywordOptionHelper.isKeyword(editor, ch) }

/** `e`/`ge` land on the *last* character of a word, so under an operator they are inclusive; `w`/`b` are exclusive. */
private fun isEndMotion(motion: String): Boolean = motion == "e" || motion == "ge"

/**
 * Drives a CamelCase [motion]. In normal/visual mode it simply moves each caret; in operator-pending
 * mode it hands the operator a [CamelMotionAction] so the motion's inclusive/exclusive type is honoured.
 */
private class CamelMotionHandler(private val motion: String) : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    if (editor.mode is Mode.OP_PENDING) {
      // Hand the operator a real motion so it applies the count and the inclusive/exclusive type.
      val commandBuilder = KeyHandler.getInstance().keyHandlerState.commandBuilder
      commandBuilder.addAction(CamelMotionAction(motion))
    } else {
      val text = editor.text()
      val isKeyword = keywordTestFor(editor)
      val count = operatorArguments.count1
      editor.sortedCarets().forEach { caret ->
        val target = nextCamelBoundary(text, caret.offset, motion, count, isKeyword)
        if (target != null) caret.moveToOffset(target)
      }
    }
  }
}

/** The operator-pending half of a CamelCase motion: an inclusive (`e`/`ge`) or exclusive (`w`/`b`) motion. */
private class CamelMotionAction(private val motion: String) : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType =
    if (isEndMotion(motion)) MotionType.INCLUSIVE else MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val target = nextCamelBoundary(editor.text(), caret.offset, motion, operatorArguments.count1, keywordTestFor(editor))
    return (target ?: -1).toMotionOrError()
  }
}

/**
 * Register the inner-"word" text object for `i<key><motion>` as a real text object.
 *
 * [TextObjectScope.register] creates the `<Plug>(CamelCaseMotion-i<key><motion>)` mapping in visual
 * and operator-pending modes (select mode is intentionally not mapped, matching upstream) and, when
 * a key is configured, binds the keys to it. The returned [TextObjectRange] is what makes operators,
 * counts and visual extension work without replaying the original plugin's visual-mode dance.
 */
private fun TextObjectScope.registerInnerObject(key: String?, motion: String) {
  register(
    keys = "i" + (key ?: "") + motion,
    registerDefaultMapping = key != null,
    preserveSelectionAnchor = true, // word objects extend the selection, like Vim's `iw`/`aw`
  ) { count ->
    innerCamelRange(motion, count)
  }
}

/**
 * Compute the inner-"word" range under the primary caret for the given [motion] and [count].
 */
private suspend fun VimApi.innerCamelRange(motion: String, count: Int): TextObjectRange? {
  val focused = injector.editorGroup.getFocusedEditor()
  val isKeyword: KeywordTest = if (focused != null) keywordTestFor(focused) else { _ -> false }
  return editor {
    read {
      val contents = text
      val caretOffset = withPrimaryCaret { offset }
      computeInnerRange(contents, caretOffset, motion, count, isKeyword)
    }
  }
}

// --- boundary detection -----------------------------------------------------------------------

/**
 * Character categories used to detect "word" boundaries. CamelCase boundaries arise from case
 * transitions, underscore_notation from [DELIM] characters, and digits form their own runs.
 */
private enum class CharClass { UPPER, LOWER, DIGIT, DELIM, SPACE, PUNCT }

private fun classOf(c: Char, isKeyword: KeywordTest): CharClass = when {
  c == '_' || c == '-' -> CharClass.DELIM
  c.isWhitespace() -> CharClass.SPACE
  c.isDigit() -> CharClass.DIGIT
  c.isUpperCase() -> CharClass.UPPER
  c.isLowerCase() -> CharClass.LOWER
  isKeyword(c) -> CharClass.LOWER // a char added to 'iskeyword' (e.g. `$`) acts as a word char
  else -> CharClass.PUNCT
}

/** `scan` predicate matching the start of a "word" (`w`/`b` landing spots), bound to this [isKeyword]. */
private fun wordStartScanner(isKeyword: KeywordTest): (CharSequence, Int) -> Boolean =
  { text, i -> isSubWordStart(text, i, isKeyword) }

/** `scan` predicate matching the end of a "word" (`e`/`ge` landing spots), bound to this [isKeyword]. */
private fun wordEndScanner(isKeyword: KeywordTest): (CharSequence, Int) -> Boolean =
  { text, i -> isSubWordEnd(text, i, isKeyword) }

/** True if a "word" begins at [i] — the landing spot for `w`/`b`. Mirrors the `forward_to_next` branches. */
private fun isSubWordStart(text: CharSequence, i: Int, isKeyword: KeywordTest): Boolean {
  if (i < 0 || i >= text.length) return false
  val cur = classOf(text[i], isKeyword)
  if (cur == CharClass.SPACE || cur == CharClass.DELIM) return false
  if (i == 0) return true
  val prev = classOf(text[i - 1], isKeyword)
  if (cur == CharClass.PUNCT) return prev != CharClass.PUNCT // start of a non-keyword run
  // cur is alphanumeric from here on
  return when {
    prev == CharClass.SPACE || prev == CharClass.DELIM || prev == CharClass.PUNCT -> true
    prev == CharClass.LOWER && cur == CharClass.UPPER -> true // camelCase
    prev == CharClass.DIGIT && cur != CharClass.DIGIT -> true // 123Test / 123test
    (prev == CharClass.UPPER || prev == CharClass.LOWER) && cur == CharClass.DIGIT -> true // word123
    // ALLCAPS run: only a boundary when this upper starts a CamelCase word (next char is lower)
    prev == CharClass.UPPER && cur == CharClass.UPPER -> classOf(text.getOrElse(i + 1) { ' ' }, isKeyword) == CharClass.LOWER
    else -> false
  }
}

/** True if a "word" ends at [i] — the landing spot for `e`/`ge`. Mirrors the `forward_to_end` branches. */
private fun isSubWordEnd(text: CharSequence, i: Int, isKeyword: KeywordTest): Boolean {
  if (i < 0 || i >= text.length) return false
  val cur = classOf(text[i], isKeyword)
  if (cur == CharClass.SPACE || cur == CharClass.DELIM) return false
  if (i == text.length - 1) return true
  val next = classOf(text[i + 1], isKeyword)
  if (cur == CharClass.PUNCT) return next != CharClass.PUNCT
  return next == CharClass.SPACE || next == CharClass.DELIM || next == CharClass.PUNCT ||
    isSubWordStart(text, i + 1, isKeyword)
}

/**
 * Offset of the [count]-th boundary from [offset] for the given [motion]. Returns the last reachable
 * boundary if fewer than [count] exist, or null if there is none (so the caret stays put).
 */
private fun nextCamelBoundary(text: CharSequence, offset: Int, motion: String, count: Int, isKeyword: KeywordTest): Int? {
  if (text.isEmpty()) return null
  val isStart = wordStartScanner(isKeyword)
  val isEnd = wordEndScanner(isKeyword)
  var pos = offset
  var moved = false
  repeat(count) {
    val next = when (motion) {
      "w" -> scan(text, pos + 1, text.length, isStart)
      "b" -> scan(text, pos - 1, -1, isStart)
      "e" -> scan(text, pos + 1, text.length, isEnd)
      "ge" -> scan(text, pos - 1, -1, isEnd)
      else -> null
    } ?: return if (moved) pos else null
    pos = next
    moved = true
  }
  return if (moved) pos else null
}

/** Scan from [from] towards [to] (exclusive), returning the first index where [predicate] holds. */
private inline fun scan(text: CharSequence, from: Int, to: Int, predicate: (CharSequence, Int) -> Boolean): Int? {
  val step = if (to > from) 1 else -1
  var i = from
  while (i != to) {
    if (i in text.indices && predicate(text, i)) return i
    i += step
  }
  return null
}

/**
 * The inner "word" covering [offset], shaped by [motion] (the suffix of `iw`/`ib`/`ie`/`ige`):
 *  - `w`: extend forward to the start of the word after [count] words, so the trailing delimiter is
 *    included (e.g. `iw` on `camel_case` selects `camel_`).
 *  - `e`: extend forward to the end of the [count]-th word, excluding any trailing delimiter.
 *  - `b`: extend backward by [count] - 1 words, ending at the current word's end (`ib` selects the
 *    current chunk plus the preceding ones).
 *  - `ge`: from the word start, walk `ge` (previous word-end) back [count] times, then one char left,
 *    selecting from there through the word start. Verified against the plugin.
 */
private fun computeInnerRange(text: CharSequence, offset: Int, motion: String, count: Int, isKeyword: KeywordTest): TextObjectRange? {
  if (text.isEmpty()) return null
  val isStart = wordStartScanner(isKeyword)
  val isEnd = wordEndScanner(isKeyword)
  val anchor = scan(text, offset.coerceIn(text.indices), -1, isStart) ?: return null

  return when (motion) {
    "w" -> {
      var end = anchor
      repeat(count) { end = scan(text, end + 1, text.length, isStart) ?: text.length }
      TextObjectRange.CharacterWise(anchor, end)
    }

    "b" -> {
      var start = anchor
      repeat(count - 1) { start = scan(text, start - 1, -1, isStart) ?: start }
      val end = scan(text, anchor, text.length, isEnd) ?: return null
      TextObjectRange.CharacterWise(start, end + 1)
    }

    "ge" -> {
      var pos = anchor
      repeat(count) { pos = scan(text, pos - 1, -1, isEnd) ?: pos }
      TextObjectRange.CharacterWise(maxOf(0, pos - 1), anchor + 1)
    }

    else -> { // "e"
      var end = scan(text, anchor, text.length, isEnd) ?: return null
      repeat(count - 1) { end = scan(text, end + 1, text.length, isEnd) ?: end }
      TextObjectRange.CharacterWise(anchor, end + 1)
    }
  }
}
