/*
 *  gnu/regexp/REToken.java
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

abstract class REToken implements Serializable
{

    protected REToken next = null;
    protected REToken uncle = null;
    protected int subIndex;

    protected REToken(int subIndex)
    {
        this.subIndex = subIndex;
    }

    int getMinimumLength()
    {
        return 0;
    }

    void setUncle(REToken anUncle)
    {
        uncle = anUncle;
    }

    /** Returns true if the match succeeded, false if it failed. */
    abstract boolean match(CharIndexed input, REMatch mymatch);

    /** Returns true if the rest of the tokens match, false if they fail. */
    protected boolean next(CharIndexed input, REMatch mymatch)
    {
        if (next == null)
        {
            if (uncle == null)
            {
                return true;
            }
            else
            {
                return uncle.match(input, mymatch);
            }
        }
        else
        {
            return next.match(input, mymatch);
        }
    }

    boolean chain(REToken token)
    {
        next = token;
        return true; // Token was accepted
    }

    abstract void dump(StringBuffer os);

    void dumpAll(StringBuffer os)
    {
        dump(os);
        if (next != null) next.dumpAll(os);
    }
}
