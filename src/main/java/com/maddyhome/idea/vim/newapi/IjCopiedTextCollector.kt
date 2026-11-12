/*
 * Copyright 2003-2026 The IdeaVim authors
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
import com.intellij.openapi.editor.CaretStateTransferableData
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData
import com.intellij.openapi.editor.richcopy.view.RtfTransferableData
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiDocumentManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.HeadlessException
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

/**
 * The IDE metadata of copied text: what the `CopyPastePostProcessor`s collect on a copy (e.g. the references
 * `ideaput` uses for imports) and find again in a transferable, and the text a `CopyPastePreProcessor` makes of a copy.
 * No clipboard is read or written here.
 */
internal object IjCopiedTextCollector {
  private val logger = vimLogger<IjCopiedTextCollector>()

  /** The text of [range] with the metadata the IDE collects on a copy. */
  fun collect(editor: VimEditor, range: TextRange, text: String): IjVimCopiedText {
    val transferableData = getTransferableData(editor, range)
    val preprocessedText = preprocessText(editor, range, text, transferableData)
    return IjVimCopiedText(preprocessedText, transferableData)
  }

  /** The text of [transferable] with the metadata the IDE finds in it, or null if it has no text. */
  fun fromTransferable(transferable: Transferable): IjVimCopiedText? {
    var text: String? = null
    var transferableData: List<TextBlockTransferableData> = emptyList()
    try {
      text = transferable.getTransferData(DataFlavor.stringFlavor).toString()
      transferableData = extractTransferableData(transferable)
    } catch (_: HeadlessException) {
    } catch (_: UnsupportedFlavorException) {
    } catch (_: IOException) {
    }
    return text?.let { IjVimCopiedText(it, transferableData) }
  }

  fun getTransferableData(vimEditor: VimEditor, textRange: TextRange): List<TextBlockTransferableData> {
    val editor = vimEditor.ij
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
  fun preprocessText(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String,
    transferableData: List<*>,
  ): String {
    val editor = vimEditor.ij
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

  private fun extractTransferableData(transferable: Transferable): List<TextBlockTransferableData> {
    val allValues: MutableList<TextBlockTransferableData> = ArrayList()
    for (processor in CopyPastePostProcessor.EP_NAME.extensionList) {
      val data = processor.extractTransferableData(transferable)
      if (data.isNotEmpty()) {
        allValues.addAll(data)
      }
    }
    return allValues
  }
}

data class IjVimCopiedText(override val text: String, val transferableData: List<Any>) : VimCopiedText {
  override fun updateText(newText: String): VimCopiedText = IjVimCopiedText(newText, transferableData)
}

/**
 * Builds the `TextBlockTransferable` for clipboard and PRIMARY writes. We pin a single-range
 * `CaretStateTransferableData` so IntelliJ doesn't reshape the pasted text to match whatever
 * multi-caret arrangement the destination editor has.
 *
 * Throws [HeadlessException] on a headless JVM; callers handle.
 */
@Suppress("UNCHECKED_CAST")
internal fun buildIjTextTransferable(
  text: String,
  rawText: String,
  transferableData: List<Any>,
): TextBlockTransferable {
  val mutableData = (transferableData as List<TextBlockTransferableData>).toMutableList()
  val normalized = TextBlockTransferable.convertLineSeparators(text, "\n", mutableData)
  if (mutableData.none { it is CaretStateTransferableData }) {
    mutableData += CaretStateTransferableData(intArrayOf(0), intArrayOf(normalized.length))
  }
  return TextBlockTransferable(normalized, mutableData, RawText(rawText))
}
