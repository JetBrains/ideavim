/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.helper.VimLockLabel
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.awt.datatransfer.Transferable

/** Text published to the PRIMARY selection, paired with the selection type PRIMARY itself cannot store. */
data class OwnedPrimaryContent(val copiedText: VimCopiedText, val selectionType: SelectionType)

/**
 * Interface representing a clipboard manager for the Vim text editor.
 * Vim supports two types of clipboards (or selections):
 * - **Primary**: This is a concept specific to Linux. It stores the most recently visually selected text and pastes its content on a middle mouse click.
 * - **Clipboard**: This is supported by all operating systems. It functions as a storage for the common 'copy and paste' operations typically done with Ctrl-C and Ctrl-V.
 */
interface VimClipboardManager {
  fun getPrimaryContent(editor: VimEditor, context: ExecutionContext): VimCopiedText?

  fun getClipboardContent(editor: VimEditor, context: ExecutionContext): VimCopiedText?

  fun setClipboardContent(editor: VimEditor, context: ExecutionContext, textData: VimCopiedText): Boolean

  /**
   * Publishes [textData] to PRIMARY. [selectionType] is remembered alongside it, because the windowing
   * system stores text only and cannot carry it — see [getOwnedPrimaryContent].
   */
  fun setPrimaryContent(
    editor: VimEditor,
    context: ExecutionContext,
    textData: VimCopiedText,
    selectionType: SelectionType,
  ): Boolean

  /**
   * What we last published via [setPrimaryContent], or `null` once anything else has claimed PRIMARY.
   *
   * A register read back out of PRIMARY has to re-guess its selection type from the text, which turns
   * line-wise into character-wise whenever the text is reformatted or re-published on the way. While
   * the selection is still ours, what we published is by definition what it holds — and we still know
   * its type. Neovim's clipboard provider works the same way, reusing the `[lines, regtype]` it last
   * handed out for as long as the `xclip`/`wl-copy` job it spawned is alive.
   *
   * This deliberately reports what was *published*, not what any register happens to cache: several
   * call sites write PRIMARY without updating the `*` register, and conflating the two hands back
   * content the selection never held.
   *
   * Default: `null` — platforms without a PRIMARY selection never own one.
   */
  fun getOwnedPrimaryContent(): OwnedPrimaryContent? = null

  /**
   * Fired when the user's visible visual selection changes. Whether to republish the new
   * selection to the windowing system's PRIMARY is platform policy, so vim-engine just notifies
   * and the IDE-specific implementation decides what (if anything) to do.
   *
   * Default: no-op.
   */
  fun onVisualSelectionChange(editor: VimEditor, caret: ImmutableVimCaret) {}

  @Deprecated("Please use com.maddyhome.idea.vim.api.VimClipboardManager#setClipboardContent")
  fun setClipboardText(text: String, rawText: String = text, transferableData: List<Any>): Transferable?

  fun collectCopiedText(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    text: String = editor.getText(range),
  ): VimCopiedText

  fun dumbCopiedText(text: String): VimCopiedText // TODO this method is NOT preffered, it does not collect transferableData

  @VimLockLabel.RequiresReadLock
  fun getTransferableData(vimEditor: VimEditor, textRange: TextRange): List<Any>

  fun preprocessText(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String,
    transferableData: List<*>,
  ): String
}

