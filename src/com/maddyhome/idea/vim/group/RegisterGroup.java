package com.maddyhome.idea.vim.group;

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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.ui.ClipboardHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.jdom.CDATA;
import org.jdom.Element;

/**
 * This group works with command associated with copying and pasting text
 */
public class RegisterGroup extends AbstractActionGroup
{
    /** The regsister key for the default register */
    public static final char REGISTER_DEFAULT = '"';

    /**
     * Creates the group
     */
    public RegisterGroup()
    {
    }

    /**
     * Check to see if the last selected register can be written to
     * @return true if writable register, false if not
     */
    public boolean isRegisterWritable()
    {
        return READONLY_REGISTERS.indexOf(lastRegister) < 0;
    }

    /**
     * Stores which register the user wishes to work with
     * @param reg The register name
     * @return true if a valid register name, false if not
     */
    public boolean selectRegister(char reg)
    {
        if (VALID_REGISTERS.indexOf(reg) != -1)
        {
            lastRegister = reg;
            logger.debug("register selected: " + lastRegister);

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Resets the selected register back to the default register
     */
    public void resetRegister()
    {
        lastRegister = REGISTER_DEFAULT;
        logger.debug("register reset");
    }

    /**
     * Stores text into the last register
     * @param editor The editor to get the text from
     * @param context The data context
     * @param start The start offset of the text to store
     * @param end The end offset of the text to store
     * @param type The type of copy - linewise or characterwise
     * @return true if able to store the text into the register, false if not
     */
    public boolean storeText(Editor editor, DataContext context, int start, int end, int type, boolean isDelete, boolean isYank)
    {
        if (isRegisterWritable())
        {
            String text = EditorHelper.getText(editor, start, end);

            return storeTextInternal(editor, context, start, end, text, type, lastRegister, isDelete, isYank);
        }

        return false;
    }

    public boolean storeTextInternal(Editor editor, DataContext context, int start, int end, String text, int type, char register, boolean isDelete, boolean isYank)
    {
        // Null register doesn't get saved
        if (lastRegister == '_') return true;

        // Normalize the start and end
        if (start > end)
        {
            int t = start;
            start = end;
            end = t;
        }

        // If this is an uppercase register, we need to append the text to the corresponding lowercase register
        if (Character.isUpperCase(register))
        {
            char lreg = Character.toLowerCase(register);
            Register r = (Register)registers.get(new Character(lreg));
            // Append the text if the lowercase register existed
            if (r != null)
            {
                r.addText(text);
            }
            // Set the text if the lowercase register didn't exist yet
            else
            {
                registers.put(new Character(lreg), new Register(type, text));
                logger.debug("register '" + register + "' contains: \"" + text + "\"");
            }
        }
        else if (register == '*' || register == '+')
        {
            ClipboardHandler.setClipboardText(text);
        }
        // Put the text in the specified register
        else
        {
            registers.put(new Character(register), new Register(type, text));
            logger.debug("register '" + register + "' contains: \"" + text + "\"");
        }

        // Also add it to the default register if the default wasn't specified
        if (register != REGISTER_DEFAULT)
        {
            registers.put(new Character(REGISTER_DEFAULT), new Register(type, text));
            logger.debug("register '" + register + "' contains: \"" + text + "\"");
        }

        // Deletes go into register 1. Old 1 goes to 2, etc. Old 8 to 9, old 9 is lost
        if (isDelete)
        {
            for (char d = '8'; d >= '1'; d--)
            {
                Register t = (Register)registers.get(new Character(d));
                if (t != null)
                {
                    registers.put(new Character((char)(d + 1)), t);
                }
            }
            registers.put(new Character('1'), new Register(type, text));

            // Deletes small than one line also go the the - register
            if (type == MotionGroup.CHARACTERWISE)
            {
                if (editor.offsetToLogicalPosition(start).line == editor.offsetToLogicalPosition(end).line)
                {
                    registers.put(new Character('-'), new Register(type, text));
                }
            }
        }
        // Yanks also go to register 0 if the default register was used
        else if (register == REGISTER_DEFAULT)
        {
            registers.put(new Character('0'), new Register(type, text));
            logger.debug("register '" + register + "' contains: \"" + text + "\"");
        }

        CommandGroups.getInstance().getMark().setMark(editor, context, '[', start);
        CommandGroups.getInstance().getMark().setMark(editor, context, ']', end);

        return true;
    }

    /**
     * Get the last register selected by the user
     * @return The register, null if no such register
     */
    public Register getLastRegister()
    {
        return getRegister(lastRegister);
    }

    public Register getRegister(char r)
    {
        // Uppercase registers actually get the lowercase register
        if (Character.isUpperCase(r))
        {
            r = Character.toLowerCase(r);
        }

        Register reg = null;
        if (r == '*' || r == '+')
        {
            String text = ClipboardHandler.getClipboardText();
            if (text != null)
            {
                reg = new Register(MotionGroup.CHARACTERWISE, text);
            }
        }
        else
        {
            reg = (Register)registers.get(new Character(r));
        }

        return reg;
    }

    /**
     * Gets the last register name selected by the user
     * @return The register name
     */
    public char getCurrentRegister()
    {
        return lastRegister;
    }

    /**
     * Save all the registers
     * @param element The plugin's root XML element that this group can add a child to
     */
    public void saveData(Element element)
    {
        Element regs = new Element("registers");
        for (Iterator iterator = registers.keySet().iterator(); iterator.hasNext();)
        {
            Character key = (Character)iterator.next();
            Register register = (Register)registers.get(key);

            Element reg = new Element("register");
            reg.setAttribute("name", String.valueOf(key));
            reg.setAttribute("type", Integer.toString(register.getType()));
            Element text = new Element("text");
            CDATA data = new CDATA(register.getText());
            text.addContent(data);
            reg.addContent(text);
            regs.addContent(reg);
        }

        element.addContent(regs);
    }

    /**
     * Restore all the registers
     * @param element The plugin's root XML element that this group can add a child to
     */
    public void readData(Element element)
    {
        logger.debug("readData");
        Element regs = element.getChild("registers");
        if (regs != null)
        {
            List list = regs.getChildren("register");
            for (int i = 0; i < list.size(); i++)
            {
                Element reg = (Element)list.get(i);
                Register register = new Register(Integer.parseInt(reg.getAttributeValue("type")), reg.getChildText("text"));
                Character key = new Character(reg.getAttributeValue("name").charAt(0));
                registers.put(key, register);
            }
        }
    }

    private char lastRegister = REGISTER_DEFAULT;
    private HashMap registers = new HashMap();

    private static final String WRITABLE_REGISTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-*+_/\"";
    private static final String READONLY_REGISTERS = ":.%#=/";
    private static final String VALID_REGISTERS = WRITABLE_REGISTERS + READONLY_REGISTERS;

    private static Logger logger = Logger.getInstance(RegisterGroup.class.getName());
}
