/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.range;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Represents a range given by a search pattern. The pattern can be '\\/', '\\?', '\\&amp;', /{pattern}/,
 * or ?{pattern}?.  The last two can be repeated 0 or more times after any of the others.
 */
public class SearchRange extends AbstractRange {
  /**
   * Create the pattern range
   *
   * @param pattern The text of the pattern. Each subpattern must be separated by the nul character (\\u0000)
   * @param offset  The range offset
   * @param move    True if the cursor should be moved
   */
  public SearchRange(String pattern, int offset, boolean move) {
    super(offset, move);
    setPattern(pattern);
  }

  /**
   * Parses the pattern into a list of subpatterns and flags
   *
   * @param pattern The full search pattern
   */
  private void setPattern(String pattern) {
    if (logger.isDebugEnabled()) {
      logger.debug("pattern=" + pattern);
    }
    StringTokenizer tok = new StringTokenizer(pattern, "\u0000");
    while (tok.hasMoreTokens()) {
      String pat = tok.nextToken();
      switch (pat) {
        case "\\/":
          patterns.add(VimPlugin.getSearch().getLastSearch());
          flags.add(Command.FLAG_SEARCH_FWD);
          break;
        case "\\?":
          patterns.add(VimPlugin.getSearch().getLastSearch());
          flags.add(Command.FLAG_SEARCH_REV);
          break;
        case "\\&":
          patterns.add(VimPlugin.getSearch().getLastPattern());
          flags.add(Command.FLAG_SEARCH_FWD);
          break;
        default:
          if (pat.charAt(0) == '/') {
            flags.add(Command.FLAG_SEARCH_FWD);
          }
          else {
            flags.add(Command.FLAG_SEARCH_REV);
          }

          pat = pat.substring(1);
          if (pat.charAt(pat.length() - 1) == pat.charAt(0)) {
            pat = pat.substring(0, pat.length() - 1);
          }
          patterns.add(pat);
          break;
      }
    }
  }

  /**
   * Gets the line number specified by this range without regard to any offset.
   *
   * @param editor   The editor to get the line for
   * @param context  The data context
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 if the text was not found
   */
  protected int getRangeLine(@NotNull Editor editor, DataContext context, boolean lastZero) {
    // Each subsequent pattern is searched for starting in the line after the previous search match
    int line = editor.getCaretModel().getLogicalPosition().line;
    int pos = -1;
    for (int i = 0; i < patterns.size(); i++) {
      String pattern = patterns.get(i);
      int flag = flags.get(i);
      if ((flag & Command.FLAG_SEARCH_FWD) != 0 && !lastZero) {
        pos = VimPlugin.getMotion().moveCaretToLineEnd(editor, line, true);
      }
      else {
        pos = VimPlugin.getMotion().moveCaretToLineStart(editor, line);
      }

      pos = VimPlugin.getSearch().search(editor, pattern, pos, 1, flag);
      if (pos == -1) {
        break;
      }
      else {
        line = editor.offsetToLogicalPosition(pos).line;
      }
    }

    if (pos != -1) {
      return line;
    }
    else {
      return -1;
    }
  }

  @Override
  protected int getRangeLine(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                             boolean lastZero) {
    int line = caret.getLogicalPosition().line;
    int offset = -1;
    for (int i = 0; i < patterns.size(); i++) {
      final String pattern = patterns.get(i);
      final int flag = flags.get(i);

      offset = VimPlugin.getSearch().search(editor, pattern, getSearchOffset(editor, line, flag, lastZero), 1, flag);
      if (offset == -1) break;

      line = editor.offsetToLogicalPosition(offset).line;
    }

    return offset != -1 ? line : -1;
  }

  private int getSearchOffset(@NotNull Editor editor, int line, int flag, boolean lastZero) {
    if ((flag & Command.FLAG_SEARCH_FWD) != 0 && !lastZero) {
      return VimPlugin.getMotion().moveCaretToLineEnd(editor, line, true);
    }

    return VimPlugin.getMotion().moveCaretToLineStart(editor, line);
  }

  @NotNull
  public String toString() {

    return "SearchRange[" + "patterns=" + patterns + ", " + super.toString() + "]";
  }

  @NotNull
  private final List<String> patterns = new ArrayList<>();
  @NotNull
  private final List<Integer> flags = new ArrayList<>();

  private static final Logger logger = Logger.getInstance(SearchRange.class.getName());
}
