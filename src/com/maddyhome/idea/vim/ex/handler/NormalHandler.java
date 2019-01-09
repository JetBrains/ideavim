package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.handler.ExecuteMethodNotOverriddenException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

public class NormalHandler extends CommandHandler {
    public NormalHandler() {
        super("norm", "al", RANGE_OPTIONAL | ARGUMENT_REQUIRED | WRITABLE, true,
                CaretOrder.DECREASING_OFFSET);
    }

    @Override
    public boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, @NotNull ExCommand cmd) throws ExException, ExecuteMethodNotOverriddenException {
        List<KeyStroke> keys = parseKeys(cmd.getArgument());
        KeyHandler keyHandler = KeyHandler.getInstance();
        keyHandler.reset(editor);
        for (KeyStroke key : keys) {
            keyHandler.handleKey(editor, key, context);
        }
        return true;
    }
}
