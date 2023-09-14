/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

@Deprecated("Use injector.optionGroup functions")
// Keep it to keep the compatibility with which-key plugin
public sealed class OptionScope {
  public object GLOBAL : OptionScope()
}
