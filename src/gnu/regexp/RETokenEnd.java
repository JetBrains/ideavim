/*
 *  gnu/regexp/RETokenEnd.java
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

final class RETokenEnd extends REToken
{
    /**
     * Indicates whether this token should match on a line break.
     */
    private String newline;

    RETokenEnd(int subIndex, String newline)
    {
        super(subIndex);
        this.newline = newline;
    }

    boolean match(CharIndexed input, REMatch mymatch)
    {
        char ch = input.charAt(mymatch.index);
        if (ch == CharIndexed.OUT_OF_BOUNDS)
            return ((mymatch.eflags & RE.REG_NOTEOL) > 0) ?
                false : next(input, mymatch);
        if (newline != null)
        {
            char z;
            int i = 0; // position in newline
            do
            {
                z = newline.charAt(i);
                if (ch != z) return false;
                ++i;
                ch = input.charAt(mymatch.index + i);
            }
            while (i < newline.length());

            return next(input, mymatch);
        }
        return false;
    }

    void dump(StringBuffer os)
    {
        os.append('$');
    }
}
