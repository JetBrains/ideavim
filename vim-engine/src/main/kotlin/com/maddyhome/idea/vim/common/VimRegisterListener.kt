/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Notified after text has been stored into a register.
 *
 * This is the Vim `TextYankPost` shaped event: it fires for yanks, deletes and changes alike,
 * because they all funnel through the same register write. Unlike [VimYankListener], which reports
 * the ranges a yank covered so they can be highlighted, this reports what was actually stored.
 *
 * The event is raised once per logical operation, not once per register write. A single yank
 * writes several registers - the named one, the unnamed one, and possibly a numbered or the small
 * delete register - and reporting each of those would look like several yanks to a listener.
 */
interface VimRegisterListener : Listener {
  /**
   * @param register   The register the operation targeted, before the copies to the unnamed and
   *                   numbered registers are made.
   * @param copiedText The stored text, together with any IDE specific transferable data.
   * @param type       Whether the text was stored character-wise, line-wise or block-wise.
   * @param isDelete   True when the text was removed from the buffer, false for a plain yank.
   */
  fun registerStored(register: Char, copiedText: VimCopiedText, type: SelectionType, isDelete: Boolean)
}
