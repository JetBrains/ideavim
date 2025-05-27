/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.thin.api

import com.intellij.vim.api.VimPluginApi
import com.intellij.vim.api.scopes.vim.VimScope
import com.intellij.vim.api.scopes.vimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.VimExtension

interface VimPluginBase : VimExtension {
  override fun init() {
    val vimPluginApi: VimPluginApi = VimPluginApiImpl()
    val dummyEditor: VimEditor = injector.fallbackWindow
    val context: ExecutionContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    vimScope(dummyEditor, context, vimPluginApi) {
      init()
    }
  }

  fun VimScope.init()
}