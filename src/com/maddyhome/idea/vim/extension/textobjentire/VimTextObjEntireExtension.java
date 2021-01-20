/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.extension.textobjentire;

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
 * Port of vim-entire:
 * https://github.com/kana/vim-textobj-entire
 *
 * <p>
 * vim-textobj-entire provides two text objects:
 * <ul>
 *   <li>ae targets the entire content of the current buffer.</li>
 *   <li>ie is similar to ae, but ie does not include leading and trailing empty lines. ie is handy for some situations. For example,</li>
 *   <ul>
 *     <li>Paste some text into a new buffer (<C-w>n"*P) -- note that the initial empty line is left as the last line.</li>
 *     <li>Edit the text (:%s/foo/bar/g etc)</li>
 *     <li>Then copy the resulting text to another application ("*yie)</li>
 *   </ul>
 * </ul>
 *
 * See also the reference manual for more details at:
 * https://github.com/kana/vim-textobj-entire/blob/master/doc/textobj-entire.txt
 *
 * @author Alexandre Grison (@agrison)
 */
public class VimTextObjEntireExtension implements VimExtension {

  @Override
  public @NotNull
  String getName() {
    return "textobj-entire";
  }


  @Override
  public void init() {
    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>textobj-entire-a"), getOwner(),
      new VimTextObjEntireExtension.EntireHandler(false), false);
    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>textobj-entire-i"), getOwner(),
      new VimTextObjEntireExtension.EntireHandler(true), false);

    putKeyMapping(MappingMode.XO, parseKeys("ae"), getOwner(), parseKeys("<Plug>textobj-entire-a"), true);
    putKeyMapping(MappingMode.XO, parseKeys("ie"), getOwner(), parseKeys("<Plug>textobj-entire-i"), true);
  }

  static class EntireHandler implements VimExtensionHandler {
    final boolean ignoreLeadingAndTrailing;

    EntireHandler(boolean ignoreLeadingAndTrailing) {
      this.ignoreLeadingAndTrailing = ignoreLeadingAndTrailing;
    }

    static class EntireTextObjectHandler extends TextObjectActionHandler {
      final boolean ignoreLeadingAndTrailing;

      EntireTextObjectHandler(boolean ignoreLeadingAndTrailing) {
        this.ignoreLeadingAndTrailing = ignoreLeadingAndTrailing;
      }

      @Override
      public @Nullable
      TextRange getRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                         int count, int rawCount, @Nullable Argument argument) {
        int start = 0, end = editor.getDocument().getTextLength();

        // for the `ie` text object we don't want leading an trailing spaces
        // so we have to scan the document text to find the correct start & end
        if (ignoreLeadingAndTrailing) {
          String content = editor.getDocument().getText();
          for (int i = 0; i < content.length(); ++i) {
            if (!Character.isWhitespace(content.charAt(i))) {
              start = i;
              break;
            }
          }

          for (int i = content.length() - 1; i >= start; --i) {
            if (!Character.isWhitespace(content.charAt(i))) {
              end = i + 1;
              break;
            }
          }
        }

        return new TextRange(start, end);
      }

      @NotNull
      @Override
      public TextObjectVisualType getVisualType() {
        return TextObjectVisualType.CHARACTER_WISE;
      }
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      @NotNull CommandState commandState = CommandState.getInstance(editor);
      int count = Math.max(1, commandState.getCommandBuilder().getCount());

      final EntireTextObjectHandler textObjectHandler = new EntireTextObjectHandler(ignoreLeadingAndTrailing);
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
