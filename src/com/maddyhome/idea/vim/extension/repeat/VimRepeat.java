package com.maddyhome.idea.vim.extension.repeat;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.*;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author dhleong
 */
public class VimRepeat extends VimNonDisposableExtension {

  static List<KeyStroke> repeatSequence;
  static Command repeatChangeCommand;

  @NotNull
  @Override
  public String getName() {
    return "repeat";
  }

  @Override
  protected void initOnce() {
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>(RepeatDot)"), new RepeatDotHandler(), false);

    putKeyMapping(MappingMode.N, parseKeys("."), parseKeys("<Plug>(RepeatDot)"), true);
  }

  private static class RepeatDotHandler implements VimExtensionHandler {
    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      Project proj = editor.getProject();
      if (proj == null) return;

      final Command newLastChange =
        CommandState.getInstance(editor).getLastChangeCommand();
      if (repeatSequence != null && newLastChange == repeatChangeCommand) {
        executeNormal(repeatSequence, editor);
      } else {
        KeyStroke repeatStroke = parseKeys(".").get(0);
        KeyHandler.getInstance().handleKey(editor, repeatStroke, context, false);
      }
    }
  }

  public static void set(Editor editor, List<KeyStroke> sequence) {
    repeatChangeCommand =
      CommandState.getInstance(editor).getLastChangeCommand();
    repeatSequence = sequence;
  }
}
