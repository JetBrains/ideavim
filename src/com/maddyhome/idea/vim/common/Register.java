package com.maddyhome.idea.vim.common;

import java.util.Comparator;

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
 * Represents a register.
 * TODO - need to support mixed array of Character and AnAction.
 */
public class Register
{
    /**
     * Create a register of the specified type for the given text
     * @param type The register type (linewise or characterwise)
     * @param text The text to store
     */
    public Register(char key, int type, String text)
    {
        this.key = key;
        this.type = type;
        this.text = text;
    }

    /**
     * Gets the key the register is assigned to
     * @return The register key
     */
    public char getKey()
    {
        return key;
    }

    /**
     * Get the register type
     * @return The register type
     */
    public int getType()
    {
        return type;
    }

    /**
     * Get the text in the register
     * @return The register text
     */
    public String getText()
    {
        return text;
    }

    /**
     * Appends the supplied text to any existing text
     * @param text The text to add
     */
    public void addText(String text)
    {
        this.text = this.text + text;
    }

    public static class KeySorter implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            Register a = (Register)o1;
            Register b = (Register)o2;
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
    private int type;
    private String text;
}
