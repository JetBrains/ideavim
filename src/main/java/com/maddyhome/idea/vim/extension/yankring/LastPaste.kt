/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.models.Path
import com.intellij.vim.api.scopes.editor.ReadScope

/**
 * Where the last paste put its text, so that `<C-P>` / `<C-N>` know what to replace.
 *
 * A paste stays replaceable only while the buffer is untouched and the caret has not wandered off
 * the pasted line. We recognise an edit by the text length - Vim would use `b:changedtick`, but
 * IdeaVim does not implement that variable yet - and a move by the caret's line, the same check
 * `s:YRReplace` makes against the `'[` mark.
 */
internal object LastPaste {

  /** A caret position that survives the document being rebuilt by undo. */
  internal data class Position(val line: Int, val column: Int)

  internal data class Pending(
    val filePath: String,
    /** The key that produced the paste, `p` or `P`, so that replacing it can repeat it. */
    val pasteCommand: String,
    /** Index into [YankRing.entries] of the entry that is currently shown in the buffer. */
    val ringIndex: Int,
    val textLength: Long,
    val line: Int,
    /**
     * Where the caret sat *before* the paste ran. Replacing undoes the paste and runs it again, and
     * `p` / `P` insert relative to the caret, so the caret has to be put back deliberately - undo
     * does not reliably leave it where the paste started from.
     */
    val positionBeforePaste: Position,
  )

  private var pending: Pending? = null

  fun remember(
    filePath: String,
    pasteCommand: String,
    ringIndex: Int,
    textLength: Long,
    line: Int,
    positionBeforePaste: Position,
  ) {
    pending = Pending(filePath, pasteCommand, ringIndex, textLength, line, positionBeforePaste)
  }

  /**
   * The pending paste, or null when there is none to replace - because nothing has been pasted,
   * because the buffer has been edited since, because we are looking at a different file, or
   * because the caret has left the line the text was pasted on.
   */
  fun pending(filePath: String, textLength: Long, line: Int): Pending? =
    pending?.takeIf { it.filePath == filePath && it.textLength == textLength && it.line == line }

  fun clear() {
    pending = null
  }
}

internal suspend fun VimApi.rememberPaste(
  pasteCommand: String,
  ringIndex: Int,
  positionBeforePaste: LastPaste.Position,
) {
  editor {
    read {
      LastPaste.remember(
        filePath.identity,
        pasteCommand,
        ringIndex,
        textLength,
        caretLine(),
        positionBeforePaste,
      )
    }
  }
}

internal suspend fun VimApi.caretPosition(): LastPaste.Position =
  editor {
    read {
      withPrimaryCaret { LastPaste.Position(line.number, offset - line.start) }
    }
  }

internal suspend fun VimApi.pendingPaste(): LastPaste.Pending? =
  editor { read { LastPaste.pending(filePath.identity, textLength, caretLine()) } }

private fun ReadScope.caretLine(): Int = withPrimaryCaret { line.number }

/**
 * A comparable identity for an editor's file.
 *
 * [Path] is an anonymous object with no `equals` or `toString` of its own, so instances cannot be
 * compared directly - two reads of the same editor produce values that differ.
 */
private val Path.identity: String
  get() = protocol + "://" + path.joinToString("/")
