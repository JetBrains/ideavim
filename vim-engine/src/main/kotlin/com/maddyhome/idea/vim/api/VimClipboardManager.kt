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
   * system stores text only and cannot carry it. The register group remembers it, see [isPrimaryOwned].
   */
  fun setPrimaryContent(
    editor: VimEditor,
    context: ExecutionContext,
    textData: VimCopiedText,
    selectionType: SelectionType,
  ): Boolean

  /**
   * Whether PRIMARY still holds what was last written with [setPrimaryContent], as nothing else has claimed it since.
   * The register group then takes the `*` register from what it wrote, type included, without reading PRIMARY. Neovim's
   * clipboard provider does the same, reusing the `[lines, regtype]` it last handed out while its `xclip`/`wl-copy` job
   * is alive.
   *
   * Default: `false` — platforms without a PRIMARY selection never own one.
   */
  fun isPrimaryOwned(): Boolean = false

  /**
   * Not used by IdeaVim anymore: the register group remembers what it wrote to PRIMARY, and asks [isPrimaryOwned].
   *
   * Default: `null`.
   */
  fun getOwnedPrimaryContent(): OwnedPrimaryContent? = null

  /**
   * Not used by IdeaVim anymore, see [getOwnedPrimaryContent].
   *
   * Default: `null`.
   */
  fun getLastPublishedPrimaryContent(): OwnedPrimaryContent? = null

  /**
   * Not called by IdeaVim anymore: the visual selection goes to PRIMARY with `autoselect` through
   * [com.maddyhome.idea.vim.register.VimRegisterGroup.onVisualSelectionChange].
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

