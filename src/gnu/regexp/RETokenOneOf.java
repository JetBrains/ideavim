/*
 *  gnu/regexp/RETokenOneOf.java
 *  Copyright (C) 1998-2001 Wes Biggs
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

import java.util.Vector;

final class RETokenOneOf extends REToken
{
    private Vector options;
    private boolean negative;

    // This constructor is used for convenience when we know the set beforehand,
    // e.g. \d --> new RETokenOneOf("0123456789",false, ..)
    //      \D --> new RETokenOneOf("0123456789",true, ..)

    RETokenOneOf(int subIndex, String optionsStr, boolean negative, boolean insens)
    {
        super(subIndex);
        options = new Vector();
        this.negative = negative;
        for (int i = 0; i < optionsStr.length(); i++)
            options.addElement(new RETokenChar(subIndex, optionsStr.charAt(i), insens));
    }

    RETokenOneOf(int subIndex, Vector options, boolean negative)
    {
        super(subIndex);
        this.options = options;
        this.negative = negative;
    }

    int getMinimumLength()
    {
        int min = Integer.MAX_VALUE;
        int x;
        for (int i = 0; i < options.size(); i++)
        {
            if ((x = ((REToken)options.elementAt(i)).getMinimumLength()) < min)
                min = x;
        }
        return min;
    }

    boolean match(CharIndexed input, REMatch mymatch)
    {
        if (negative && (input.charAt(mymatch.index) == CharIndexed.OUT_OF_BOUNDS))
            return false;

        REMatch newMatch = null;
        REMatch last = null;
        REToken tk;
        boolean isMatch;
        for (int i = 0; i < options.size(); i++)
        {
            tk = (REToken)options.elementAt(i);
            REMatch tryMatch = (REMatch)mymatch.clone();
            if (tk.match(input, tryMatch))
            { // match was successful
                if (negative) return false;

                if (next(input, tryMatch))
                {
                    // Add tryMatch to list of possibilities.
                    if (last == null)
                    {
                        newMatch = tryMatch;
                        last = tryMatch;
                    }
                    else
                    {
                        last.next = tryMatch;
                        last = tryMatch;
                    }
                } // next succeeds
            } // is a match
        } // try next option

        if (newMatch != null)
        {
            if (negative)
            {
                return false;
            }
            else
            {
                // set contents of mymatch equal to newMatch

                // try each one that matched
                mymatch.assignFrom(newMatch);
                return true;
            }
        }
        else
        {
            if (negative)
            {
                ++mymatch.index;
                return next(input, mymatch);
            }
            else
            {
                return false;
            }
        }

        // index+1 works for [^abc] lists, not for generic lookahead (--> index)
    }

    void dump(StringBuffer os)
    {
        os.append(negative ? "[^" : "(?:");
        for (int i = 0; i < options.size(); i++)
        {
            if (!negative && (i > 0)) os.append('|');
            ((REToken)options.elementAt(i)).dumpAll(os);
        }
        os.append(negative ? ']' : ')');
    }
}
