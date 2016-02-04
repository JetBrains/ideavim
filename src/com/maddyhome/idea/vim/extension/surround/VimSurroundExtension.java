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

package com.maddyhome.idea.vim.extension.surround;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Mark;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension;
import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.key.OperatorFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.*;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * Port of vim-surround.
 *
 * See https://github.com/tpope/vim-surround
 *
 * @author dhleong
 * @author vlan
 */
public class VimSurroundExtension extends VimNonDisposableExtension {

  private static final char REGISTER = '"';

  private static final Map<Character, Pair<String, String>> SURROUND_PAIRS = ImmutableMap.<Character, Pair<String, String>>builder()
    .put('b', Pair.create("(", ")"))
    .put('(', Pair.create("( ", " )"))
    .put(')', Pair.create("(", ")"))
    .put('B', Pair.create("{", "}"))
    .put('{', Pair.create("{ ", " }"))
    .put('}', Pair.create("{", "}"))
    .put('r', Pair.create("[", "]"))
    .put('[', Pair.create("[ ", " ]"))
    .put(']', Pair.create("[", "]"))
    .put('a', Pair.create("<", ">"))
    .put('>', Pair.create("<", ">"))
    .build();

  @NotNull
  @Override
  public String getName() {
    return "surround";
  }

  @Override
  protected void initOnce() {
    putExtensionHandlerMapping(MappingMode.N, parseKeys("ys"), new YSurroundHandler(), false);
    putExtensionHandlerMapping(MappingMode.N, parseKeys("cs"), new CSurroundHandler(), false);
    putExtensionHandlerMapping(MappingMode.N, parseKeys("ds"), new DSurroundHandler(), false);
    putExtensionHandlerMapping(MappingMode.VO, parseKeys("S"), new VSurroundHandler(), false);
  }

  @Nullable
  private static Pair<String, String> getSurroundPair(char c) {
    if (SURROUND_PAIRS.containsKey(c)) {
      return SURROUND_PAIRS.get(c);
    }
    else if (!Character.isLetter(c)) {
      final String s = String.valueOf(c);
      return Pair.create(s, s);
    }
    else {
      return null;
    }
  }

  @Nullable
  private static Pair<String, String> inputTagPair(@NotNull Editor editor) {
    final String tagInput = input(editor, "<");
    if (tagInput.endsWith(">")) {
      final String tagName = tagInput.substring(0, tagInput.length() - 1);
      return Pair.create("<" + tagName + ">", "</" + tagName + ">");
    }
    else {
      return null;
    }
  }

  @Nullable
  private static Pair<String, String> getOrInputPair(char c, @NotNull Editor editor) {
    return c == '<' || c == 't' ? inputTagPair(editor) : getSurroundPair(c);
  }

  private static class YSurroundHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      setOperatorFunction(new Operator());
      executeNormal(parseKeys("g@"), editor, context);
    }
  }

  private static class VSurroundHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      final TextRange visualRange = VimPlugin.getMark().getVisualSelectionMarks(editor);
      if (visualRange == null) {
        return;
      }

      // NB: Operator ignores SelectionType anyway
      new Operator().apply(editor, context, SelectionType.CHARACTER_WISE);

      // leave visual mode
      executeNormal(parseKeys("<Esc>"), editor);

      editor.getCaretModel().moveToOffset(visualRange.getStartOffset());
    }
  }

  private static class CSurroundHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      final char charFrom = getchar(editor);
      if (charFrom == 0) {
        return;
      }

      final char charTo = getchar(editor);
      if (charTo == 0) {
        return;
      }

      Pair<String, String> newSurround = getOrInputPair(charTo, editor);
      if (newSurround == null) {
        return;
      }

      change(editor, charFrom, newSurround);
    }

    static void change(@NotNull Editor editor, char charFrom, @Nullable Pair<String, String> newSurround) {

      // we take over the " register, so preserve it
      final List<KeyStroke> oldValue = getreg(REGISTER);

      // extract the inner value
      perform("di" + pick(charFrom), editor);
      List<KeyStroke> innerValue = getreg(REGISTER);
      if (innerValue == null) {
        innerValue = new ArrayList<KeyStroke>();
      }

      // delete the surrounding
      perform("da" + pick(charFrom), editor);

      // insert the surrounding characters and paste
      if (newSurround != null) {
        innerValue.addAll(0, parseKeys(escape(newSurround.first)));
        innerValue.addAll(parseKeys(escape(newSurround.second)));
      }
      pasteSurround(innerValue, editor);

      // restore the old value
      setreg(REGISTER, oldValue);

      // jump back to start
      executeNormal(parseKeys("`["), editor);
    }

    private static String escape(String sequence) {
      return sequence.replace("<", "\\<");
    }

    /** perform an action, storing the result in our register */
    private static void perform(String sequence, Editor editor) {
      final List<KeyStroke> keys = parseKeys(
        "\"" + REGISTER + sequence
      );
      executeNormal(keys, editor);
    }

    private static void pasteSurround(List<KeyStroke> innerValue, Editor editor) {
      // this logic is direct from vim-surround
      final int offset = editor.getCaretModel().getOffset();
      final int line = editor.getDocument().getLineNumber(offset);
      final int lineEnd = editor.getDocument().getLineEndOffset(line);

      final Mark mark = VimPlugin.getMark().getMark(editor, ']');
      final int motionEndCol = mark == null ? -1 : mark.getCol();
      final String pasteCommand;
      if (motionEndCol == lineEnd && offset + 1 == lineEnd) {
        pasteCommand = "p";
      } else {
        pasteCommand = "P";
      }

      setreg(REGISTER, innerValue);
      perform(pasteCommand, editor);
    }

    private static char pick(char charFrom) {
      switch (charFrom) {
        case 'a': return '>';
        case 'r': return ']';
        default: return charFrom;
      }
    }
  }

  private static class DSurroundHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      // deleting surround is just changing the surrounding to "nothing"
      final char charFrom = getchar(editor);
      if (charFrom == 0) {
        return;
      }

      CSurroundHandler.change(editor, charFrom, null);
    }
  }

  private static class Operator implements OperatorFunction {
    @Override
    public boolean apply(@NotNull Editor editor, @NotNull DataContext context, @NotNull SelectionType selectionType) {
      final KeyStroke keyStroke = getKeyStroke(editor);
      if (keyStroke.getKeyCode() == KeyEvent.VK_ESCAPE) {
        return true;
      }
      final char c = keyStroke.getKeyChar();
      if (c == KeyEvent.CHAR_UNDEFINED) {
        return false;
      }
      final Pair<String, String> pair = getOrInputPair(c, editor);
      if (pair == null) {
        return false;
      }
      // XXX: Will it work with line-wise or block-wise selections?
      final TextRange range = getSurroundRange(editor);
      if (range == null) {
        return false;
      }
      final ChangeGroup change = VimPlugin.getChange();
      final String leftSurround = pair.getFirst();
      change.insertText(editor, range.getStartOffset(), leftSurround);
      change.insertText(editor, range.getEndOffset() + leftSurround.length(), pair.getSecond());

      // jump back to start
      executeNormal(parseKeys("`["), editor);
      return true;
    }

    @Nullable
    private TextRange getSurroundRange(@NotNull Editor editor) {
      final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
      switch (mode) {
        case COMMAND:
          return VimPlugin.getMark().getChangeMarks(editor);
        case VISUAL:
          return VimPlugin.getMark().getVisualSelectionMarks(editor);
        default:
          return null;
      }
    }
  }

}
