package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.helper.ShellCommandHelper;
import org.jetbrains.annotations.NotNull;

public class AdbHandler extends CommandHandler {

    private static String cachedWifiAddress;

    public AdbHandler() {
        super("a", "db", ARGUMENT_OPTIONAL);
    }

    @Override
    public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd)
            throws ExException {
        final String argument = cmd.getArgument();
        final String[] splitArguments = argument.split(" ");

        StringBuilder script = new StringBuilder();
        if (splitArguments.length > 0) {
            final String option = splitArguments[0].trim();

            if (option.equalsIgnoreCase("start")) {
                script.append("adb tcpip 5555\n");
                if (splitArguments.length > 1) {
                    cachedWifiAddress = splitArguments[1].trim();
                    script.append("adb connect " + cachedWifiAddress);
                }
                else if (cachedWifiAddress != null) {
                    script.append("adb connect " + cachedWifiAddress);
                }
                else {
                    ExOutputModel.getInstance(editor).output("Usage: adb start <host>");
                    return false;
                }
            }
            else {
                script.append("adb " + argument);
            }
        }
        else {
            script.append("adb");
        }

        ExOutputModel.getInstance(editor).output(ShellCommandHelper.getResultFromShell(script));
        return true;
    }
}
