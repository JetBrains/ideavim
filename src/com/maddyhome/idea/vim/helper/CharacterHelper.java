package com.maddyhome.idea.vim.helper;

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

/**
 * This helper class is used when working with various character level operations
 */
public class CharacterHelper
{
    public static final char CASE_TOGGLE = '~';
    public static final char CASE_UPPER = 'u';
    public static final char CASE_LOWER = 'l';

    public static final int TYPE_CHAR = 1;
    public static final int TYPE_PUNC = 2;
    public static final int TYPE_SPACE = 3;

    /**
     * This returns the type of the supplied character. The logic is as follows:<br>
     * If the character is whitespace, <code>TYPE_SPACE</code> is returned.<br>
     * If the punction is being skipped or the character is a letter, digit, or underscore, <code>TYPE_CHAR</code>
     * is returned.<br>
     * Otherwise <code>TYPE_PUNC</code> is returned.
     * @param ch The character to analyze
     * @param skipPunc True if punctuation is to be ignored, false if not
     * @return The type of the character
     */
    public static int charType(char ch, boolean skipPunc)
    {
        if (Character.isWhitespace(ch))
        {
            return TYPE_SPACE;
        }
        else if (skipPunc || Character.isLetterOrDigit(ch) || ch == '_')
        {
            return TYPE_CHAR;
        }
        else
        {
            return TYPE_PUNC;
        }
    }

    /**
     * Changes the case of the supplied character based on the supplied change type
     * @param ch The character to change
     * @param type One of <code>CASE_TOGGLE</code>, <code>CASE_UPPER</code>, or <code>CASE_LOWER</code>
     * @return The character with changed case or the original if not a letter
     */ 
    public static char changeCase(char ch, char type)
    {
        switch (type)
        {
            case CASE_TOGGLE:
                if (Character.isLowerCase(ch))
                {
                    ch = Character.toUpperCase(ch);
                }
                else if (Character.isUpperCase(ch))
                {
                    ch = Character.toLowerCase(ch);
                }
                break;
            case CASE_LOWER:
                ch = Character.toLowerCase(ch);
                break;
            case CASE_UPPER:
                ch = Character.toUpperCase(ch);
                break;
        }

        return ch;
    }
}
