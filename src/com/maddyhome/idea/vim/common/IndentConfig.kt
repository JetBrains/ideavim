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
        // [VERSION UPDATE] 191+
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        CodeStyle.getDefaultSettings().indentOptions!!
      }
      return IndentConfig(indentOptions)
    }
  }
}