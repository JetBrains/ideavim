package com.maddyhome.idea.vim.group;

/*
* IdeaVim - A Vim emulator plugin for IntelliJ Idea
* Copyright (C) 2003 Rick Maddy
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 *
 */
public class ProcessGroup extends AbstractActionGroup
{
    public ProcessGroup()
    {
    }

    public String getLastCommand()
    {
        return lastCommand;
    }

    public boolean executeFilter(Editor editor, DataContext context, TextRange range, String command)
    {
        logger.debug("command=" + command);
        char[] chars = editor.getDocument().getChars();
        CharArrayReader car = new CharArrayReader(chars, range.getStartOffset(),
            range.getEndOffset() - range.getStartOffset());
        StringWriter sw = new StringWriter();

        try
        {
            logger.debug("about to create filter");
            Process filter = Runtime.getRuntime().exec(command);
            logger.debug("filter created");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(filter.getOutputStream()));
            logger.debug("sending text");
            copy(car, writer);
            writer.close();
            logger.debug("sent");

            BufferedReader reader = new BufferedReader(new InputStreamReader(filter.getInputStream()));
            logger.debug("getting result");
            copy(reader, sw);
            sw.close();
            logger.debug("received");

            editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), sw.toString());

            lastCommand = command;
            
            return true;
        }
        catch (IOException e)
        {
            // TODO
            return false;
        }
    }

    private void copy(Reader from, Writer to) throws IOException
    {
        char[] buf = new char[2048];
        int cnt;
        while ((cnt = from.read(buf)) != -1)
        {
            logger.debug("buf="+buf);
            to.write(buf, 0, cnt);
        }
    }

    private String lastCommand;

    private static Logger logger = Logger.getInstance(ProcessGroup.class.getName());
}
