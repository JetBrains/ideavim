/*
 *  gnu/regexp/RETokenWordBoundary.java
 *  Copyright (C) 2001 Wes Biggs
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published
 *  by the Free Software Foundation; either version 2.1 of the License, or
 *  (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package gnu.regexp;

/**
 * Represents a combination lookahead/lookbehind for POSIX [:alnum:].
 */
final class RETokenWordBoundary extends REToken
{
    private boolean negated;
    private int where;
    static final int BEGIN = 1;
    static final int END = 2;

    RETokenWordBoundary(int subIndex, int where, boolean negated)
    {
        super(subIndex);
        this.where = where;
        this.negated = negated;
    }

    boolean match(CharIndexed input, REMatch mymatch)
    {
        // Word boundary means input[index-1] was a word character
        // and input[index] is not, or input[index] is a word character
        // and input[index-1] was not
        //  In the string "one two three", these positions match:
        //  |o|n|e| |t|w|o| |t|h|r|e|e|
        //  ^     ^ ^     ^ ^         ^
        boolean after = false;  // is current character a letter or digit?
        boolean before = false; // is previous character a letter or digit?
        char ch;

        // TODO: Also check REG_ANCHORINDEX vs. anchor
        if (((mymatch.eflags & RE.REG_ANCHORINDEX) != RE.REG_ANCHORINDEX)
            || (mymatch.offset + mymatch.index > mymatch.anchor))
        {
            if ((ch = input.charAt(mymatch.index - 1)) != CharIndexed.OUT_OF_BOUNDS)
            {
                before = Character.isLetterOrDigit(ch) || (ch == '_');
            }
        }

        if ((ch = input.charAt(mymatch.index)) != CharIndexed.OUT_OF_BOUNDS)
        {
            after = Character.isLetterOrDigit(ch) || (ch == '_');
        }

        // if (before) and (!after), we're at end (\>)
        // if (after) and (!before), we're at beginning (\<)
        boolean doNext = false;

        if ((where & BEGIN) == BEGIN)
        {
            doNext = after && !before;
        }
        if ((where & END) == END)
        {
            doNext ^= before && !after;
        }

        if (negated) doNext = !doNext;

        return (doNext ? next(input, mymatch) : false);
    }

    void dump(StringBuffer os)
    {
        if (where == (BEGIN | END))
        {
            os.append(negated ? "\\B" : "\\b");
        }
        else if (where == BEGIN)
        {
            os.append("\\<");
        }
        else
        {
            os.append("\\>");
        }
    }
}
