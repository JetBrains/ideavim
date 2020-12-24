/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim.extension.textobjindent;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.TextObjectVisualType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.handler.TextObjectActionHandler;
import com.maddyhome.idea.vim.helper.InlayHelperKt;
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor;
import com.maddyhome.idea.vim.listener.VimListenerSuppressor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping;
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping;
import static com.maddyhome.idea.vim.group.visual.VisualGroupKt.vimSetSelection;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * Port of vim-indent-object:
 * https://github.com/michaeljsmith/vim-indent-object
 *
 * <p>
 * vim-indent-object provides these text objects based on the cursor line's indentation:
 * <ul>
 *   <li><code>ai</code> <b>A</b>n <b>I</b>ndentation level and line above.</li>
 *   <li><code>ii</code> <b>I</b>nner <b>I</b>ndentation level (no line above).</li>
 *   <li><code>aI</code> <b>A</b>n <b>I</b>ndentation level and lines above and below.</li>
 *   <li><code>iI</code> <b>I</b>nner <b>I</b>ndentation level (no lines above and below). Synonym of <code>ii</code></li>
 * </ul>
 *
 * See also the reference manual for more details at:
 * https://github.com/michaeljsmith/vim-indent-object/blob/master/doc/indent-object.txt
 *
 * @author Shrikant Kandula (@sharat87)
 */
public class VimIndentObject implements VimExtension {

  @Override
  public @NotNull
  String getName() {
    return "textobj-indent";
  }

  @Override
  public void init() {
    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>textobj-indent-ai"), getOwner(),
      new IndentObject(true, false), false);
    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>textobj-indent-aI"), getOwner(),
      new IndentObject(true, true), false);
    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>textobj-indent-ii"), getOwner(),
      new IndentObject(false, false), false);

    putKeyMapping(MappingMode.XO, parseKeys("ai"), getOwner(), parseKeys("<Plug>textobj-indent-ai"), true);
    putKeyMapping(MappingMode.XO, parseKeys("aI"), getOwner(), parseKeys("<Plug>textobj-indent-aI"), true);
    putKeyMapping(MappingMode.XO, parseKeys("ii"), getOwner(), parseKeys("<Plug>textobj-indent-ii"), true);
  }

  static class IndentObject implements VimExtensionHandler {
    final boolean includeAbove;
    final boolean includeBelow;

    IndentObject(boolean includeAbove, boolean includeBelow) {
      this.includeAbove = includeAbove;
      this.includeBelow = includeBelow;
    }

    static class IndentObjectHandler extends TextObjectActionHandler {
      final boolean includeAbove;
      final boolean includeBelow;

      IndentObjectHandler(boolean includeAbove, boolean includeBelow) {
        this.includeAbove = includeAbove;
        this.includeBelow = includeBelow;
      }

      @Override
      public @Nullable
      TextRange getRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                         int count, int rawCount, @Nullable Argument argument) {
        final int caretLineNum = caret.getCaretModel().getVisualPosition().getLine();
        String content = editor.getDocument().getText();
        String[] lines = content.split("\n");
        String caretLine = lines[caretLineNum];

        Pattern indentPattern = Pattern.compile("^\\s+");
        Matcher matcher = indentPattern.matcher(caretLine);
        if (!matcher.find()) {
          return new TextRange(
            editor.getDocument().getLineStartOffset(caretLineNum),
            editor.getDocument().getLineEndOffset(caretLineNum)
          );
        }

        int indentSize = matcher.end();
        int startLineNum = caretLineNum;
        int endLineNum = caretLineNum;

        if (indentSize > 0) {
          while (--startLineNum >= 0) {
            final String line = lines[startLineNum];
            if (isWhiteSpace(line)) {
              continue;
            }
            Matcher matcher1 = indentPattern.matcher(line);
            if (!matcher1.find()) {
              break;
            }
            int indentSize1 = matcher1.end();
            if (indentSize1 < indentSize) {
              break;
            }
          }

          while (++endLineNum < lines.length) {
            final String line = lines[endLineNum];
            if (isWhiteSpace(line)) {
              continue;
            }
            Matcher matcher1 = indentPattern.matcher(line);
            if (!matcher1.find()) {
              break;
            }
            int indentSize1 = matcher1.end();
            if (indentSize1 < indentSize) {
              break;
            }
          }
        }

        if (!includeAbove) {
          ++startLineNum;
          while (startLineNum < caretLineNum && isWhiteSpace(lines[startLineNum])) {
            ++startLineNum;
          }
        }

        if (!includeBelow) {
          --endLineNum;
          while (endLineNum < caretLineNum && isWhiteSpace(lines[endLineNum])) {
            --endLineNum;
          }
        }

        return new TextRange(
          editor.getDocument().getLineStartOffset(startLineNum),
          editor.getDocument().getLineEndOffset(endLineNum)
        );
      }

      @NotNull
      @Override
      public TextObjectVisualType getVisualType() {
        return TextObjectVisualType.LINE_WISE;
      }

      private boolean isWhiteSpace(String text) {
        return text.matches("^\\s*$");
      }
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      @NotNull CommandState commandState = CommandState.getInstance(editor);
      int count = Math.max(1, commandState.getCommandBuilder().getCount());

      final IndentObjectHandler textObjectHandler = new IndentObjectHandler(includeAbove, includeBelow);

      if (!commandState.isOperatorPending()) {
        editor.getCaretModel().runForEachCaret((Caret caret) -> {
          final TextRange range = textObjectHandler.getRange(editor, caret, context, count, 0, null);
          if (range != null) {
            try (VimListenerSuppressor.Locked ignored = SelectionVimListenerSuppressor.INSTANCE.lock()) {
              if (commandState.getMode() == CommandState.Mode.VISUAL) {
                vimSetSelection(caret, range.getStartOffset(), range.getEndOffset() - 1, true);
              } else {
                InlayHelperKt.moveToInlayAwareOffset(caret, range.getStartOffset());
              }
            }
          }

        });
      } else {
        commandState.getCommandBuilder().completeCommandPart(new Argument(new Command(count,
          textObjectHandler, Command.Type.MOTION,
          EnumSet.noneOf(CommandFlags.class))));
      }
    }
  }
}
