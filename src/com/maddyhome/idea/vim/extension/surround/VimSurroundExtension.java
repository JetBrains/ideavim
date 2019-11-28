/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.extension.surround;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Caret;
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
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.key.OperatorFunction;
import com.maddyhome.idea.vim.option.ClipboardOptionsData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private final static Pattern tagNameAndAttributesCapturePattern = Pattern.compile("(\\w+)([^>]*)>");

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
    .put('s', Pair.create(" ", ""))
    .build();

  @NotNull
  @Override
  public String getName() {
    return "surround";
  }

  @Override
  protected void initOnce() {
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>YSurround"), new YSurroundHandler(), false);
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>CSurround"), new CSurroundHandler(), false);
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>DSurround"), new DSurroundHandler(), false);
    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>VSurround"), new VSurroundHandler(), false);

    putKeyMapping(MappingMode.N, parseKeys("ys"), parseKeys("<Plug>YSurround"), true);
    putKeyMapping(MappingMode.N, parseKeys("cs"), parseKeys("<Plug>CSurround"), true);
    putKeyMapping(MappingMode.N, parseKeys("ds"), parseKeys("<Plug>DSurround"), true);
    putKeyMapping(MappingMode.XO, parseKeys("S"), parseKeys("<Plug>VSurround"), true);
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
    final String tagInput = inputString(editor, "<", '>');
    final Matcher matcher = tagNameAndAttributesCapturePattern.matcher(tagInput);

    if (matcher.find()) {
      final String tagName = matcher.group(1);
      final String tagAttributes = matcher.group(2);
      return Pair.create("<" + tagName + tagAttributes + ">", "</" + tagName + ">");
    }
    else {
      return null;
    }
  }

  @Nullable
  private static Pair<String, String> inputFunctionName(
    @NotNull Editor editor,
    boolean withInternalSpaces
  ) {
    final String functionNameInput = inputString(editor, "function: ", null);

    if (functionNameInput.isEmpty()) {
      return null;
    }

    return withInternalSpaces
      ? Pair.create(functionNameInput + "( ", " )")
      : Pair.create(functionNameInput + "(", ")");
  }

  @Nullable
  private static Pair<String, String> getOrInputPair(char c, @NotNull Editor editor) {
    switch (c) {
      case '<':
      case 't':
        return inputTagPair(editor);
      case 'f':
        return inputFunctionName(editor, false);
      case 'F':
        return inputFunctionName(editor, true);
      default:
        return getSurroundPair(c);
    }
  }

  private static char getChar(@NotNull Editor editor) {
    final KeyStroke key = inputKeyStroke(editor);
    final char keyChar = key.getKeyChar();
    if (keyChar == KeyEvent.CHAR_UNDEFINED || keyChar == KeyEvent.VK_ESCAPE) {
      return 0;
    }
    return keyChar;
  }

  private static class YSurroundHandler implements VimExtensionHandler {
    @Override
    public boolean isRepeatable() {
      return true;
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      setOperatorFunction(new Operator());
      executeNormal(parseKeys("g@"), editor);
    }
  }

  private static class VSurroundHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      int selectionStart = editor.getCaretModel().getPrimaryCaret().getSelectionStart();
      // NB: Operator ignores SelectionType anyway
      if (!new Operator().apply(editor, context, SelectionType.CHARACTER_WISE)) {
        return;
      }

      WriteAction.run(() -> {
        // Leave visual mode
        executeNormal(parseKeys("<Esc>"), editor);
        editor.getCaretModel().moveToOffset(selectionStart);
      });
    }

  }

  private static class CSurroundHandler implements VimExtensionHandler {
    @Override
    public boolean isRepeatable() {
      return true;
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      final char charFrom = getChar(editor);
      if (charFrom == 0) {
        return;
      }

      final char charTo = getChar(editor);
      if (charTo == 0) {
        return;
      }

      Pair<String, String> newSurround = getOrInputPair(charTo, editor);
      if (newSurround == null) {
        return;
      }

      WriteAction.run(() -> change(editor, charFrom, newSurround));
    }

    static void change(@NotNull Editor editor, char charFrom, @Nullable Pair<String, String> newSurround) {
      // We take over the " register, so preserve it
      final List<KeyStroke> oldValue = getRegister(REGISTER);

      // Extract the inner value
      perform("di" + pick(charFrom), editor);
      List<KeyStroke> innerValue = getRegister(REGISTER);
      if (innerValue == null) {
        innerValue = new ArrayList<>();
      } else {
        innerValue = new ArrayList<>(innerValue);
      }

      // Delete the surrounding
      perform("da" + pick(charFrom), editor);

      // Insert the surrounding characters and paste
      if (newSurround != null) {
        innerValue.addAll(0, parseKeys(escape(newSurround.first)));
        innerValue.addAll(parseKeys(escape(newSurround.second)));
      }
      pasteSurround(innerValue, editor);

      // Restore the old value
      setRegister(REGISTER, oldValue);

      // Jump back to start
      executeNormal(parseKeys("`["), editor);
    }

    @NotNull
    private static String escape(@NotNull String sequence) {
      return sequence.replace("<", "\\<");
    }

    private static void perform(@NotNull String sequence, @NotNull Editor editor) {
      try (ClipboardOptionsData.IdeaputDisabler ignored = new ClipboardOptionsData.IdeaputDisabler()) {
        executeNormal(parseKeys("\"" + REGISTER + sequence), editor);
      }
    }

    private static void pasteSurround(@NotNull List<KeyStroke> innerValue, @NotNull Editor editor) {
      // This logic is direct from vim-surround
      final int offset = editor.getCaretModel().getOffset();
      final int lineEndOffset = EditorHelper.getLineEndForOffset(editor, offset);

      final Mark motionEndMark = VimPlugin.getMark().getMark(editor, ']');
      final int motionEndOffset;
      if (motionEndMark != null) {
        motionEndOffset = EditorHelper.getOffset(editor, motionEndMark.getLogicalLine(), motionEndMark.getCol());
      }
      else {
        motionEndOffset = -1;
      }
      final String pasteCommand = motionEndOffset == lineEndOffset && offset + 1 == lineEndOffset ? "p" : "P";
      setRegister(REGISTER, innerValue);
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
    public boolean isRepeatable() {
      return true;
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      // Deleting surround is just changing the surrounding to "nothing"
      final char charFrom = getChar(editor);
      if (charFrom == 0) {
        return;
      }
      WriteAction.run(() -> CSurroundHandler.change(editor, charFrom, null));
    }
  }

  private static class Operator implements OperatorFunction {
    @Override
    public boolean apply(@NotNull Editor editor, @NotNull DataContext context, @NotNull SelectionType selectionType) {
      final char c = getChar(editor);
      if (c == 0) {
        return true;
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
      WriteAction.run(() -> {
        final ChangeGroup change = VimPlugin.getChange();
        final String leftSurround = pair.getFirst();
        final Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        primaryCaret.moveToOffset(range.getStartOffset());
        change.insertText(editor, primaryCaret, leftSurround);
        primaryCaret.moveToOffset(range.getEndOffset() + leftSurround.length());
        change.insertText(editor, primaryCaret, pair.getSecond());

        // Jump back to start
        executeNormal(parseKeys("`["), editor);
      });
      return true;
    }

    @Nullable
    private TextRange getSurroundRange(@NotNull Editor editor) {
      final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
      switch (mode) {
        case COMMAND:
          return VimPlugin.getMark().getChangeMarks(editor);
        case VISUAL:
          int selectionStart = editor.getCaretModel().getPrimaryCaret().getSelectionStart();
          int selectionEnd = editor.getCaretModel().getPrimaryCaret().getSelectionEnd();
          return new TextRange(selectionStart, selectionEnd);
        default:
          return null;
      }
    }
  }
}
