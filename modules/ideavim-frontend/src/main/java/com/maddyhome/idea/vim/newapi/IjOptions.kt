/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.group.EffectiveIjOptions
import com.maddyhome.idea.vim.group.GlobalIjOptions
import com.maddyhome.idea.vim.group.IjVimOptionGroup

/**
 * Convenience function to get the IntelliJ implementation specific global option accessor
 */
fun VimInjector.globalIjOptions(): GlobalIjOptions = (this.optionGroup as IjVimOptionGroup).getGlobalIjOptions()

/**
 * Convenience function to get the IntelliJ implementation specific option accessor for the given editor's scope
 */
fun VimInjector.ijOptions(editor: VimEditor): EffectiveIjOptions =
  (this.optionGroup as IjVimOptionGroup).getEffectiveIjOptions(editor)
