/*
 *  gnu/regexp/CharIndexedCharArray.java
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

import java.io.Serializable;

class CharIndexedCharArray implements CharIndexed, Serializable
{
    private char[] s;
    private int anchor;

    CharIndexedCharArray(char[] str, int index)
    {
        s = str;
        anchor = index;
    }

    public char charAt(int index)
    {
        int pos = anchor + index;
        return ((pos < s.length) && (pos >= 0)) ? s[pos] : OUT_OF_BOUNDS;
    }

    public boolean isValid()
    {
        return (anchor < s.length);
    }

    public boolean move(int index)
    {
        return ((anchor += index) < s.length);
    }
}
