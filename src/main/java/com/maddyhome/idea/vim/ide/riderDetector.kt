/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ide

import com.intellij.openapi.extensions.ExtensionPointName

internal val riderEP = ExtensionPointName.create<RiderProvider>("IdeaVIM.riderProvider")

internal interface RiderProvider {
  fun isRider(): Boolean
}

internal class RiderProviderImpl : RiderProvider {
  override fun isRider(): Boolean = true
}

internal fun isRider(): Boolean {
  return riderEP.extensions.any { it.isRider() }
}
