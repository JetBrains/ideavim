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
import com.maddyhome.idea.vim.command.*;
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
        final CharSequence charSequence = editor.getDocument().getCharsSequence();
        final int caretOffset = caret.getOffset();

        // Part 1: Find the start of the caret line.
        int caretLineStartOffset = caretOffset;
        int accumulatedWhitespace = 0;
        while (--caretLineStartOffset >= 0) {
          final char ch = charSequence.charAt(caretLineStartOffset);
          if (ch == ' ' || ch == '\t') {
            ++accumulatedWhitespace;
          } else if (ch == '\n') {
            ++caretLineStartOffset;
            break;
          } else {
            accumulatedWhitespace = 0;
          }
        }
        if (caretLineStartOffset < 0) {
          caretLineStartOffset = 0;
        }

        // `caretLineStartOffset` points to the first character in the line where the caret is located.

        // Part 2: Compute the indentation level of the caret line.
        // This is done as a separate step so that it works even when the caret is inside the indentation.
        int offset = caretLineStartOffset;
        int indentSize = 0;
        while (++offset < charSequence.length()) {
          final char ch = charSequence.charAt(offset);
          if (ch == ' ' || ch == '\t') {
            ++indentSize;
          } else {
            break;
          }
        }

        // `indentSize` contains the amount of indent to be used for the text object range to be returned.

        Integer upperBoundaryOffset = null;
        // Part 3: Find a line above the caret line, that has an indentation lower than `indentSize`.
        int pos1 = caretLineStartOffset - 1;
        boolean isUpperBoundaryFound = false;
        while (upperBoundaryOffset == null) {
          // 3.1: Going backwards from `caretLineStartOffset`, find the first non-whitespace character.
          while (--pos1 >= 0) {
            final char ch = charSequence.charAt(pos1);
            if (ch != ' ' && ch != '\t' && ch != '\n') {
              break;
            }
          }
          // 3.2: Find the indent size of the line with this non-whitespace character and check against `indentSize`.
          accumulatedWhitespace = 0;
          while (--pos1 >= 0) {
            final char ch = charSequence.charAt(pos1);
            if (ch == ' ' || ch == '\t') {
              ++accumulatedWhitespace;
            } else if (ch == '\n') {
              if (accumulatedWhitespace < indentSize) {
                upperBoundaryOffset = pos1 + 1;
                isUpperBoundaryFound = true;
              }
              break;
            } else {
              accumulatedWhitespace = 0;
            }
          }
          if (pos1 < 0) {
            // Reached start of the buffer.
            upperBoundaryOffset = 0;
            isUpperBoundaryFound = accumulatedWhitespace < indentSize;
          }
        }

        // Now `upperBoundaryOffset` marks the beginning of an `ai` text object.
        if (isUpperBoundaryFound && !includeAbove) {
          while (++upperBoundaryOffset < charSequence.length()) {
            final char ch = charSequence.charAt(upperBoundaryOffset);
            if (ch == '\n') {
              ++upperBoundaryOffset;
              break;
            }
          }
          while (charSequence.charAt(upperBoundaryOffset) == '\n') {
            ++upperBoundaryOffset;
          }
        }

        // Part 4: Find the start of the caret line.
        int caretLineEndOffset = caretOffset;
        while (++caretLineEndOffset < charSequence.length()) {
          final char ch = charSequence.charAt(caretLineEndOffset);
          if (ch == '\n') {
            ++caretLineEndOffset;
            break;
          }
        }

        // `caretLineEndOffset` points to the first charater in the line below caret line.

        Integer lowerBoundaryOffset = null;
        // Part 5: Find a line below the caret line, that has an indentation lower than `indentSize`.
        int pos2 = caretLineEndOffset - 1;
        boolean isLowerBoundaryFound = false;
        while (lowerBoundaryOffset == null) {
          int accumulatedWhitespace2 = 0;
          int lastNewlinePos = caretLineEndOffset - 1;
          boolean isInIndent = true;
          while (++pos2 < charSequence.length()) {
            final char ch = charSequence.charAt(pos2);
            if (isIndentChar(ch) && isInIndent) {
              ++accumulatedWhitespace2;
            } else if (ch == '\n') {
              accumulatedWhitespace2 = 0;
              lastNewlinePos = pos2;
              isInIndent = true;
            } else {
              if (isInIndent && accumulatedWhitespace2 < indentSize) {
                lowerBoundaryOffset = lastNewlinePos;
                isLowerBoundaryFound = true;
                break;
              }
              isInIndent = false;
            }
          }
          if (pos2 >= charSequence.length()) {
            // Reached end of the buffer.
            lowerBoundaryOffset = charSequence.length() - 1;
          }
        }

        // Now `lowerBoundaryOffset` marks the end of an `ii` text object.
        if (isLowerBoundaryFound && includeBelow) {
          while (++lowerBoundaryOffset < charSequence.length()) {
            final char ch = charSequence.charAt(lowerBoundaryOffset);
            if (ch == '\n') {
              break;
            }
          }
        }

        return new TextRange(upperBoundaryOffset, lowerBoundaryOffset);
      }

      @NotNull
      @Override
      public TextObjectVisualType getVisualType() {
        return TextObjectVisualType.LINE_WISE;
      }

      private boolean isIndentChar(char ch) {
        return ch == ' ' || ch == '\t';
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
