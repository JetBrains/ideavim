package com.maddyhome.idea.vim.group;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2004 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.common.Register;
import java.util.List;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Used to handle playback of macros
 */
public class MacroGroup extends AbstractActionGroup
{
    public MacroGroup()
    {
    }

    /**
     * This method is used to play the macro of keystrokes stored in the specified registers.
     * @param editor The editor to play the macro in
     * @param context The data context
     * @param reg The register to get the macro from
     * @param count The number of times to execute the macro
     * @return true if able to play the macro, false if invalid or empty register
     */
    public boolean playbackRegister(Editor editor, DataContext context, char reg, int count)
    {
        Register register = CommandGroups.getInstance().getRegister().getPlaybackRegister(reg);
        if (register == null)
        {
            return false;
        }

        List keys = register.getKeys();
        for (int i = 0; i < count; i++)
        {
            playbackKeys(editor, context, keys, 0);
        }

        lastRegister = reg;

        return true;
    }

    /**
     * This plays back the last register that was executed, if any.
     * @param editor The editr to play the macro in
     * @param context The data context
     * @param count The number of times to execute the macro
     * @return true if able to play the macro, false in no previous playback
     */
    public boolean playbackLastRegister(Editor editor, DataContext context, int count)
    {
        if (lastRegister != 0)
        {
            return playbackRegister(editor, context, lastRegister, count);
        }

        return false;
    }

    /**
     * This puts a single keystroke at the end of the event queue for playback
     * @param editor The editor to play the key in
     * @param context The data cotnext
     * @param keys The list of keys to playback
     * @param pos The position within the list for the specific key to queue
     */
    public void playbackKeys(final Editor editor, final DataContext context, final List keys, final int pos)
    {
        if (pos >= keys.size())
        {
            return;
        }

        // This took a while to get just right. The original approach has a loop that made a runnable for each
        // character. It worked except for one case - if the macro had a complete ex command, the editor did not
        // end up with the focus and I couldn't find anyway to get it to have focus. This approach was the only
        // solution. This makes the most sense now (of course it took hours of trial and error to come up with
        // this one). Each key gets added, one at a time, to the event queue. If a given key results in other
        // events getting queued, they get queued before the next key, just what would happen if the user was typing
        // the keys one at a time. With the old loop approach, all the keys got queued, then any events they caused
        // were queued - after the keys. This is what caused the problem.
        final KeyStroke stroke = (KeyStroke)keys.get(pos);
        final Runnable run = new Runnable() {
            public void run()
            {
                // Handle one keystroke then queue up the next key
                KeyHandler.getInstance().handleKey(editor, stroke, context);
                if (pos < keys.size())
                {
                    playbackKeys(editor, context, keys, pos + 1);
                }
            }
        };

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                CommandProcessor.getInstance().executeCommand((Project)context.getData(DataConstants.PROJECT), run, "foo", "bar");
            }
        });
    }

    private char lastRegister = 0;
}
