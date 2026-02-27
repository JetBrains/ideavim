/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.CaretStateTransferableData
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData
import com.intellij.openapi.editor.richcopy.view.RtfTransferableData
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.ui.EmptyClipboardOwner
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimClipboardManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

@Service
internal class IjClipboardManager : VimClipboardManager {
  override fun getPrimaryContent(editor: VimEditor, context: ExecutionContext): IjVimCopiedText? {
    val clipboard = Toolkit.getDefaultToolkit()?.systemSelection ?: return null
    val contents = clipboard.getContents(null) ?: return null
    val (text, transferableData) = getTextAndTransferableData(contents) ?: return null
    return IjVimCopiedText(text, transferableData ?: emptyList())
  }

  override fun getClipboardContent(editor: VimEditor, context: ExecutionContext): VimCopiedText? {
    val contents = getContents() ?: return null
    val (text, transferableData) = getTextAndTransferableData(contents) ?: return null
    return IjVimCopiedText(text, transferableData ?: emptyList())
  }

  override fun setClipboardContent(editor: VimEditor, context: ExecutionContext, textData: VimCopiedText): Boolean {
    require(textData is IjVimCopiedText)
    return handleTextSetting(
      textData.text,
      textData.text,
      textData.transferableData
    ) { content -> setContents(content) } != null
  }

  // TODO prefer methods with ranges, because they collect and preprocess for us
//  override fun createClipboardEntry(
//    editor: VimEditor,
//    context: ExecutionContext,
//    text: String,
//    range: TextRange,
//  ): ClipboardEntry {
//    val transferableData = getTransferableData(editor, range, text)
//    val preprocessedText = preprocessText(editor, range, text, transferableData)
//    return IJClipboardEntry(preprocessedText, text, transferableData)
//  }

//  override fun setClipboardText(editor: VimEditor, context: ExecutionContext, entry: ClipboardEntry): Boolean {
//    require(entry is IJClipboardEntry)
//    return setClipboardText(entry.text, entry.rawText, entry.transferableData) != null
//  }

  private fun getTextAndTransferableData(trans: Transferable): Pair<String, List<TextBlockTransferableData>?>? {
    var res: String? = null
    var transferableData: List<TextBlockTransferableData> = ArrayList()
    try {
      val data = trans.getTransferData(DataFlavor.stringFlavor)
      res = data.toString()
      transferableData = collectTransferableData(trans)
    } catch (ignored: HeadlessException) {
    } catch (ignored: UnsupportedFlavorException) {
    } catch (ignored: IOException) {
    }
    if (res == null) return null
    return Pair(res, transferableData)
  }

  @Deprecated("Please use com.maddyhome.idea.vim.api.VimClipboardManager#setClipboardText")
  override fun setClipboardText(text: String, rawText: String, transferableData: List<Any>): Transferable? {
    return handleTextSetting(text, rawText, transferableData) { content -> setContents(content) }
  }

  override fun setPrimaryContent(
    editor: VimEditor,
    context: ExecutionContext,
    textData: VimCopiedText,
  ): Boolean {
    require(textData is IjVimCopiedText)
    return handleTextSetting(textData.text, textData.text, textData.transferableData) { content ->
      val clipboard = Toolkit.getDefaultToolkit()?.systemSelection ?: return@handleTextSetting null
      clipboard.setContents(content, EmptyClipboardOwner.INSTANCE)
    } != null
  }

//  override fun setPrimaryText(editor: VimEditor, context: ExecutionContext, entry: ClipboardEntry): Boolean {
//    require(entry is IJClipboardEntry)
//    return setPrimaryText(entry.text, entry.rawText, entry.transferableData) != null
//  }

  override fun collectCopiedText(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    text: String,
  ): VimCopiedText {
    val transferableData = getTransferableData(editor, range)
    val preprocessedText = preprocessText(editor, range, text, transferableData)
    return IjVimCopiedText(preprocessedText, transferableData)
  }

  override fun dumbCopiedText(text: String): VimCopiedText {
    return IjVimCopiedText(text, emptyList())
  }

