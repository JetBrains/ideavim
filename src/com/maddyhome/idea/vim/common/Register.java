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

/**
 * Represents a register
 */
public class Register
{
    /**
     * Create a register of the specified type for the given text
     * @param type The register type (linewise or characterwise)
     * @param text The text to store
     */
    public Register(int type, String text)
    {
        this.type = type;
        this.text = text;
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

    private int type;
    private String text;
}
