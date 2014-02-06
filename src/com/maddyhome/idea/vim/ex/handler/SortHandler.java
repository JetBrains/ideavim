package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.LineRange;
import com.maddyhome.idea.vim.group.CommandGroups;

import java.util.Comparator;

public class SortHandler extends CommandHandler {
  public SortHandler() {
    super("sor", "t", RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE);
  }

  @Override
  public boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException {
    String arg = cmd.getArgument();
    boolean reverse = false;
    boolean ignoreCase = false;
    boolean number = false;
    if (arg != null && arg.trim().length() > 0) {
      number = arg.contains("n");
      reverse = arg.contains("!");
      ignoreCase = arg.contains("i");
    }

    LineRange range = cmd.getLineRange(editor, context, false);

    // Something like "30,20sort" gets converted to "20,30sort"
    if (range.getEndLine() < range.getStartLine()) {
      range = new LineRange(range.getEndLine(), range.getStartLine());
    }

    // If we don't have a range, we either have "sort", a selection, or a block
    if (range.getEndLine() - range.getStartLine() == 0) {
      // If we have a selection.
      if (editor.getSelectionModel().hasSelection()) {
        int start = editor.getSelectionModel().getSelectionStart();
        int end = editor.getSelectionModel().getSelectionEnd();

        int startLine = editor.offsetToLogicalPosition(start).line;
        int endLine = editor.offsetToLogicalPosition(end).line;

        range = new LineRange(startLine, endLine);
      }
      // If we have a block selection
      else if (editor.getSelectionModel().hasBlockSelection()) {
        range = new LineRange(editor.getSelectionModel().getBlockStart().line, editor.getSelectionModel().getBlockEnd().line);
      }
      // If we have a generic selection, i.e. "sort" entire document
      else {
        range = new LineRange(0, editor.getDocument().getLineCount() - 1);
      }
    }

    Comparator<String> lineComparator = new VimLineComparator(ignoreCase, number, reverse);

    return CommandGroups.getInstance().getChange().sortRange(editor, range, lineComparator);
  }

  private class VimLineComparator implements Comparator<String> {
    public VimLineComparator(boolean ignoreCase, boolean number, boolean reverse) {
      this.ignoreCase = ignoreCase;
      this.number = number;
      this.reverse = reverse;
    }

    @Override
    public int compare(String o1, String o2) {
      int comparison;

      if (reverse) {
        String tmp = o2;
        o2 = o1;
        o1 = tmp;
      }

      if (ignoreCase) {
        o1 = o1.toUpperCase();
        o2 = o2.toUpperCase();
      }

      if (number) {
        comparison = StringUtil.naturalCompare(o1, o2);
      }
      else {
        comparison = o1.compareTo(o2);
      }
      return comparison;
    }

    private final boolean ignoreCase;
    private final boolean number;
    private final boolean reverse;
  }
}