  @Suppress("UNCHECKED_CAST")
  private fun handleTextSetting(
    text: String,
    rawText: String,
    transferableData: List<Any>,
    setContent: (TextBlockTransferable) -> Unit?,
  ): Transferable? {
    val mutableTransferableData = (transferableData as List<TextBlockTransferableData>).toMutableList()
    try {
      val s = TextBlockTransferable.convertLineSeparators(text, "\n", mutableTransferableData)
      if (mutableTransferableData.none { it is CaretStateTransferableData }) {
        // Manually add CaretStateTransferableData to avoid adjustment of a copied text to multicaret
        mutableTransferableData += CaretStateTransferableData(intArrayOf(0), intArrayOf(s.length))
      }
      logger.debug { "Paste text with transferable data: ${mutableTransferableData.joinToString { it.javaClass.name }}" }
      val content = TextBlockTransferable(s, mutableTransferableData, RawText(rawText))
      setContent(content)
      return content
    } catch (ignored: HeadlessException) {
    }
    return null
  }

  override fun getTransferableData(vimEditor: VimEditor, textRange: TextRange): List<TextBlockTransferableData> {
    val editor = (vimEditor as IjVimEditor).editor
    val transferableData: MutableList<TextBlockTransferableData> = ArrayList()
    val project = editor.project ?: return emptyList()

    val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return emptyList()

    // This thing enables alternative context resolve for dumb mode.
    // Please read docs for com.intellij.openapi.project.DumbService.isAlternativeResolveEnabled
    DumbService.getInstance(project).withAlternativeResolveEnabled {
      for (processor in CopyPastePostProcessor.EP_NAME.extensionList) {
        try {
          logger.debug { "Copy paste processor: ${processor.javaClass.name}" }
          transferableData.addAll(
            processor.collectTransferableData(
              file,
              editor,
              textRange.startOffsets,
              textRange.endOffsets,
            ),
          )
        } catch (ignore: IndexNotReadyException) {
        }
      }
    }
    transferableData.add(
      CaretStateTransferableData(
        intArrayOf(0),
        intArrayOf(textRange.endOffset - textRange.startOffset)
      )
    )

    // These data provided by {@link com.intellij.openapi.editor.richcopy.TextWithMarkupProcessor} doesn't work with
    //   IdeaVim and I don't see a way to fix it
    // See https://youtrack.jetbrains.com/issue/VIM-1785
    // See https://youtrack.jetbrains.com/issue/VIM-1731
    transferableData.removeIf { it: TextBlockTransferableData? -> it is RtfTransferableData || it is HtmlTransferableData }
    logger.debug { "Transferable data collected: ${transferableData.joinToString { it.javaClass.name }}" }
    return transferableData
  }

  @Suppress("UNCHECKED_CAST")
  override fun preprocessText(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String,
    transferableData: List<*>,
  ): String {
    val editor = (vimEditor as IjVimEditor).editor
    val project = editor.project ?: return text
    val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return text
    val rawText = TextBlockTransferable.convertLineSeparators(
      text,
      "\n",
      transferableData as Collection<TextBlockTransferableData?>,
    )
    if (injector.ijOptions(vimEditor).ideacopypreprocess) {
      for (processor in CopyPastePreProcessor.EP_NAME.extensionList) {
        val escapedText = processor.preprocessOnCopy(file, textRange.startOffsets, textRange.endOffsets, rawText)
        if (escapedText != null) {
          return escapedText
        }
      }
    }
    return text
  }

  private fun setContents(contents: Transferable) {
    CopyPasteManagerEx.getInstanceEx().setContents(contents)
  }

  private fun collectTransferableData(transferable: Transferable): List<TextBlockTransferableData> {
    val allValues: MutableList<TextBlockTransferableData> = ArrayList()
    for (processor in CopyPastePostProcessor.EP_NAME.extensionList) {
      val data = processor.extractTransferableData(transferable)
      if (data.isNotEmpty()) {
        allValues.addAll(data)
      }
    }
    return allValues
  }

  private fun getContents(): Transferable? = CopyPasteManagerEx.getInstanceEx().contents

  companion object {
    val logger = vimLogger<IjClipboardManager>()
  }
}

data class IjVimCopiedText(override val text: String, val transferableData: List<Any>) : VimCopiedText {
  override fun updateText(newText: String): VimCopiedText = IjVimCopiedText(newText, transferableData)
}
