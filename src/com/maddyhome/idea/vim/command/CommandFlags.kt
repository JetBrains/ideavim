/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.command

enum class CommandFlags {
    /**
     * Motion flags
     */
    FLAG_MOT_LINEWISE,
    FLAG_MOT_CHARACTERWISE,
    FLAG_MOT_BLOCKWISE,
    FLAG_MOT_INCLUSIVE,
    FLAG_MOT_EXCLUSIVE,
    /**
     * Indicates that the cursor position should be saved prior to this visual command
     */
    FLAG_SAVE_JUMP,
    /**
     * Special flag that says this is characterwise only for visual mode
     */
    FLAG_VISUAL_CHARACTERWISE,
    /**
     * Special command flag that indicates it is not to be repeated
     */
    FLAG_NO_REPEAT,
    /**
     * This insert command should clear all saved keystrokes from the current insert
     */
    FLAG_CLEAR_STROKES,
    /**
     * This keystroke should be saved as part of the current insert
     */
    FLAG_SAVE_STROKE,
    /**
     * This is a backspace command
     */
    FLAG_IS_BACKSPACE,
    FLAG_IGNORE_SCROLL_JUMP,
    FLAG_IGNORE_SIDE_SCROLL_JUMP,
    /**
     * Indicates a command can accept a count in mid command
     */
    FLAG_ALLOW_MID_COUNT,
    /**
     * Search Flags
     */
    FLAG_SEARCH_FWD,
    FLAG_SEARCH_REV,
    /**
     * Command exits the visual mode, so caret movement shouldn't update visual selection
     */
    FLAG_EXIT_VISUAL,
    FLAG_FORCE_VISUAL,
    FLAG_FORCE_LINEWISE,
    /**
     * Special flag used for any mappings involving operators
     */
    FLAG_OP_PEND,
    /**
     * This command starts a multi-command undo transaction
     */
    FLAG_MULTIKEY_UNDO,
    /**
     * This command should be followed by another command
     */
    FLAG_EXPECT_MORE,
    /**
     * This flag indicates the command's argument isn't used while recording
     */
    FLAG_NO_ARG_RECORDING,
    /**
     * Indicate that the character argument may come from a digraph
     */
    FLAG_ALLOW_DIGRAPH,
    FLAG_COMPLETE_EX,
    FLAG_TEXT_BLOCK
}