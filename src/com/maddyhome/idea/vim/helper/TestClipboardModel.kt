package com.maddyhome.idea.vim.helper

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.openapi.editor.RawText
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

object TestClipboardModel {

  var contents: Transferable = EmptyTransferable

  fun setClipboardText(text: String) {
    contents = TextBlockTransferable(text, emptyList(), RawText(text))
  }

  fun clearClipboard() {
    contents = EmptyTransferable
  }

  private object EmptyTransferable : Transferable {
    override fun getTransferData(flavor: DataFlavor): Any {
      throw UnsupportedFlavorException(flavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = false
    override fun getTransferDataFlavors(): Array<DataFlavor> = emptyArray()
  }
}

