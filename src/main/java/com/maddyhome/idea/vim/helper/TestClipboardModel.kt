/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.openapi.editor.RawText
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

internal object TestClipboardModel {

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
