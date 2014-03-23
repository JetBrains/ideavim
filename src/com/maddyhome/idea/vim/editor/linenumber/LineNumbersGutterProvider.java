package com.maddyhome.idea.vim.editor.linenumber;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.option.LineNumberOption;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.option.RelativeLineNumberOption;
import org.jetbrains.annotations.Nullable;

/**
 * Gutter provider that sets line numbers depending on the 'nu' and 'rnu' options.
 *
 * If none of these options are set, nothing is shown in the gutter.
 *
 * If only the 'nu' option is set, the line number is shown in the gutter.
 *
 * If only the 'rnu' option is set, the line number relative to the
 * current caret position is shown in the gutter.
 *
 * If both the 'nu' and 'rnu' options are set, the line number relative to the
 * current caret position is shown in the gutter, except for the line where the
 * gutter is positioned, which will show the line number.
 */
public class LineNumbersGutterProvider extends NullGutterProvider {

  @Nullable
  @Override
  public String getLineText(int line, Editor editor) {
    if (!VimPlugin.isEnabled()) {
      return null;
    }

    boolean lineNumberOptSet = isLineNumberOptionSet();
    boolean relativeLineNumberOptSet = isRelativeLineNumberOptionSet();

    if (!lineNumberOptSet && !relativeLineNumberOptSet) {
      return null;
    }

    if (lineNumberOptSet && relativeLineNumberOptSet) {
      return isCaretInLine(line, editor)
        ? getLineNumberText(line)
        : getRelativeLineNumberText(line, editor);
    }

    return relativeLineNumberOptSet
      ? getRelativeLineNumberText(line, editor)
      : getLineNumberText(line);
  }

  private boolean isRelativeLineNumberOptionSet() {
    return Options.getInstance().isSet(RelativeLineNumberOption.NAME);
  }

  private boolean isLineNumberOptionSet() {
    return Options.getInstance().isSet(LineNumberOption.NAME);
  }

  private boolean isCaretInLine(int line, Editor editor) {
    int caretLine = getCaretLine(editor);
    return caretLine - line == 0;
  }

  private String getLineNumberText(int line) {
    return "" + (line + 1);
  }

  private String getRelativeLineNumberText(int line, Editor editor) {
    int caretLine = getCaretLine(editor);
    return "" + Math.abs(caretLine - line);
  }

  private int getCaretLine(Editor editor) {
    return editor.getCaretModel().getLogicalPosition().line;
  }
}
