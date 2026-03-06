/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.codeInsight.template.impl.TemplateState
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimTemplateManager
import com.maddyhome.idea.vim.api.VimTemplateState

class IjTemplateManager : VimTemplateManager {
  override fun getTemplateState(editor: VimEditor): VimTemplateState? {
    return TemplateManagerImpl.getTemplateState(editor.ij)?.let { IjTemplateState(it) }
  }
}

class IjTemplateState(val templateState: TemplateState) : VimTemplateState
