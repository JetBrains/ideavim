/*
 *  gnu/regexp/RETokenRange.java
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

final class RETokenRange extends REToken
{
    private char lo, hi;
    private boolean insens;

    RETokenRange(int subIndex, char lo, char hi, boolean ins)
    {
        super(subIndex);
        this.lo = (insens = ins) ? Character.toLowerCase(lo) : lo;
        this.hi = ins ? Character.toLowerCase(hi) : hi;
    }

    int getMinimumLength()
    {
        return 1;
    }

    boolean match(CharIndexed input, REMatch mymatch)
    {
        char c = input.charAt(mymatch.index);
        if (c == CharIndexed.OUT_OF_BOUNDS) return false;
        if (insens) c = Character.toLowerCase(c);
        if ((c >= lo) && (c <= hi))
        {
            ++mymatch.index;
            return next(input, mymatch);
        }
        return false;
    }

    void dump(StringBuffer os)
    {
        os.append(lo).append('-').append(hi);
    }
}

