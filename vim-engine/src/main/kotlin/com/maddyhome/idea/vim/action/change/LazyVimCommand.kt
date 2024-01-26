/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.vimscript.model.LazyInstance
import javax.swing.KeyStroke

public class LazyVimCommand(
  public val keys: Set<List<KeyStroke>>,
  public val modes: Set<MappingMode>,
  className: String,
  classLoader: ClassLoader,
) : LazyInstance<EditorActionHandlerBase>(className, classLoader) {
  public val actionId: String = EditorActionHandlerBase.getActionId(className)
}