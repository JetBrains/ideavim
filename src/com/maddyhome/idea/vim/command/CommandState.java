package com.maddyhome.idea.vim.command;

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

import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.maddyhome.idea.vim.key.KeyParser;

/**
 * This singleton maintains various state information about commands being run
 */
public class CommandState
{
    /** Indicates a runtime state of being in command mode */
    public static final int MODE_COMMAND = 1;
    /** Indicates a runtime state of being in insert mode */
    public static final int MODE_INSERT = 2;
    /** Indicates a runtime state of being in replace mode */
    public static final int MODE_REPLACE = 3;
    /** Indicates a runtime state of being in repeat mode */
    public static final int MODE_REPEAT = 4;
    /** Indicates a runtime state of being in visual mode */
    public static final int MODE_VISUAL = 5;

    /**
     * Gets the command state singleton
     * @return The singleton instance
     */
    public synchronized static CommandState getInstance()
    {
        if (ourInstance == null)
        {
            ourInstance = new CommandState();
        }

        return ourInstance;
    }

    /**
     * Gets the currently executing command
     * @return The running command
     */
    public Command getCommand()
    {
        return command;
    }

    /**
     * This maintains the current command that is being executed
     * @param cmd The currently executing command
     */
    public void setCommand(Command cmd)
    {
        command = cmd;
    }

    /**
     * Gets the current mode the command is in
     * @return The current runtime mode
     */
    public int getMode()
    {
        return mode;
    }

    /**
     * Sets the runtime mode
     * @param mode The new mode
     */
    public void setMode(int mode)
    {
        this.mode = mode;
    }

    /**
     * Gets the current visual mode type
     * @return The visual mode type
     */
    public int getVisualType()
    {
        return visualType;
    }

    /**
     * Sets the visual mode type. {@link com.maddyhome.idea.vim.group.MotionGroup#CHARACTERWISE} and
     * {@link com.maddyhome.idea.vim.group.MotionGroup#LINEWISE}
     * @param visualType The new visual mode type
     */
    public void setVisualType(int visualType)
    {
        this.visualType = visualType;
    }

    /**
     * Toggles the insert/overwrite state. If currently insert, goto replace mode. If currently replace, goto insert
     * mode.
     */
    public void toggleInsertOverwrite()
    {
        if (mode == MODE_INSERT)
        {
            mode = MODE_REPLACE;
        }
        else if (mode == MODE_REPLACE)
        {
            mode = MODE_INSERT;
        }
    }

    /**
     * Save the current mode state. This saves the mode and mapping mode and then resets them to initial values
     */
    public void saveMode()
    {
        oldMode = mode;
        oldMapping = mappingMode;
        mode = MODE_COMMAND;
        mappingMode = KeyParser.MAPPING_NORMAL;
    }

    /**
     * Restores the previously saves mode and mapping mode.
     */
    public void restoreMode()
    {
        mode = oldMode;
        mappingMode = oldMapping;
        oldMode = MODE_COMMAND;
        oldMapping = KeyParser.MAPPING_NORMAL;
    }

    /**
     * Resets the command, mode, visual mode, and mapping mode to initial values.
     */
    public void reset()
    {
        command = null;
        mode = MODE_COMMAND;
        visualType = 0;
        mappingMode = KeyParser.MAPPING_NORMAL;
    }

    /**
     * Gets the current key mapping mode
     * @return The current key mapping mode
     */
    public int getMappingMode()
    {
        return mappingMode;
    }

    /**
     * Sets the key mapping mode. See the MAPPING constants in KeyParser.
     * @param mappingMode The new mapping mode.
     */
    public void setMappingMode(int mappingMode)
    {
        this.mappingMode = mappingMode;
    }

    /**
     * Gets the last command that performed a change
     * @return The last change command, null if there hasn't been a change yet
     */
    public Command getLastChangeCommand()
    {
        return lastChange;
    }

    /**
     * Gets the register used by the last saved change command
     * @return The register key
     */
    public char getLastChangeRegister()
    {
        return lastRegister;
    }

    /**
     * Saves the last command that performed a change. It also preserves the register the command worked with.
     * @param cmd The change command
     */
    public void saveLastChangeCommand(Command cmd)
    {
        lastChange = cmd;
        lastRegister = CommandGroups.getInstance().getRegister().getCurrentRegister();
    }

    /**
     * Signleton, no public object creation
     */
    private CommandState()
    {
        reset();
    }

    private Command command;
    private int mode;
    private Command lastChange;
    private char lastRegister = RegisterGroup.REGISTER_DEFAULT;
    private int oldMode = MODE_COMMAND;
    private int mappingMode = KeyParser.MAPPING_NORMAL;
    private int oldMapping = KeyParser.MAPPING_NORMAL;
    private int visualType = 0;

    private static CommandState ourInstance;
}

