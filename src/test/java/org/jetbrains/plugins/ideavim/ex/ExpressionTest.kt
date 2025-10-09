/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.CurlyBracesName
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import javax.swing.JTextArea

fun Expression.evaluate(vimContext: VimLContext = CommandLineVimLContext): VimDataType {
  val editor = TextComponentEditorImpl(null, JTextArea())
  val context = DataContext.EMPTY_CONTEXT
  return this.evaluate(editor.vim, context.vim, vimContext)
}

fun CurlyBracesName.evaluate(vimContext: VimLContext = CommandLineVimLContext): VimString {
  val editor = TextComponentEditorImpl(null, JTextArea())
  val context = DataContext.EMPTY_CONTEXT
  return this.evaluate(editor.vim, context.vim, vimContext)
}
