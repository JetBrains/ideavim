/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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

