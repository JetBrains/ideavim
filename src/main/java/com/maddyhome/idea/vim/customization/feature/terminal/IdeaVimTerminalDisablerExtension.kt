/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.customization.feature.terminal

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.key.IdeaVimDisablerExtensionPoint
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils.isAlternateBufferEditor
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils.isAlternateBufferModelEditor
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils.isOutputEditor
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils.isOutputModelEditor
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils.isPromptEditor

/**
 * The only implementation is defined right here.
 */
internal class IdeaVimTerminalDisablerExtension : IdeaVimDisablerExtensionPoint {
  override fun isDisabledForEditor(editor: Editor): Boolean {
    return editor.isPromptEditor || editor.isOutputEditor || editor.isAlternateBufferEditor
      || editor.isOutputModelEditor || editor.isAlternateBufferModelEditor
  }
}
