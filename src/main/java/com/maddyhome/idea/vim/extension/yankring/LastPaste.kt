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

/**
 * The buffer as it looked right after a paste, and therefore the state that paste is still
 * replaceable in.
 *
 * A paste stays replaceable only while the buffer is untouched and the caret has not wandered off
 * the pasted line. We recognise an edit by the text length - Vim would use `b:changedtick`, but
 * IdeaVim does not implement that variable yet - and a move by the caret's line, the same check
 * `s:YRReplace` makes against the `'[` mark.
 */
internal data class BufferState(val filePath: String, val textLength: Long, val caretLine: Int)

/**
 * Where the last paste put its text, so that `<C-P>` / `<C-N>` know what to replace.
 */
internal object LastPaste {

  internal data class Pending(
    val paste: Paste,
    /** Index into [YankRing.entries] of the entry that is currently shown in the buffer. */
    val ringIndex: Int,
    val buffer: BufferState,
  )

  private var pending: Pending? = null

  fun remember(paste: Paste, ringIndex: Int, buffer: BufferState) {
    pending = Pending(paste, ringIndex, buffer)
  }

  /**
   * The pending paste, or null when there is none to replace - because nothing has been pasted,
   * because the buffer has been edited since, because we are looking at a different file, or
   * because the caret has left the line the text was pasted on.
   */
  fun pendingIn(buffer: BufferState): Pending? = pending?.takeIf { it.buffer == buffer }

  fun clear() {
    pending = null
  }
}

internal suspend fun VimApi.rememberPaste(paste: Paste, ringIndex: Int) {
  LastPaste.remember(paste, ringIndex, bufferState())
}

internal suspend fun VimApi.pendingPaste(): LastPaste.Pending? = LastPaste.pendingIn(bufferState())

private suspend fun VimApi.bufferState(): BufferState =
  editor {
    read {
      BufferState(filePath.identity, textLength, withPrimaryCaret { line.number })
    }
  }

/**
 * A comparable identity for an editor's file.
 *
 * [Path] is an anonymous object with no `equals` or `toString` of its own, so instances cannot be
 * compared directly - two reads of the same editor produce values that differ.
 */
private val Path.identity: String
  get() = protocol + "://" + path.joinToString("/")
