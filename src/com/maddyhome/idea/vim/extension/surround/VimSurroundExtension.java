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
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension;
import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.key.OperatorFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Map;

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
  }

  private static class YSurroundHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      setOperatorFunction(new Operator());
      executeNormal(parseKeys("g@"), editor, context);
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
      final Pair<String, String> pair = c == '<' || c == 't' ? inputTagPair(editor) : getSurroundPair(c);
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
      // XXX: Should we move the caret to start offset?
      return true;
    }

    @Nullable
    private TextRange getSurroundRange(@NotNull Editor editor) {
      final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
      switch (mode) {
        case COMMAND:
          return VimPlugin.getMark().getChangeMarks(editor);
        case VISUAL:
          // XXX: Untested code
          return VimPlugin.getMark().getVisualSelectionMarks(editor);
        default:
          return null;
      }
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
  }
}
