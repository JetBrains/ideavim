/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.Disposable
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.OwnedPrimaryContent
import com.maddyhome.idea.vim.api.VimClipboardManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.awt.datatransfer.Transferable

/**
 * The [VimClipboardManager] of IntelliJ. It only hands each call to the part that does the work:
 * - [IjClipboardSelection]: CLIPBOARD, through the IDE clipboard
 * - [IjPrimarySelection]: PRIMARY of X11, and whether it still holds what IdeaVim wrote
 * - [PrimaryAutoselect]: the visual selection to PRIMARY with `autoselect`
 * - [IjCopiedTextCollector]: the IDE metadata of copied text
 *
 * Disposable so the PRIMARY writer's background thread and helper process are released when the service goes away.
 * That covers plugin unload as well as shutdown: IdeaVim's extension points are all dynamic, so an un-disposed executor
 * would pin the plugin classloader across a reload.
 */
internal class IjClipboardManager(
  private val clipboard: IjClipboardSelection = IjClipboardSelection(),
  private val primary: IjPrimarySelection = IjPrimarySelection(),
  private val autoselect: PrimaryAutoselect = PrimaryAutoselect(primary),
) : VimClipboardManager, Disposable {

  override fun dispose() {
    primary.dispose()
  }

  override fun getClipboardContent(editor: VimEditor, context: ExecutionContext): VimCopiedText? = clipboard.read()

  override fun setClipboardContent(editor: VimEditor, context: ExecutionContext, textData: VimCopiedText): Boolean {
    require(textData is IjVimCopiedText)
    return IjClipboardSelection.write(textData.text, textData.text, textData.transferableData) != null
  }

  @Deprecated("Please use com.maddyhome.idea.vim.api.VimClipboardManager#setClipboardText")
  override fun setClipboardText(text: String, rawText: String, transferableData: List<Any>): Transferable? =
    IjClipboardSelection.write(text, rawText, transferableData)

  override fun getPrimaryContent(editor: VimEditor, context: ExecutionContext): IjVimCopiedText? = primary.read()

  override fun setPrimaryContent(
    editor: VimEditor,
    context: ExecutionContext,
    textData: VimCopiedText,
    selectionType: SelectionType,
  ): Boolean {
    require(textData is IjVimCopiedText)
    return primary.write(textData, selectionType)
  }

  override fun getOwnedPrimaryContent(): OwnedPrimaryContent? = primary.ownedContent()

  override fun getLastPublishedPrimaryContent(): OwnedPrimaryContent? = primary.lastPublishedContent()

  override fun onVisualSelectionChange(editor: VimEditor, caret: ImmutableVimCaret) =
    autoselect.onVisualSelectionChange(editor, caret)

  override fun collectCopiedText(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    text: String,
  ): VimCopiedText = IjCopiedTextCollector.collect(editor, range, text)

  override fun dumbCopiedText(text: String): VimCopiedText = IjVimCopiedText(text, emptyList())

  override fun getTransferableData(vimEditor: VimEditor, textRange: TextRange): List<Any> =
    IjCopiedTextCollector.getTransferableData(vimEditor, textRange)

  override fun preprocessText(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String,
    transferableData: List<*>,
  ): String = IjCopiedTextCollector.preprocessText(vimEditor, textRange, text, transferableData)
}
