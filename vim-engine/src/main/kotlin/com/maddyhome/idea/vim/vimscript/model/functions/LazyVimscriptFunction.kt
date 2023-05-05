/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions

import com.intellij.vim.model.LazyInstance

public class LazyVimscriptFunction(public val name: String, className: String, classLoader: ClassLoader):
  LazyInstance<FunctionHandler>(className, classLoader)