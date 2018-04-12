package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.helper.ShellCommandHelper;
import org.jetbrains.annotations.NotNull;

public class GitHandler extends CommandHandler {

    public GitHandler() {
        super("g", "it", ARGUMENT_OPTIONAL);
    }

    @Override
    public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                           @NotNull ExCommand cmd) throws ExException {
        final String argument = cmd.getArgument();

        StringBuilder script = new StringBuilder();
        script.append("cd " + editor.getProject().getBasePath() + "\n");
        script.append("git " + argument + "\n");

        ExOutputModel.getInstance(editor).output(ShellCommandHelper.getResultFromShell(script));
        return true;
    }
}