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
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.helper.SearchHelper;
import com.maddyhome.idea.vim.option.Options;
import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RESyntax;
import java.nio.CharBuffer;
import java.util.StringTokenizer;
import java.awt.Color;
import java.text.NumberFormat;
import java.text.ParsePosition;
import javax.swing.JOptionPane;

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

    public String getLastSearch()
    {
        return lastSearch;
    }

    public String getLastPattern()
    {
        return lastPattern;
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

        int pflags = RE.REG_MULTILINE;
        // If the user set the i flag or they didn't set the I flag but the ignorecase option is set, then ignore case
        if ((flags & IGNORE_CASE) != 0 || ((flags & NO_IGNORE_CASE) == 0 && shouldIgnoreCase(pattern, false)))
        {
            pflags |= RE.REG_ICASE;
        }

        RE sp;
        if (pattern.length() == 0)
        {
            if ((flags & REUSE) != 0)
            {
                pattern = lastSearch;
            }
            else
            {
                pattern = lastPattern;
            }
        }

        try
        {
            sp = new RE(pattern, pflags, RESyntax.RE_SYNTAX_ED);
        }
        catch (Exception e)
        {
            return false;
        }

        lastSearch = pattern;
        lastPattern = pattern;

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
            REMatch matcher = sp.getMatch(buf, from);
            found = matcher != null;
            if (found)
            {
                int spos = matcher.getStartIndex();
                int epos = matcher.getEndIndex();
                String match = matcher.substituteInto(replace);
                logger.debug("found match[" + spos + "," + epos + "] - replace " + match);

                int line = editor.offsetToLogicalPosition(start + spos).line;
                if ((flags & GLOBAL) != 0 || line != lastLine)
                {
                    boolean doReplace = true;
                    if ((flags & CONFIRM) != 0 && checkConfirm)
                    {
                        editor.getSelectionModel().setSelection(start + spos, start + epos);
                        int choice = JOptionPane.showOptionDialog(null, "Replace with " + match + " ?",
                            "Confirm Replace", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            getConfirmButtons(), null);
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

    private boolean shouldIgnoreCase(String pattern, boolean noSmartCase)
    {
        boolean sc = noSmartCase ? false : Options.getInstance().isSet("smartcase");
        boolean ic = Options.getInstance().isSet("ignorecase");
        if (ic && !(sc && StringHelper.containsUpperCase(pattern)))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static int argsToFlags(String args)
    {
        int res = 0;
        boolean global = Options.getInstance().isSet("gdefault");
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
                    global = !global;
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

        if (global)
        {
            res |= GLOBAL;
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

    public int search(Editor editor, DataContext context, String command, int count, int flags, boolean moveCursor)
    {
        int res = search(editor, context, command, editor.getCaretModel().getOffset(), count, flags);

        if (res != -1 && moveCursor)
        {
            CommandGroups.getInstance().getMark().saveJumpLocation(editor, context);
            MotionGroup.moveCaret(editor, context, res);
        }

        return res;
    }

    public int search(Editor editor, DataContext context, String command, int startOffset, int count, int flags)
    {
        int dir = 1;
        char type = '/';
        String pattern = lastSearch;
        String offset = lastOffset;
        if ((flags & Command.FLAG_SEARCH_REV) != 0)
        {
            dir = -1;
            type = '?';
        }

        // TODO - broke - doesn't handle ranges and backslashes
        if (command.length() > 0)
        {
            StringTokenizer tokenizer = new StringTokenizer(command, Character.toString(type));
            if (command.charAt(0) != type)
            {
                pattern = tokenizer.nextToken();
                logger.debug("pattern=" + pattern);
                if (!tokenizer.hasMoreTokens())
                {
                    logger.debug("no offset");
                    offset = "";
                }
                else
                {
                    offset = tokenizer.nextToken("\n").substring(1);
                    logger.debug("offset=" + offset);
                }
            }
            else if (command.length() == 1)
            {
                offset = "";
            }
            else
            {
                offset = tokenizer.nextToken("\n").substring(1);
                logger.debug("offset=" + offset);
            }
        }

        lastSearch = pattern;
        lastOffset = offset;
        lastDir = dir;

        logger.debug("lastSearch=" + lastSearch);
        logger.debug("lastOffset=" + lastOffset);
        logger.debug("lastDir=" + lastDir);

        int res = findItOffset(editor, context, startOffset, count, lastDir, false);

        return res;
    }

    public int searchWord(Editor editor, DataContext context, int count, boolean whole, int dir)
    {
        TextRange range = SearchHelper.findWordUnderCursor(editor);
        if (range == null)
        {
            return -1;
        }

        StringBuffer pattern = new StringBuffer();
        if (whole)
        {
            pattern.append("\\<");
        }
        pattern.append(EditorHelper.getText(editor, range.getStartOffset(), range.getEndOffset()));
        if (whole)
        {
            pattern.append("\\>");
        }

        MotionGroup.moveCaret(editor, context, range.getStartOffset());

        lastSearch = pattern.toString();
        lastOffset = "";
        lastDir = dir;

        int res = findItOffset(editor, context, editor.getCaretModel().getOffset(), count, lastDir, true);
        if (res != -1)
        {
            MotionGroup.moveCaret(editor, context, res);
        }

        return res;
    }

    public int searchNext(Editor editor, DataContext context, int count)
    {
        return findItOffset(editor, context, editor.getCaretModel().getOffset(), count, lastDir, false);
    }

    public int searchPrevious(Editor editor, DataContext context, int count)
    {
        return findItOffset(editor, context, editor.getCaretModel().getOffset(), count, -lastDir, false);
    }

    private int findItOffset(Editor editor, DataContext context, int startOffset, int count, int dir,
        boolean noSmartCase)
    {
        TextRange range = findIt(editor, context, startOffset, count, dir, noSmartCase);
        if (range == null)
        {
            return -1;
        }

        ParsePosition pp = new ParsePosition(0);
        int res = range.getStartOffset();

        if (lastOffset.length() == 0)
        {
            return range.getStartOffset();
        }
        else if (Character.isDigit(lastOffset.charAt(0)) || lastOffset.charAt(0) == '+' || lastOffset.charAt(0) == '-')
        {
            int lineOffset = 0;
            if (lastOffset.equals("+"))
            {
                lineOffset = 1;
            }
            else if (lastOffset.equals("-"))
            {
                lineOffset = -1;
            }
            else
            {
                if (lastOffset.charAt(0) == '+')
                {
                    lastOffset = lastOffset.substring(1);
                }
                NumberFormat nf = NumberFormat.getIntegerInstance();
                pp = new ParsePosition(0);
                Number num = nf.parse(lastOffset, pp);
                if (num != null)
                {
                    lineOffset = num.intValue();
                }
            }

            int line = editor.offsetToLogicalPosition(range.getStartOffset()).line;
            int newLine = EditorHelper.normalizeLine(editor, line + lineOffset);

            res = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor, newLine);
        }
        else if ("ebs".indexOf(lastOffset.charAt(0)) != -1)
        {
            int charOffset = 0;
            if (lastOffset.length() >= 2)
            {
                if ("+-".indexOf(lastOffset.charAt(1)) != -1)
                {
                    charOffset = 1;
                }
                NumberFormat nf = NumberFormat.getIntegerInstance();
                pp = new ParsePosition(lastOffset.charAt(1) == '+' ? 2 : 1);
                Number num = nf.parse(lastOffset, pp);
                if (num != null)
                {
                    charOffset = num.intValue();
                }
            }

            int base = range.getStartOffset();
            if (lastOffset.charAt(0) == 'e')
            {
                base = range.getEndOffset() - 1;
            }

            res = Math.max(0, Math.min(base + charOffset, EditorHelper.getFileSize(editor) - 1));
        }

        int ppos = pp.getIndex();
        if (ppos < lastOffset.length() - 1 && lastOffset.charAt(ppos) == ';')
        {
            int flags;
            if (lastOffset.charAt(ppos + 1) == '/')
            {
                flags = Command.FLAG_SEARCH_FWD;
            }
            else if (lastOffset.charAt(ppos + 1) == '?')
            {
                flags = Command.FLAG_SEARCH_REV;
            }
            else
            {
                return res;
            }

            if (lastOffset.length() - ppos > 2)
            {
                ppos++;
            }
            
            res = search(editor, context, lastOffset.substring(ppos + 1), res, 1, flags);

            return res;
        }
        else
        {
            return res;
        }
    }

    private TextRange findIt(Editor editor, DataContext context, int startOffset, int count, int dir,
        boolean noSmartCase)
    {
        TextRange res = null;

        if (lastSearch == null || lastSearch.length() == 0)
        {
            return res;
        }

        int pflags = RE.REG_MULTILINE;
        if (shouldIgnoreCase(lastSearch, noSmartCase))
        {
            pflags |= RE.REG_ICASE;
        }
        RE sp;
        try
        {
            sp = new RE(lastSearch, pflags, RESyntax.RE_SYNTAX_ED);
        }
        catch (REException e)
        {
            logger.debug("bad pattern: " + lastSearch);
            return res;
        }

        int extra_col = 1;
        int startcol = -1;
        boolean found = false;
        boolean match_ok = true;
        LogicalPosition pos = editor.offsetToLogicalPosition(startOffset);
        LogicalPosition endpos = null;
        REMatch match = null;

        do	/* loop for count */
        {
            LogicalPosition start_pos = pos;	/* remember start pos for detecting no match */
            found = false;		/* default: not found */
            boolean at_first_line = true;	/* default: start in first line */
            if (pos.line == -1)	/* correct lnum for when starting in line 0 */
            {
                pos = new LogicalPosition(0, 0);
                at_first_line = false;  /* not in first line now */
            }

            /*
            * Start searching in current line, unless searching backwards and
            * we're in column 0.
            */
            int lnum;
            if (dir == -1 && start_pos.column == 0)
            {
                lnum = pos.line - 1;
                at_first_line = false;
            }
            else
                lnum = pos.line;

            for (int loop = 0; loop <= 1; ++loop)   /* loop twice if 'wrapscan' set */
            {
                for ( ; lnum >= 0 && lnum < EditorHelper.getLineCount(editor);
                      lnum += dir, at_first_line = false)
                {
                    /*
                    * Look for a match somewhere in the line.
                    */
                    int first_lnum = lnum;
                    /*
                    nmatched = vim_regexec_multi(&regmatch, win, buf,
                        lnum, (colnr_T)0);
                    */
                    match = sp.getMatch(EditorHelper.getLineBuffer(editor, lnum), 0);
                    int nmatched = match == null ? 0 : 1;
                    /* Abort searching on an error (e.g., out of stack). */
                    /*
                    if (called_emsg)
                        break;
                    */
                    if (nmatched > 0)
                    {
                        /* match may actually be in another line when using \zs */
                        //lnum += regmatch.startpos[0].lnum;
                        int ptr = EditorHelper.getLineStartOffset(editor, lnum); //ptr = ml_get_buf(buf, lnum, false);
                        startcol = match.getStartIndex(); //regmatch.startpos[0].col;
                        endpos = new LogicalPosition(lnum, match.getEndIndex()); //endpos = regmatch.endpos[0];

                        /*
                        * Forward search in the first line: match should be after
                        * the start position. If not, continue at the end of the
                        * match (this is vi compatible) or on the next char.
                        */
                        if (dir == 1 && at_first_line)
                        {
                            match_ok = true;
                            /*
                            * When match lands on a NUL the cursor will be put
                            * one back afterwards, compare with that position,
                            * otherwise "/$" will get stuck on end of line.
                            */
                            while (startcol < start_pos.column + extra_col)
                            {
                                int matchcol = startcol;
                                if (matchcol < EditorHelper.getLineLength(editor, lnum))//(ptr[matchcol] != NUL)
                                {
                                        ++matchcol;
                                }
                                if (matchcol == EditorHelper.getLineLength(editor, lnum))//ptr[matchcol] == NUL
                                {
                                    match_ok = false;
                                    break;
                                }
                                match = sp.getMatch(EditorHelper.getLineBuffer(editor, lnum), matchcol);
                                nmatched = match == null ? 0 : 1;
                                if (nmatched == 0)
                                {
                                    match_ok = false;
                                    break;
                                }
                                startcol = match.getStartIndex(); //regmatch.startpos[0].col;
                                endpos = new LogicalPosition(lnum, match.getEndIndex()); //endpos = regmatch.endpos[0];

                                    /* Need to get the line pointer again, a
                                    * multi-line search may have made it invalid. */
                                    //ptr = ml_get_buf(buf, lnum, false);
                            }
                            if (!match_ok)
                                continue;
                        }
                        if (dir == -1)
                        {
                            /*
                            * Now, if there are multiple matches on this line,
                            * we have to get the last one. Or the last one before
                            * the cursor, if we're on that line.
                            * When putting the new cursor at the end, compare
                            * relative to the end of the match.
                            */
                            match_ok = false;
                            for (;;)
                            {
                                if (!at_first_line || (match.getEndIndex() + extra_col <= start_pos.column))
                                {
                                    /* Remember this position, we use it if it's
                                    * the last match in the line. */
                                    match_ok = true;
                                    startcol = match.getStartIndex(); //regmatch.startpos[0].col;
                                    endpos = new LogicalPosition(lnum, match.getEndIndex());
                                    //endpos = regmatch.endpos[0];
                                }
                                else
                                    break;

                                /*
                                * We found a valid match, now check if there is
                                * another one after it.
                                * If vi-compatible searching, continue at the end
                                * of the match, otherwise continue one position
                                * forward.
                                */
                                int matchcol = startcol;
                                if (matchcol < EditorHelper.getLineLength(editor, lnum))//(ptr[matchcol] != NUL)
                                {
                                    ++matchcol;
                                }
                                if (matchcol == EditorHelper.getLineLength(editor, lnum))//ptr[matchcol] == NUL
                                {
                                    break;
                                }
                                match = sp.getMatch(EditorHelper.getLineBuffer(editor, lnum), matchcol);
                                nmatched = match == null ? 0 : 1;
                                if (nmatched == 0)
                                {
                                    break;
                                }

                                /* Need to get the line pointer again, a
                                * multi-line search may have made it invalid. */
                                //ptr = ml_get_buf(buf, lnum, false);
                            }

                            /*
                            * If there is only a match after the cursor, skip
                            * this match.
                            */
                            if (!match_ok)
                                continue;
                        }

                        pos = new LogicalPosition(lnum, startcol);
                        found = true;

                        /* Set variables used for 'incsearch' highlighting. */
                        //search_match_lines = endpos.lnum - (lnum - first_lnum);
                        //search_match_endcol = endpos.col;
                        break;
                    }
                    //line_breakcheck();	/* stop if ctrl-C typed */
                    //if (got_int)
                    //    break;

                    /* Cancel searching if a character was typed.  Used for
                    * 'incsearch'.  Don't check too often, that would slowdown
                    * searching too much. */
                    /*
                    if ((options & SEARCH_PEEK)
                        && ((lnum - pos.lnum) & 0x3f) == 0
                        && char_avail())
                    {
                        break_loop = true;
                        break;
                    }
                    */

                    if (loop == 1 && lnum == start_pos.line)
                        break;	    /* if second loop, stop where started */
                }
                at_first_line = false;

                /*
                * stop the search if wrapscan isn't set, after an interrupt and
                * after a match
                */
                if (found)
                    break;

                /*
                * If 'wrapscan' is set we continue at the other end of the file.
                * If 'shortmess' does not contain 's', we give a message.
                * This message is also remembered in keep_msg for when the screen
                * is redrawn. The keep_msg is cleared whenever another message is
                * written.
                */
                if (dir == -1)    /* start second loop at the other end */
                {
                    lnum = EditorHelper.getLineCount(editor) - 1;//buf.b_ml.ml_line_count;
                    /*
                    if (!shortmess(SHM_SEARCH) && (options & SEARCH_MSG))
                        give_warning((char_u *)_(top_bot_msg), true);
                    */
                }
                else
                {
                    lnum = 0;
                    /*
                    if (!shortmess(SHM_SEARCH) && (options & SEARCH_MSG))
                        give_warning((char_u *)_(bot_top_msg), true);
                    */
                }

                if (!Options.getInstance().isSet("wrapscan"))
                {
                    break;
                }
            }
            /*
            if (got_int || called_emsg || break_loop)
                break;
            */
        }
        while (--count > 0 && found);   /* stop after count matches or no match */

        //vim_free(regmatch.regprog);

        if (!found)		    /* did not find it */
        {
            /*
            if (got_int)
                EMSG(_(e_interr));
            else if ((options & SEARCH_MSG) == SEARCH_MSG)
            {
                if (p_ws)
                    EMSG2(_(e_patnotf2), mr_pattern);
                else if (lnum == 0)
                    EMSG2(_("E384: search hit TOP without match for: %s"), mr_pattern);
                else
                    EMSG2(_("E385: search hit BOTTOM without match for: %s"), mr_pattern);
            }
            */
            res = null; //return FAIL;
        }
        else
        {
            res = new TextRange(editor.logicalPositionToOffset(pos), editor.logicalPositionToOffset(endpos));
            //highlightMatch(editor, res.getStartOffset(), res.getEndOffset());
        }

        return res;
    }

    private void highlightMatch(Editor editor, int start, int end)
    {
        editor.getMarkupModel().addRangeHighlighter(start, end, HighlighterLayer.SELECTION,
            new TextAttributes(Color.BLACK, Color.YELLOW, null, null, 0), HighlighterTargetArea.EXACT_RANGE);
    }

    private String lastSearch;
    private String lastPattern;
    private String lastReplace;
    private String lastOffset;
    private int lastDir;
    private int lastFlags;
    private Object[] confirmBtns;

    private static Logger logger = Logger.getInstance(SearchGroup.class.getName());
}
