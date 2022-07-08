package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.CaretStateTransferableData
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData
import com.intellij.openapi.editor.richcopy.view.RtfTransferableData
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiDocumentManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimClipboardManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.TestClipboardModel
import com.maddyhome.idea.vim.helper.TestClipboardModel.contents
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import java.awt.HeadlessException
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

@Service
class IjClipboardManager : VimClipboardManager {
  override fun getClipboardTextAndTransferableData(): Pair<String, List<Any>?>? {
    var res: String? = null
    var transferableData: List<TextBlockTransferableData> = ArrayList()
    try {
      val trans = getContents() ?: return null
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

  @Suppress("UNCHECKED_CAST")
  override fun setClipboardText(text: String, rawText: String, transferableData: List<Any>): Any? {
    val transferableData1 = (transferableData as List<TextBlockTransferableData>).toMutableList()
    try {
      val s = TextBlockTransferable.convertLineSeparators(text, "\n", transferableData1)
      if (transferableData1.none { it is CaretStateTransferableData }) {
        // Manually add CaretStateTransferableData to avoid adjustment of copied text to multicaret
        transferableData1 += CaretStateTransferableData(intArrayOf(0), intArrayOf(s.length))
      }
      logger.debug { "Paste text with transferable data: ${transferableData1.joinToString { it.javaClass.name }}" }
      val content = TextBlockTransferable(s, transferableData1, RawText(rawText))
      setContents(content)
      return content
    } catch (ignored: HeadlessException) {
    }
    return null
  }

  override fun getTransferableData(vimEditor: VimEditor, textRange: TextRange, text: String): List<Any> {
    val editor = (vimEditor as IjVimEditor).editor
    val transferableData: MutableList<TextBlockTransferableData> = ArrayList()
    val project = editor.project ?: return ArrayList()

    val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return ArrayList()
    DumbService.getInstance(project).withAlternativeResolveEnabled {
      for (processor in CopyPastePostProcessor.EP_NAME.extensionList) {
        try {
          transferableData.addAll(
            processor.collectTransferableData(
              file,
              editor,
              textRange.startOffsets,
              textRange.endOffsets
            )
          )
        } catch (ignore: IndexNotReadyException) {
        }
      }
    }
    transferableData.add(CaretStateTransferableData(intArrayOf(0), intArrayOf(text.length)))

    // These data provided by {@link com.intellij.openapi.editor.richcopy.TextWithMarkupProcessor} doesn't work with
    //   IdeaVim and I don't see a way to fix it
    // See https://youtrack.jetbrains.com/issue/VIM-1785
    // See https://youtrack.jetbrains.com/issue/VIM-1731
    transferableData.removeIf { it: TextBlockTransferableData? -> it is RtfTransferableData || it is HtmlTransferableData }
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
      text, "\n",
      transferableData as Collection<TextBlockTransferableData?>
    )
    if (VimPlugin.getOptionService()
      .isSet(OptionScope.GLOBAL, OptionConstants.ideacopypreprocessName, OptionConstants.ideacopypreprocessName)
    ) {
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
    if (ApplicationManager.getApplication().isUnitTestMode) {
      TestClipboardModel.contents = contents
      CopyPasteManagerEx.getInstanceEx().setContents(contents)
    } else {
      CopyPasteManagerEx.getInstanceEx().setContents(contents)
    }
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

  private fun getContents(): Transferable? {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      return contents
    }
    val manager = CopyPasteManagerEx.getInstanceEx()
    return manager.contents
  }

  companion object {
    val logger = vimLogger<IjClipboardManager>()
  }
}
