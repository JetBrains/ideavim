/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjindent;

import com.intellij.openapi.editor.Caret;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.ImmutableVimCaret;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.group.visual.EngineVisualGroupKt;
import com.maddyhome.idea.vim.handler.TextObjectActionHandler;
import com.maddyhome.idea.vim.helper.InlayHelperKt;
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor;
import com.maddyhome.idea.vim.listener.VimListenerSuppressor;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.state.VimStateMachine;
import com.maddyhome.idea.vim.state.mode.Mode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping;
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping;

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
 * <p>
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
    putExtensionHandlerMapping(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-indent-ai"), getOwner(),
      new IndentObject(true, false), false);
    putExtensionHandlerMapping(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-indent-aI"), getOwner(),
      new IndentObject(true, true), false);
    putExtensionHandlerMapping(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-indent-ii"), getOwner(),
      new IndentObject(false, false), false);

    putKeyMapping(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("ai"), getOwner(), VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-indent-ai"), true);
    putKeyMapping(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("aI"), getOwner(), VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-indent-aI"), true);
    putKeyMapping(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("ii"), getOwner(), VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-indent-ii"), true);
  }

  static class IndentObject implements ExtensionHandler {
    final boolean includeAbove;
    final boolean includeBelow;

    IndentObject(boolean includeAbove, boolean includeBelow) {
      this.includeAbove = includeAbove;
      this.includeBelow = includeBelow;
    }

    @Override
    public boolean isRepeatable() {
      return false;
    }

    static class IndentObjectHandler extends TextObjectActionHandler {
      final boolean includeAbove;
      final boolean includeBelow;

      IndentObjectHandler(boolean includeAbove, boolean includeBelow) {
        this.includeAbove = includeAbove;
        this.includeBelow = includeBelow;
      }

      @Nullable
      @Override
      public TextRange getRange(@NotNull VimEditor editor,
                                @NotNull ImmutableVimCaret caret,
                                @NotNull ExecutionContext context,
                                int count,
                                int rawCount) {
        final CharSequence charSequence = ((IjVimEditor) editor).getEditor().getDocument().getCharsSequence();
        final int caretOffset = ((IjVimCaret) caret).getCaret().getOffset();

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
    public void execute(@NotNull VimEditor editor, @NotNull ExecutionContext context, @NotNull OperatorArguments operatorArguments) {
      IjVimEditor vimEditor = (IjVimEditor) editor;
      @NotNull VimStateMachine vimStateMachine = VimStateMachine.Companion.getInstance(vimEditor);
      int count = Math.max(1, vimStateMachine.getCommandBuilder().getCount());

      final IndentObjectHandler textObjectHandler = new IndentObjectHandler(includeAbove, includeBelow);

      if (!vimStateMachine.isOperatorPending()) {
        ((IjVimEditor) editor).getEditor().getCaretModel().runForEachCaret((Caret caret) -> {
          final TextRange range = textObjectHandler.getRange(vimEditor, new IjVimCaret(caret), context, count, 0);
          if (range != null) {
            try (VimListenerSuppressor.Locked ignored = SelectionVimListenerSuppressor.INSTANCE.lock()) {
              if (vimStateMachine.getMode() instanceof Mode.VISUAL) {
                EngineVisualGroupKt.vimSetSelection(new IjVimCaret(caret), range.getStartOffset(), range.getEndOffset() - 1, true);
              } else {
                InlayHelperKt.moveToInlayAwareOffset(caret, range.getStartOffset());
              }
            }
          }

        });
      } else {
        vimStateMachine.getCommandBuilder().completeCommandPart(new Argument(new Command(count,
          textObjectHandler, Command.Type.MOTION,
          EnumSet.noneOf(CommandFlags.class))));
      }
    }
  }
}
