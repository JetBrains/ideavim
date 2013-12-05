package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Ira Klotzko iklotzko@ltech.com
 * Date: 12/4/13
 * Time: 2:21 PM
 * 
 * Somewhat based upon: http://vimdoc.sourceforge.net/htmldoc/change.html#sorting
 * 
 * 
 * 
 */
public class SortHandler extends CommandHandler {

  private boolean reverse = false;
  private boolean ignoreCase = false;
  
  public SortHandler() {
    super("sor", "t", RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE);
  }

  @Override
  public boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException {
    int lineCount = editor.getDocument().getLineCount();
    String arg = cmd.getArgument();
    boolean unique = false;
    boolean number = false;
    if (arg != null && arg.trim().length() > 0) {
      unique = arg.contains("u");
      number = arg.contains("n");
      reverse = arg.contains("!");
      ignoreCase = arg.contains("i");
    } else { // reset in case handler object is recycled
      reverse = false;
      ignoreCase = false;
    }
    int startLine;
    int endLine;
    int ss;
    int se = 0;
    boolean hasSelection = editor.getSelectionModel().hasSelection();
    if (hasSelection) {
      ss = editor.getSelectionModel().getSelectionStart();
      se = editor.getSelectionModel().getSelectionEnd();
      startLine = editor.offsetToLogicalPosition(ss).line;
      endLine = editor.offsetToLogicalPosition(se).line;
    } else {
      startLine = 0;
      endLine = editor.getDocument().getLineCount() - 1;
    }

    
    List<OrderedEntry> linesToSort = new ArrayList<OrderedEntry>(lineCount);
    Set<String> uniqueLineSet = unique ? new HashSet<String>() : null;
    int start = 0;
    int end = 0;
    for (int i = startLine; i <= endLine; i++) {
      int lineStartIndex = editor.getDocument().getLineStartOffset(i);
      int lineEndIndex = editor.getDocument().getLineEndOffset(i);
      if (startLine - i == 0) {
        start = lineStartIndex;
      }
      if (i == lineCount - 1) {
        end = lineEndIndex;
      }
      String line = editor.getDocument().getText(new com.intellij.openapi.util.TextRange(lineStartIndex, lineEndIndex));
      if (unique) {
        if (uniqueLineSet.contains(ignoreCase ? line.toLowerCase() : line)) {
          continue;
        }
        uniqueLineSet.add(ignoreCase ? line.toLowerCase() : line);
      }
      OrderedEntry ie = new OrderedEntry();
      ie.entry = line;
      ie.order = i;
      linesToSort.add(ie);
    }
    Comparator<OrderedEntry> sorter;
    if (!number) {
      sorter = new StringSort();
    } else {
      sorter = new NumberStringSort();
    }
    // determine sorter
    Collections.sort(linesToSort, sorter);
    StringBuilder sb = new StringBuilder();
    for(OrderedEntry line: linesToSort) {
      sb.append(line.entry).append('\n');
    }
    sb.setLength(sb.length() - 1); // remove last \n
    
    if (hasSelection) {
      end = se;
    }
    
    CommandGroups.getInstance().getChange().replaceTextForSort(editor, start, end, sb.toString());
    if (hasSelection) {
      editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(startLine, 0));
    } else {
      editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(0, 0));
    }
    return true;
  }
  Pattern num = Pattern.compile("^\\D*(-?\\d+)\\D*$");
  // @todo: hex and octal sorting
  // With [x] sorting is done on the first hexadecimal number 
  // in the line (after or inside a {pattern} match).  
  // A leading "0x" or "0X" is ignored. One leading '-' is 
  // included in the number. 
  // Pattern hexNum = Pattern.compile("^\\D*(-?\\d+)\\D*$"); 
  // With [o] sorting is done on the first octal number 
  // in the line (after or inside a {pattern} match).
  // Pattern octaNum = Pattern.compile("^\\D*(-?\\d+)\\D*$");
  private class NumberStringSort implements Comparator<OrderedEntry> {
    @Override
    public int compare(OrderedEntry e1, OrderedEntry e2) {
      String o1 = reverse ? e2.entry : e1.entry;
      String o2 = reverse ? e1.entry : e2.entry;
      if (!num.matcher(o1).matches() && !num.matcher(o2).matches()) {
        int i1 = reverse ? e2.order : e1.order;
        int i2 = reverse ? e1.order : e2.order;
        if (o1.equals("\n")) return -1;
        if (o2.equals("\n")) return 1;
        return Integer.compare(i1, i2);
      }
      if (!num.matcher(o1).matches()) return -1;
      if (!num.matcher(o2).matches()) return 1;
      // first integer match
      Matcher matcher = num.matcher(o1);
      matcher.find();
      long n1 = Long.parseLong(matcher.group(1));
      matcher = num.matcher(o2);
      matcher.find();
      long n2 = Long.parseLong(matcher.group(1));
      return Long.compare(n1, n2);
    }
  }
  private class StringSort implements Comparator<OrderedEntry> {
    @Override
    public int compare(OrderedEntry e1, OrderedEntry e2) {
      String o1 = reverse ? e2.entry : e1.entry;
      String o2 = reverse ? e1.entry : e2.entry;
      return ignoreCase ?  o1.compareToIgnoreCase(o2) : o1.compareTo(o2);
    }
  }
  
  private class OrderedEntry {
    private String entry;
    private int order;
  }
  
}
