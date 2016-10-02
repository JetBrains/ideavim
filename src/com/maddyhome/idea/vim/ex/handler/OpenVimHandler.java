/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.option.StringOption;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * :vim to open file with gui vim (gvim, macvim, etc)
 * set gvimpath in .ideavimrc
 *
 * example :
 *     set gvimpath=/usr/local/opt/macvim/bin/mvim
 *
 * @author John Grib
 */
public class OpenVimHandler extends CommandHandler {
    public OpenVimHandler() {
        super("vi", "m", RANGE_FORBIDDEN | ARGUMENT_FORBIDDEN);
    }

    public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) {

        VirtualFile vf = EditorData.getVirtualFile(editor);
        String filePath = vf.getCanonicalPath();

        StringOption o = (StringOption) Options.getInstance().getOption(Options.GVIM_PATH);
        String gVimPath = o.getValue();

        try {
            String command = gVimPath + " " + filePath;
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}
