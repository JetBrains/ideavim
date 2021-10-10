/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.common

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions

class IndentConfig private constructor(indentOptions: IndentOptions) {
  private val indentSize = indentOptions.INDENT_SIZE
  private val tabSize = indentOptions.TAB_SIZE
  private val isUseTabs = indentOptions.USE_TAB_CHARACTER

  fun getTotalIndent(count: Int): Int = indentSize * count

  fun createIndentByCount(count: Int): String = createIndentBySize(getTotalIndent(count))

  fun createIndentBySize(size: Int): String {
    val tabCount: Int
    val spaceCount: Int
    if (isUseTabs) {
      tabCount = size / tabSize
      spaceCount = size % tabSize
    } else {
      tabCount = 0
      spaceCount = size
    }
    return "\t".repeat(tabCount) + " ".repeat(spaceCount)
  }

  companion object {
    @JvmStatic
    fun create(editor: Editor, context: DataContext): IndentConfig {
      return create(editor, PlatformDataKeys.PROJECT.getData(context))
    }

    @JvmStatic
    @JvmOverloads
    fun create(editor: Editor, project: Project? = editor.project): IndentConfig {
      val indentOptions = if (project != null) {
        CodeStyle.getIndentOptions(project, editor.document)
      } else {
        CodeStyle.getDefaultSettings().indentOptions
      }
      return IndentConfig(indentOptions)
    }
  }
}
