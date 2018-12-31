package com.maddyhome.idea.vim.action.change.change.number;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ChangeVisualNumberIncAction extends VimCommandAction {
    protected ChangeVisualNumberIncAction() {
        super(new VisualOperatorActionHandler(true, CaretOrder.DECREASING_OFFSET) {
            @Override
            protected boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                                      @NotNull Command cmd, @NotNull TextRange range) {
                return VimPlugin.getChange().changeNumberVisualMode(editor, caret, range, cmd.getCount(), false);
            }
        });
    }

    @NotNull
    @Override
    public Set<MappingMode> getMappingModes() {
        return MappingMode.V;
    }

    @NotNull
    @Override
    public Set<List<KeyStroke>> getKeyStrokesSet() {
        return parseKeysSet("<C-A>");
    }

    @NotNull
    @Override
    public Command.Type getType() {
        return Command.Type.CHANGE;
    }

    @Override
    public EnumSet<CommandFlags> getFlags() {
        return EnumSet.of(CommandFlags.FLAG_EXIT_VISUAL);
    }
}
