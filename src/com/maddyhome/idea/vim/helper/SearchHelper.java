package com.maddyhome.idea.vim.helper;

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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.option.ListOption;
import com.maddyhome.idea.vim.option.OptionChangeEvent;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.Options;

import java.util.List;

/**
 * Helper methods for searching text
 */
public class SearchHelper
{
    public static boolean anyNonWhitespace(Editor editor, int offset, int dir)
    {
        int start;
        int end;
        if (dir > 0)
        {
            start = offset + 1;
            end = EditorHelper.getLineEndForOffset(editor, offset);
        }
        else
        {
            start = EditorHelper.getLineStartForOffset(editor, offset);
            end = offset - 1;
        }

        char[] chars = editor.getDocument().getChars();
        for (int i = start; i <= end; i++)
        {
            if (!Character.isWhitespace(chars[i]))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * This looks on the current line, starting at the cursor postion for one of {, }, (, ), [, or ]. It then searches
     * forward or backward, as appropriate for the associated match pair. String in double quotes are skipped over.
     * Single characters in single quotes are skipped too.
     *
     * @param editor The editor to search in
     * @return The offset within the editor of the found character or -1 if no match was found or none of the characters
     *         were found on the remainder of the current line.
     */
    public static int findMatchingPairOnCurrentLine(Editor editor)
    {
        int res = -1;
        int line = EditorHelper.getCurrentLogicalLine(editor);
        int end = EditorHelper.getLineEndOffset(editor, line, true);
        char[] chars = editor.getDocument().getChars();
        int pos = editor.getCaretModel().getOffset();
        int loc = -1;
        // Search the remainder of the current line for one of the candicate characters
        while (pos < end)
        {
            loc = getPairChars().indexOf(chars[pos]);
            if (loc >= 0)
            {
                break;
            }

            pos++;
        }

        // If we found one ...
        if (loc >= 0)
        {
            // What direction should we go now (-1 is backward, 1 is forward)
            int dir = loc % 2 == 0 ? 1 : -1;
            // Which character did we find and which should we now search for
            char found = getPairChars().charAt(loc);
            char match = getPairChars().charAt(loc + dir);
            boolean inString = false;
            int stack = 0;
            pos += dir;
            // Search to start or end of file, as appropriate
            while (pos >= 0 && pos < chars.length)
            {
                // If we found a match and we're not in a string...
                if (chars[pos] == match && !inString)
                {
                    // We found our match
                    if (stack == 0)
                    {
                        res = pos;
                        break;
                    }
                    // Found the character but it "closes" a different pair
                    else
                    {
                        stack--;
                    }
                }
                // We found another character like our original - belongs to another pair
                else if (chars[pos] == found && !inString)
                {
                    stack++;
                }
                // We found the start/end of a string
                else if (chars[pos] == '"' && (pos == 0 || chars[pos - 1] != '\\'))
                {
                    inString = !inString;
                }
                // We found character literal - skip it
                else if (chars[pos] == '\'')
                {
                    int tmp = pos + 2 * dir;
                    if (tmp < chars.length && chars[tmp] == '\'')
                    {
                        pos = tmp;
                    }
                }
                // End of line - mark not in a string any more (in case we started in the middle of one
                else if (chars[pos] == '\n')
                {
                    inString = false;
                }

                pos += dir;
            }
        }

        return res;
    }

    public static int findNextCamelStart(Editor editor, int count)
    {
        char[] chars = editor.getDocument().getChars();
        int pos = editor.getCaretModel().getOffset();
        int size = EditorHelper.getFileSize(editor);

        int found = 0;
        int step = count >= 0 ? 1 : -1;
        if (pos < 0 || pos >= size)
        {
            return pos;
        }

        int res = pos;
        pos += step;
        while (pos >= 0 && pos < size && found < Math.abs(count))
        {
            if (Character.isUpperCase(chars[pos]))
            {
                if ((pos == 0 || !Character.isUpperCase(chars[pos - 1])) ||
                    (pos == size - 1 || Character.isLowerCase(chars[pos + 1])))
                {
                    res = pos;
                    found++;
                }
            }
            else if (Character.isLowerCase(chars[pos]))
            {
                if (pos == 0 || !Character.isLetter(chars[pos - 1]))
                {
                    res = pos;
                    found++;
                }
            }
            else if (Character.isDigit(chars[pos]))
            {
                if (pos == 0 || !Character.isDigit(chars[pos - 1]))
                {
                    res = pos;
                    found++;
                }
            }

            pos += step;
        }

        if (found < Math.abs(count))
        {
            res = -1;
        }

        return res;
    }

    public static int findNextCamelEnd(Editor editor, int count)
    {
        char[] chars = editor.getDocument().getChars();
        int pos = editor.getCaretModel().getOffset();
        int size = EditorHelper.getFileSize(editor);

        int found = 0;
        int step = count >= 0 ? 1 : -1;
        if (pos < 0 || pos >= size)
        {
            return pos;
        }

        int res = pos;
        pos += step;
        while (pos >= 0 && pos < size && found < Math.abs(count))
        {
            if (Character.isUpperCase(chars[pos]))
            {
                if (pos == size - 1 || !Character.isLetter(chars[pos + 1]) ||
                    (Character.isUpperCase(chars[pos + 1]) && pos <= size - 2 && Character.isLowerCase(chars[pos + 2])))
                {
                    res = pos;
                    found++;
                }
            }
            else if (Character.isLowerCase(chars[pos]))
            {
                if (pos == size - 1 || !Character.isLowerCase(chars[pos + 1]))
                {
                    res = pos;
                    found++;
                }
            }
            else if (Character.isDigit(chars[pos]))
            {
                if (pos == size - 1 || !Character.isDigit(chars[pos + 1]))
                {
                    res = pos;
                    found++;
                }
            }

            pos += step;
        }

        if (found < Math.abs(count))
        {
            res = -1;
        }

        return res;
    }

    /**
     * This finds the offset to the start of the next/previous word/WORD.
     *
     * @param editor The editor to find the words in
     * @param count The number of words to skip. Negative for backward searches
     * @param skipPunc If true then find WORD, if false then find word
     * @return The offset of the match
     */
    public static int findNextWord(Editor editor, int count, boolean skipPunc)
    {
        char[] chars = editor.getDocument().getChars();
        int pos = editor.getCaretModel().getOffset();
        int size = EditorHelper.getFileSize(editor);

        return findNextWord(chars, pos, size, count, skipPunc);
    }

    public static int findNextWord(char[] chars, int pos, int size, int count, boolean skipPunc)
    {
        int step = count >= 0 ? 1 : -1;
        count = Math.abs(count);

        int res = pos;
        for (int i = 0; i < count; i++)
        {
            res = findNextWordOne(chars, res, size, step, skipPunc);
            if (res == pos || res == 0 || res == size - 1)
            {
                break;
            }
        }

        return res;
    }

    private static int findNextWordOne(char[] chars, int pos, int size, int step, boolean skipPunc)
    {
        boolean found = false;
        // For back searches, skip any current whitespace so we start at the end of a word
        if (step < 0 && pos > 0)
        {
            if (CharacterHelper.charType(chars[pos - 1], skipPunc) == CharacterHelper.TYPE_SPACE)
            {
                pos = skipSpace(chars, pos - 1, step, size) + 1;
            }
            if (CharacterHelper.charType(chars[pos], skipPunc) != CharacterHelper.charType(chars[pos - 1], skipPunc))
            {
                pos += step;
            }
        }
        int res = pos;
        if (pos < 0 || pos >= size)
        {
            return pos;
        }

        int type = CharacterHelper.charType(chars[pos], skipPunc);
        if (type == CharacterHelper.TYPE_SPACE && step < 0 && pos > 0)
        {
            type = CharacterHelper.charType(chars[pos - 1], skipPunc);
        }
        
        pos += step;
        while (pos >= 0 && pos < size && !found)
        {
            int newType = CharacterHelper.charType(chars[pos], skipPunc);
            if (newType != type)
            {
                if (newType == CharacterHelper.TYPE_SPACE && step >= 0)
                {
                    pos = skipSpace(chars, pos, step, size);
                    res = pos;
                }
                else if (step < 0)
                {
                    res = pos + 1;
                }
                else
                {
                    res = pos;
                }

                type = CharacterHelper.charType(chars[res], skipPunc);
                found = true;
            }

            pos += step;
        }

        if (found)
        {
            if (pos <= 0)
            {
                res = 0;
            }
            else if (pos >= size)
            {
                res = size - 1;
            }
        }

        return res;
    }

    /**
     * Find the word under the cursor or the next word to the right of the cursor on the current line.
     *
     * @param editor The editor to find the word in
     * @return The text range of the found word or null if there is no word under/after the cursor on the line
     */
    public static TextRange findWordUnderCursor(Editor editor)
    {
        char[] chars = editor.getDocument().getChars();
        int stop = EditorHelper.getLineEndOffset(editor, EditorHelper.getCurrentLogicalLine(editor), true);

        int pos = editor.getCaretModel().getOffset();
        int start = pos;
        int[] types = new int[]{CharacterHelper.TYPE_CHAR, CharacterHelper.TYPE_PUNC};
        for (int i = 0; i < 2; i++)
        {
            start = pos;
            int type = CharacterHelper.charType(chars[start], false);
            if (type == types[i])
            {
                // Search back for start of word
                while (start > 0 && CharacterHelper.charType(chars[start - 1], false) == types[i])
                {
                    start--;
                }
            }
            else
            {
                // Search forward for start of word
                while (start < stop && CharacterHelper.charType(chars[start], false) != types[i])
                {
                    start++;
                }
            }

            if (start != stop)
            {
                break;
            }
        }

        if (start == stop)
        {
            return null;
        }

        int end = findNextWordEnd(chars, start, stop, 1, false, false) + 1;

        return new TextRange(start, end);
    }

    /**
     * This finds the offset to the end of the next/previous word/WORD.
     *
     * @param editor The editor to search in
     * @param count The number of words to skip. Negative for backward searches
     * @param skipPunc If true then find WORD, if false then find word
     * @return The offset of match
     */
    public static int findNextWordEnd(Editor editor, int count, boolean skipPunc, boolean stayEnd)
    {
        char[] chars = editor.getDocument().getChars();
        int pos = editor.getCaretModel().getOffset();
        int size = EditorHelper.getFileSize(editor);

        return findNextWordEnd(chars, pos, size, count, skipPunc, stayEnd);
    }

    public static int findNextWordEnd(char[] chars, int pos, int size, int count, boolean skipPunc, boolean stayEnd)
    {
        int step = count >= 0 ? 1 : -1;
        count = Math.abs(count);

        int res = pos;
        for (int i = 0; i < count; i++)
        {
            res = findNextWordEndOne(chars, res, size, step, skipPunc, stayEnd);
            if (res == pos || res == 0 || res == size - 1)
            {
                break;
            }
        }

        return res;
    }

    private static int findNextWordEndOne(char[] chars, int pos, int size, int step, boolean skipPunc, boolean stayEnd)
    {
        boolean found = false;
        // For forward searches, skip any current whitespace so we start at the start of a word
        if (step > 0 && pos < size - 1)
        {
            /*
            if (CharacterHelper.charType(chars[pos + step], false) == CharacterHelper.TYPE_SPACE)
            {
                if (!stayEnd)
                {
                    pos += step;
                }
                pos = skipSpace(chars, pos, step, size);
            }
            */
            if (CharacterHelper.charType(chars[pos + 1], skipPunc) == CharacterHelper.TYPE_SPACE)
            {
                pos = skipSpace(chars, pos + 1, step, size) - 1;
            }
            if (CharacterHelper.charType(chars[pos], skipPunc) != CharacterHelper.charType(chars[pos + 1], skipPunc))
            {
                pos += step;
            }
        }
        int res = pos;
        if (pos < 0 || pos >= size)
        {
            return pos;
        }
        int type = CharacterHelper.charType(chars[pos], skipPunc);
        if (type == CharacterHelper.TYPE_SPACE && step >= 0 && pos < size - 1)
        {
            type = CharacterHelper.charType(chars[pos + 1], skipPunc);
        }

        pos += step;
        while (pos >= 0 && pos < size && !found)
        {
            int newType = CharacterHelper.charType(chars[pos], skipPunc);
            if (newType != type)
            {
                if (step >= 0)
                {
                    res = pos - 1;
                }
                else if (newType == CharacterHelper.TYPE_SPACE && step < 0)
                {
                    pos = skipSpace(chars, pos, step, size);
                    res = pos;
                }
                else
                {
                    res = pos;
                }

                found = true;
            }

            pos += step;
        }

        if (found)
        {
            if (pos <= 0)
            {
                res = 0;
            }
            else if (pos >= size)
            {
                res = size - 1;
            }
        }

        return res;
    }

    /**
     * This skips whitespace starting with the supplied position.
     *
     * @param chars The text as a character array
     * @param offset The starting position
     * @param step The direction to move
     * @param size The size of the document
     * @return The new position. This will be the first non-whitespace character found
     */
    public static int skipSpace(char[] chars, int offset, int step, int size)
    {
        while (offset >= 0 && offset < size)
        {
            if (CharacterHelper.charType(chars[offset], false) != CharacterHelper.TYPE_SPACE)
            {
                break;
            }

            offset += step;
        }

        return offset;
    }

    /**
     * This locates the position with the document of the count'th occurence of ch on the current line
     *
     * @param editor The editor to search in
     * @param count The number of occurences of ch to locate. Negative for backward searches
     * @param ch The character on the line to find
     * @return The document offset of the matching character match, -1
     */
    public static int findNextCharacterOnLine(Editor editor, int count, char ch)
    {
        int line = EditorHelper.getCurrentLogicalLine(editor);
        int start = EditorHelper.getLineStartOffset(editor, line);
        int end = EditorHelper.getLineEndOffset(editor, line, true);
        char[] chars = editor.getDocument().getChars();
        int found = 0;
        int step = count >= 0 ? 1 : -1;
        int pos = editor.getCaretModel().getOffset() + step;
        while (pos >= start && pos < end && pos >= 0 && pos < chars.length)
        {
            if (chars[pos] == ch)
            {
                found++;
                if (found == Math.abs(count))
                {
                    break;
                }
            }
            pos += step;
        }

        if (found == Math.abs(count))
        {
            return pos;
        }
        else
        {
            return -1;
        }
    }

    public static int findNextParagraph(Editor editor, int count)
    {
        int offset = editor.getCaretModel().getOffset();
        char[] chars = editor.getDocument().getChars();
        int found = 0;
        int step = count >= 0 ? 1 : -1;
        int pos = offset;
        int res = offset;
        int cnt = 0;
        pos = skipNewlines(chars, pos, step);
        while (pos >= 0 && pos < chars.length && found < Math.abs(count))
        {
            if (chars[pos] == '\n')
            {
                cnt++;
                if (cnt == 2)
                {
                    found++;
                    res = pos + (step == -1 ? 1 : 0);
                    pos = skipNewlines(chars, pos, step) - step;
                }
            }
            else
            {
                cnt = 0;
            }

            pos += step;
        }

        if (found < Math.abs(count))
        {
            if (pos <= 0)
            {
                res = 0;
            }
            else if (pos >= chars.length)
            {
                res = chars.length - 1;
            }
        }

        return res;
    }

    private static int skipNewlines(char[] chars, int offset, int step)
    {
        while (offset >= 0 && offset < chars.length && chars[offset] == '\n')
        {
            offset += step;
        }

        return offset;
    }

    private static String getPairChars()
    {
        if (pairsChars == null)
        {
            ListOption lo = (ListOption)Options.getInstance().getOption("matchpairs");
            pairsChars = parseOption(lo);

            lo.addOptionChangeListener(new OptionChangeListener()
            {
                public void valueChange(OptionChangeEvent event)
                {
                    pairsChars = parseOption((ListOption)event.getOption());
                }
            });
        }

        return pairsChars;
    }

    private static String parseOption(ListOption option)
    {
        List vals = option.values();
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < vals.size(); i++)
        {
            String s = (String)vals.get(i);
            if (s.length() == 3)
            {
                res.append(s.charAt(0)).append(s.charAt(2));
            }
        }

        return res.toString();
    }

    private static String pairsChars = null;

    private static Logger logger = Logger.getInstance(SearchHelper.class.getName());
}
