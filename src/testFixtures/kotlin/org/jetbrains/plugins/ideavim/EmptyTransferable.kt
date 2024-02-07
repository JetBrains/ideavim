/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

internal object EmptyTransferable : Transferable {
  override fun getTransferDataFlavors(): Array<DataFlavor> = emptyArray()

  override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
    return false
  }

  override fun getTransferData(flavor: DataFlavor?): Any {
    throw UnsupportedFlavorException(flavor)
  }
}
