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
import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JButton;

/**
 *
 */
public class SearchGroup extends AbstractActionGroup
{
    public static final int KEEP_FLAGS = 1;
    public static final int CONFIRM = 2;
    public static final int IGNORE_ERROR = 4;
    public static final int GLOBAL = 8;
    public static final int IGNORE_CASE = 16;
    public static final int NO_IGNORE_CASE = 32;
    public static final int PRINT = 64;
    public static final int REUSE = 128;

    public SearchGroup()
    {
    }

    public boolean searchAndReplace(Editor editor, DataContext context, TextRange range, String pattern, String replace,
        int flags)
    {
        boolean res = true;

        if ((flags & KEEP_FLAGS) != 0)
        {
            flags |= lastFlags;
        }
        lastFlags = flags;

        // TODO - default case sensitivity
        int pflags = Pattern.MULTILINE;
        if ((flags & IGNORE_CASE) != 0)
        {
            pflags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }

        Pattern sp;
        if (pattern.length() == 0)
        {
            if ((flags & REUSE) != 0)
            {
                sp = lastSearch;
            }
            else
            {
                sp = lastPattern;
            }
        }
        else
        {
            sp = Pattern.compile(pattern, pflags);
        }
        lastSearch = sp;
        lastPattern = sp;

        if (replace.equals("~"))
        {
            replace = lastReplace;
        }
        else
        {
            lastReplace = replace;
        }

        int start = range.getStartOffset();
        int end = range.getEndOffset();
        logger.debug("search range=[" + start + "," + end + "]");
        logger.debug("pattern="+pattern + ", replace="+replace);
        int from = 0;
        int lastMatch = -1;
        boolean found = true;
        int lastLine = -1;
        boolean checkConfirm = true;
        while (found)
        {
            char[] chars = editor.getDocument().getChars();
            CharBuffer buf = CharBuffer.wrap(chars, start, end - start);
            logger.debug("buf=" + buf);
            Matcher matcher = sp.matcher(buf);
            found = matcher.find(from);
            if (found)
            {
                int spos = matcher.start();
                int epos = matcher.end();
                StringBuffer sb = new StringBuffer();
                matcher.appendReplacement(sb, replace);
                logger.debug("sb=" + sb);
                String match = sb.substring(spos);
                logger.debug("found match[" + spos + "," + epos + "] - replace " + match);

                int line = editor.offsetToLogicalPosition(start + spos).line;
                if ((flags & GLOBAL) != 0 || line != lastLine)
                {
                    boolean doReplace = true;
                    if ((flags & CONFIRM) != 0 && checkConfirm)
                    {
                        editor.getSelectionModel().setSelection(start + spos, start + epos);
                        int choice = JOptionPane.showOptionDialog(null, "Replace with " + match + " ?", "Confirm Replace",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, getConfirmButtons(), null);
                        editor.getSelectionModel().removeSelection();
                        switch (choice)
                        {
                            case 0: // Yes
                                doReplace = true;
                                break;
                            case 1: // No
                                doReplace = false;
                                break;
                            case 2: // All
                                checkConfirm = false;
                                break;
                            case JOptionPane.CLOSED_OPTION:
                            case 3: // Quit
                                found = false;
                                doReplace = false;
                                break;
                            case 4: // Last
                                found = false;
                                doReplace = true;
                                break;
                        }
                    }

                    if (doReplace)
                    {
                        lastLine = line;
                        editor.getDocument().replaceString(start + spos, start + epos, match);
                        lastMatch = start + spos;
                    }
                }

                int diff = match.length() - (epos - spos);
                end += diff;
                from = epos + diff;
            }
        }

        if (lastMatch != -1)
        {
            MotionGroup.moveCaret(editor, context,
                CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor,
                editor.offsetToLogicalPosition(lastMatch).line));
        }

        return res;
    }

    public static int argsToFlags(String args)
    {
        int res = 0;
        for (int i = 0; i < args.length(); i++)
        {
            switch (args.charAt(i))
            {
                case '&':
                    res |= KEEP_FLAGS;
                    break;
                case 'c':
                    res |= CONFIRM;
                    break;
                case 'e':
                    res |= IGNORE_ERROR;
                    break;
                case 'g':
                    res |= GLOBAL;
                    break;
                case 'i':
                    res |= IGNORE_CASE;
                    break;
                case 'I':
                    res |= NO_IGNORE_CASE;
                    break;
                case 'p':
                    res |= PRINT;
                    break;
                case 'r':
                    res |= REUSE;
                    break;
            }
        }

        return res;
    }

    private Object[] getConfirmButtons()
    {
        if (confirmBtns == null)
        {
            // TODO - need buttons with mnemonics
            /*
            confirmBtns = new JButton[] {
                new JButton("Yes"),
                new JButton("No"),
                new JButton("All"),
                new JButton("Quit"),
                new JButton("Last")
            };

            confirmBtns[0].setMnemonic('Y');
            confirmBtns[1].setMnemonic('N');
            confirmBtns[2].setMnemonic('A');
            confirmBtns[3].setMnemonic('Q');
            confirmBtns[4].setMnemonic('L');
            */
            confirmBtns = new String[] { "Yes", "No", "All", "Quit", "Last" };
        }

        return confirmBtns;
    }

    private Pattern lastSearch;
    private Pattern lastPattern;
    private String lastReplace;
    private int lastFlags;
    private Object[] confirmBtns;

    private static Logger logger = Logger.getInstance(SearchGroup.class.getName());
}
