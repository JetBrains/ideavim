package com.maddyhome.idea.vim.ex.handler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import org.jetbrains.annotations.NotNull;

public class PreviousTabHandler extends CommandHandler {
    public PreviousTabHandler(){
        super(new CommandName[]{
                new CommandName("tabp", "revious"),
                new CommandName("tabN", "ext")
        }, ARGUMENT_OPTIONAL);
    }

    public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) {
        VimPlugin.getMotion().moveCaretGotoPreviousTab(editor, context,1 );
        return true;
    }
}
