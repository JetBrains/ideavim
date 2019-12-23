package com.maddyhome.idea.vim.common;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;

public class IndentConfig {

  private final int indentSize;
  private final int tabSize;
  private final boolean useTabs;

  private IndentConfig(CommonCodeStyleSettings.IndentOptions indentOptions) {
    this.indentSize = indentOptions.INDENT_SIZE;
    this.tabSize = indentOptions.TAB_SIZE;
    this.useTabs = indentOptions.USE_TAB_CHARACTER;
  }

  public static IndentConfig create(Editor editor) {
    return create(editor, editor.getProject());
  }

  public static IndentConfig create(Editor editor, DataContext context) {
    return create(editor, PlatformDataKeys.PROJECT.getData(context));
  }

  public static IndentConfig create(Editor editor, Project project) {
    CommonCodeStyleSettings.IndentOptions indentOptions;

    if(project != null) {
      indentOptions = CodeStyle.getIndentOptions(project, editor.getDocument());
    } else {
      indentOptions = CodeStyle.getDefaultSettings().getIndentOptions();
    }

    return new IndentConfig(indentOptions);
  }

  public int getIndentSize() {
    return indentSize;
  }

  public int getTabSize() {
    return tabSize;
  }

  public boolean isUseTabs() {
    return useTabs;
  }

  public int getTotalIndent(int count) {
    return indentSize * count;
  }

  public String createIndentByCount(int count) {
    return createIndentBySize(getTotalIndent(count));
  }

  public String createIndentBySize(int size) {
    final int tabCount;
    final int spaceCount;
    if (useTabs) {
      tabCount = size / tabSize;
      spaceCount = size % tabSize;
    }
    else {
      tabCount = 0;
      spaceCount = size;
    }
    return StringUtil.repeat("\t", tabCount) + StringUtil.repeat(" ", spaceCount);
  }
}
