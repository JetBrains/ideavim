/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ide

import com.intellij.openapi.extensions.ExtensionPointName

internal val clionEP = ExtensionPointName.create<ClionNovaProvider>("IdeaVIM.clionNovaProvider")

interface ClionNovaProvider {
  fun isClionNova(): Boolean
}

internal fun isClionNova(): Boolean {
  return clionEP.extensions.any { it.isClionNova() }
}
