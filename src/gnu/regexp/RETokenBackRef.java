/*
 *  gnu/regexp/RETokenBackRef.java
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

final class RETokenBackRef extends REToken
{
    private int num;
    private boolean insens;

    RETokenBackRef(int subIndex, int num, boolean insens)
    {
        super(subIndex);
        this.num = num;
        this.insens = insens;
    }

    // should implement getMinimumLength() -- any ideas?

    boolean match(CharIndexed input, REMatch mymatch)
    {
        int b,e;
        b = mymatch.start[num];
        e = mymatch.end[num];
        if ((b == -1) || (e == -1)) return false; // this shouldn't happen, but...
        for (int i = b; i < e; i++)
        {
            if (input.charAt(mymatch.index + i - b) != input.charAt(i))
            {
                return false;
            }
        }
        mymatch.index += e - b;
        return next(input, mymatch);
    }

    void dump(StringBuffer os)
    {
        os.append('\\').append(num);
    }
}


