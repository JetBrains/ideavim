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
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.ui.EmptyClipboardOwner
import com.maddyhome.idea.vim.api.VimClipboardManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
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
  override fun getPrimaryTextAndTransferableData(): Pair<String, List<Any>?>? {
    val clipboard = Toolkit.getDefaultToolkit()?.systemSelection ?: return null
    val contents = clipboard.getContents(null) ?: return null
    return getTextAndTransferableData(contents)
  }

  override fun getClipboardTextAndTransferableData(): Pair<String, List<Any>?>? {
    val contents = getContents() ?: return null
    return getTextAndTransferableData(contents)
  }

  private fun getTextAndTransferableData(trans: Transferable): Pair<String, List<Any>?>? {
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

  override fun setClipboardText(text: String, rawText: String, transferableData: List<Any>): Transferable? {
    return handleTextSetting(text, rawText, transferableData) { content -> setContents(content) }
  }

  override fun setPrimaryText(text: String, rawText: String, transferableData: List<Any>): Transferable? {
    return handleTextSetting(text, rawText, transferableData) { content ->
      val clipboard = Toolkit.getDefaultToolkit()?.systemSelection ?: return@handleTextSetting null
      clipboard.setContents(content, EmptyClipboardOwner.INSTANCE)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun handleTextSetting(text: String, rawText: String, transferableData: List<Any>, setContent: (TextBlockTransferable) -> Unit?): Transferable? {
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

  override fun getTransferableData(vimEditor: VimEditor, textRange: TextRange, text: String): List<Any> {
    val editor = (vimEditor as IjVimEditor).editor
    val transferableData: MutableList<TextBlockTransferableData> = ArrayList()
    val project = editor.project ?: return emptyList()

    val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return emptyList()

    // This thing enables alternative context resolve for dumb mode.
    // Please read docs for com.intellij.openapi.project.DumbService.isAlternativeResolveEnabled
    // [VERSION UPDATE] 2023.2+ Enable alternative context back
//    DumbService.getInstance(project).withAlternativeResolveEnabled {
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
//    }
    transferableData.add(CaretStateTransferableData(intArrayOf(0), intArrayOf(text.length)))

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
