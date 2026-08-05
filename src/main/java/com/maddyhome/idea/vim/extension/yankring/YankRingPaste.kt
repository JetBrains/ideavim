/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.getVariable
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.scopes.editor.ReadScope

/** A caret position that survives the document being rebuilt by undo. */
internal data class CaretPosition(val line: Int, val column: Int)

/**
 * A paste as the user asked for it, kept complete enough to be run a second time.
 *
 * Replacing undoes the paste and repeats it, so everything the first run depended on has to travel
 * with it: [command] because `p`, `gP` and `]p` each put the text somewhere different, [count]
 * because replacing three copies with one would be wrong, [fromVisual] because a paste over a
 * selection has to reselect it first, and [startedFrom] because `p` / `P` insert relative to the
 * caret and undo does not reliably leave the caret where the paste began.
 */
internal data class Paste(
  val command: String,
  val count: Int,
  val fromVisual: Boolean,
  val startedFrom: CaretPosition,
)

/**
 * Runs a real paste and records where it put its text, so that it can be replaced afterwards.
 */
internal suspend fun VimApi.pasteAndRemember(pasteCommand: String, fromVisual: Boolean = false) {
  val paste = Paste(pasteCommand, typedCount(), fromVisual, caretPosition())
  normal(keystrokesFor(paste))

  val pasted = editor { read { pastedText() } }
  if (pasted == null) {
    LastPaste.clear()
    return
  }
  rememberPaste(paste, YankRing.cycleStartIndex(pasted))
}

private fun VimApi.keystrokesFor(paste: Paste): String {
  // <Esc> first for the same reason as in undoAndRepaste: the count the user typed is still pending
  // in the key handler that `normal` feeds, so "3p" on top of a pending 3 would paste 33 times.
  // It drops the visual selection too, so reselect with `gv` before pasting over it.
  val reselect = if (paste.fromVisual) "gv" else ""
  return "<Esc>$reselect${requestedRegisterPrefix()}${paste.count}${paste.command}"
}

/**
 * The text the last paste inserted, or null when it inserted nothing we can find again.
 */
private fun ReadScope.pastedText(): String? {
  val marks = withPrimaryCaret { changeMarks } as? Range.Simple ?: return null
  return text.substring(marks.start, marks.end.coerceAtMost(text.length))
}

/**
 * The keys that put the register the user asked for back in front of the paste.
 *
 * The paste keys are mapped, so the register is no longer part of the key sequence by the time we
 * get here - it has to be read back out of `v:register`.
 */
private fun VimApi.requestedRegisterPrefix(): String {
  val register = getVariable<String>("v:register").orEmpty()
  if (register.isEmpty() || register == UNNAMED_REGISTER.toString()) return ""
  return "\"$register"
}

/** The count typed in front of the mapping, which the mapping has to apply itself. */
internal fun VimApi.typedCount(): Int = getVariable<Int>("v:count1") ?: 1

private suspend fun VimApi.caretPosition(): CaretPosition =
  editor {
    read {
      withPrimaryCaret { CaretPosition(line.number, offset - line.start) }
    }
  }
