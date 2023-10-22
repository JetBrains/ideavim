/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjentire;

import com.intellij.openapi.editor.Caret;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.ImmutableVimCaret;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.extension.VimExtension;
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
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing;

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
 * <p>
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
    putExtensionHandlerMapping(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-entire-a"), getOwner(),
      new VimTextObjEntireExtension.EntireHandler(false), false);
    putExtensionHandlerMapping(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-entire-i"), getOwner(),
      new VimTextObjEntireExtension.EntireHandler(true), false);

    putKeyMappingIfMissing(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("ae"), getOwner(), VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-entire-a"), true);
    putKeyMappingIfMissing(MappingMode.XO, VimInjectorKt.getInjector().getParser().parseKeys("ie"), getOwner(), VimInjectorKt.getInjector().getParser().parseKeys("<Plug>textobj-entire-i"), true);
  }

  static class EntireHandler implements ExtensionHandler {
    final boolean ignoreLeadingAndTrailing;

    EntireHandler(boolean ignoreLeadingAndTrailing) {
      this.ignoreLeadingAndTrailing = ignoreLeadingAndTrailing;
    }

    @Override
    public boolean isRepeatable() {
      return false;
    }

    static class EntireTextObjectHandler extends TextObjectActionHandler {
      final boolean ignoreLeadingAndTrailing;

      EntireTextObjectHandler(boolean ignoreLeadingAndTrailing) {
        this.ignoreLeadingAndTrailing = ignoreLeadingAndTrailing;
      }

      @Nullable
      @Override
      public TextRange getRange(@NotNull VimEditor editor,
                                @NotNull ImmutableVimCaret caret,
                                @NotNull ExecutionContext context,
                                int count,
                                int rawCount) {
        int start = 0, end = ((IjVimEditor) editor).getEditor().getDocument().getTextLength();

        // for the `ie` text object we don't want leading an trailing spaces
        // so we have to scan the document text to find the correct start & end
        if (ignoreLeadingAndTrailing) {
          String content = ((IjVimEditor) editor).getEditor().getDocument().getText();
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
    public void execute(@NotNull VimEditor editor, @NotNull ExecutionContext context, @NotNull OperatorArguments operatorArguments) {
      @NotNull VimStateMachine vimStateMachine = VimStateMachine.Companion.getInstance(editor);
      int count = Math.max(1, vimStateMachine.getCommandBuilder().getCount());

      final EntireTextObjectHandler textObjectHandler = new EntireTextObjectHandler(ignoreLeadingAndTrailing);
      //noinspection DuplicatedCode
      if (!vimStateMachine.isOperatorPending()) {
        ((IjVimEditor) editor).getEditor().getCaretModel().runForEachCaret((Caret caret) -> {
          final TextRange range = textObjectHandler.getRange(editor, new IjVimCaret(caret), context, count, 0);
          if (range != null) {
            try (VimListenerSuppressor.Locked ignored = SelectionVimListenerSuppressor.INSTANCE.lock()) {
              if (vimStateMachine.getMode() instanceof Mode.VISUAL) {
                com.maddyhome.idea.vim.group.visual.EngineVisualGroupKt.vimSetSelection(new IjVimCaret(caret), range.getStartOffset(), range.getEndOffset() - 1, true);
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
