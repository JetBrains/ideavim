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

package com.maddyhome.idea.vim.extension.commentary;

import com.intellij.codeInsight.actions.MultiCaretCodeInsightActionHandler;
import com.intellij.codeInsight.generation.CommentByBlockCommentHandler;
import com.intellij.codeInsight.generation.CommentByLineCommentHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.key.OperatorFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.*;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author dhleong
 */
public class CommentaryExtension implements VimExtension {

  @Override
  public @NotNull String getName() {
    return "commentary";
  }

  @Override
  public void init() {
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>(CommentMotion)"), getOwner(), new CommentMotionHandler(), false);
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>(CommentLine)"), getOwner(), new CommentLineHandler(), false);
    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>(CommentMotionV)"), getOwner(), new CommentMotionVHandler(), false);

    putKeyMapping(MappingMode.N, parseKeys("gc"), getOwner(), parseKeys("<Plug>(CommentMotion)"), true);
    putKeyMapping(MappingMode.N, parseKeys("gcc"), getOwner(), parseKeys("<Plug>(CommentLine)"), true);
    putKeyMapping(MappingMode.XO, parseKeys("gc"), getOwner(), parseKeys("<Plug>(CommentMotionV)"), true);
  }

  private static class CommentMotionHandler implements VimExtensionHandler {
    @Override
    public boolean isRepeatable() {
      return true;
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      setOperatorFunction(new Operator());
      executeNormalWithoutMapping(parseKeys("g@"), editor);
    }
  }

  private static class CommentMotionVHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      if (!editor.getCaretModel().getPrimaryCaret().hasSelection()) {
        return;
      }

      // always use line-wise comments
      if (!new Operator().apply(editor, context, SelectionType.LINE_WISE)) {
        return;
      }

      WriteAction.run(() -> {
        // Leave visual mode
        executeNormalWithoutMapping(parseKeys("<Esc>"), editor);
        editor.getCaretModel().moveToOffset(editor.getCaretModel().getPrimaryCaret().getSelectionStart());
      });
    }
  }

  private static class Operator implements OperatorFunction {
    @Override
    public boolean apply(@NotNull Editor editor, @NotNull DataContext context, @NotNull SelectionType selectionType) {
      final TextRange range = getCommentRange(editor);
      if (range == null) return false;

      if (CommandState.getInstance(editor).getMode() != CommandState.Mode.VISUAL) {
        editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
      }

      final MultiCaretCodeInsightActionHandler handler =
        selectionType == SelectionType.CHARACTER_WISE
          ? new CommentByBlockCommentHandler()
          : new CommentByLineCommentHandler();

      return WriteAction.compute(() -> {
        try {
          Project proj = editor.getProject();
          if (proj == null) return false;

          PsiFile file = PsiDocumentManager.getInstance(proj).getPsiFile(editor.getDocument());
          if (file == null) return false;

          handler.invoke(editor.getProject(), editor, editor.getCaretModel().getCurrentCaret(), file);
          handler.postInvoke();

          // Jump back to start if in block mode
          if (selectionType == SelectionType.CHARACTER_WISE) {
            executeNormalWithoutMapping(parseKeys("`["), editor);
          }
          return true;
        } finally {
          // remove the selection
          editor.getSelectionModel().removeSelection();
        }
      });
    }

    private @Nullable TextRange getCommentRange(@NotNull Editor editor) {
      final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
      switch (mode) {
        case COMMAND:
          return VimPlugin.getMark().getChangeMarks(editor);
        case VISUAL:
          Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
          return new TextRange(primaryCaret.getSelectionStart(), primaryCaret.getSelectionEnd());
        default:
          return null;
      }
    }
  }

  private static class CommentLineHandler implements VimExtensionHandler {
    @Override
    public boolean isRepeatable() {
      return true;
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      final int offset = editor.getCaretModel().getOffset();
      final int line = editor.getDocument().getLineNumber(offset);
      final int lineStart = editor.getDocument().getLineStartOffset(line);
      final int lineEnd = editor.getDocument().getLineEndOffset(line);
      VimPlugin.getMark().setChangeMarks(editor, new TextRange(lineStart, lineEnd));
      new Operator().apply(editor, context, SelectionType.LINE_WISE);
    }
  }
}
