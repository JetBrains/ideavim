/*
 *  gnu/regexp/RETokenStart.java
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

class RETokenStart extends REToken
{
    private String newline; // matches after a newline

    RETokenStart(int subIndex, String newline)
    {
        super(subIndex);
        this.newline = newline;
    }

    boolean match(CharIndexed input, REMatch mymatch)
    {
        // charAt(index-n) may be unknown on a Reader/InputStream. FIXME
        // Match after a newline if in multiline mode

        if (newline != null)
        {
            int len = newline.length();
            if (mymatch.offset >= len)
            {
                boolean found = true;
                char z;
                int i = 0; // position in REToken.newline
                char ch = input.charAt(mymatch.index - len);
                do
                {
                    z = newline.charAt(i);
                    if (ch != z)
                    {
                        found = false;
                        break;
                    }
                    ++i;
                    ch = input.charAt(mymatch.index - len + i);
                }
                while (i < len);

                if (found) return next(input, mymatch);
            }
        }

        // Don't match at all if REG_NOTBOL is set.
        if ((mymatch.eflags & RE.REG_NOTBOL) > 0) return false;

        if ((mymatch.eflags & RE.REG_ANCHORINDEX) > 0)
            return (mymatch.anchor == mymatch.offset) ?
                next(input, mymatch) : false;
        else
            return ((mymatch.index == 0) && (mymatch.offset == 0)) ?
                next(input, mymatch) : false;
    }

    void dump(StringBuffer os)
    {
        os.append('^');
    }
}
