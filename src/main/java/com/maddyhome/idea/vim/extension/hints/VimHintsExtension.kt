/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.maddyhome.idea.vim.extension.VimExtension

internal class VimHintsExtension : VimExtension {
  override fun getName() = "vimhints"
  override fun init() { /* no-op — ToggleHintsAction checks the option dynamically */
  }
}
