package com.maddyhome.idea.vim.common;

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

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Comparator;

/**
 * This represents a file mark. Each mark has a line and a column, the file it applies to, and the mark key
 */
public class Mark
{
    /**
     * Creates a file mark
     * @param key The mark's key
     * @param lline The logical line within the file
     * @param col The column within the line
     * @param file The file being marked
     */
    public Mark(char key, int lline, int col, VirtualFile file)
    {
        this.key = key;
        this.line = lline;
        this.col = col;
        this.file = file;
        this.filename = null;
    }

    /**
     * Creates a file mark
     * @param key The mark's key
     * @param lline The logical line within the file
     * @param col The column within the line
     * @param filename The file being marked
     */
    public Mark(char key, int lline, int col, String filename)
    {
        this.key = key;
        this.line = lline;
        this.col = col;
        this.file = null;
        this.filename = filename;
    }

    /**
     * Clears the mark indicating that it is no longer a valid mark
     */
    public void clear()
    {
        line = -1;
        col = -1;
        file = null;
        filename = null;
    }

    /**
     * Checks to see if the mark has been invalidated
     * @return true is invalid or clear, false if not
     */
    public boolean isClear()
    {
        return (line == -1 && col == -1);
    }

    /**
     * The mark's key
     * @return The mark's key
     */
    public char getKey()
    {
        return key;
    }

    /**
     * The mark's line
     * @return The mark's line
     */
    public int getLogicalLine()
    {
        return line;
    }

    /**
     * Updates the mark's lline
     * @param lline The new lline for the mark
     */
    public void setLogicalLine(int lline)
    {
        this.line = lline;
    }

    /**
     * The mark's column
     * @return The mark's columnn
     */
    public int getCol()
    {
        return col;
    }

    /**
     * The virtual file associated with this mark
     * @return The mark's virtual file
     */
    public VirtualFile getFile()
    {
        if (file == null)
        {
            file = LocalFileSystem.getInstance().findFileByPath(filename);
        }

        return file;
    }

    /**
     * Gets the filename the mark is associate with
     * @return The mark's filename
     */
    public String getFilename()
    {
        if (filename != null)
        {
            return filename;
        }
        else if (file != null)
        {
            return file.getPath();
        }
        else
        {
            return null;
        }
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();
        res.append("Mark(key=");
        res.append(key);
        res.append(", line=");
        res.append(line);
        res.append(", col=");
        res.append(col);
        res.append(", file=");
        res.append(file);
        res.append(", filename=");
        res.append(filename);

        return res.toString();
    }

    public static class KeySorter implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            Mark a = (Mark)o1;
            Mark b = (Mark)o2;
            if (a.key < b.key)
            {
                return -1;
            }
            else if (a.key > b.key)
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
    }

    private char key;
    private int line;
    private int col;
    private VirtualFile file;
    private String filename;
}
