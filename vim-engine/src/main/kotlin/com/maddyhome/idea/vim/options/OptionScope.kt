/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.api.VimEditor

public sealed class OptionScope {
  public object GLOBAL : OptionScope()
  public class LOCAL(public val editor: VimEditor) : OptionScope()
}
