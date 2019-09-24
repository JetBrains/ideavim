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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.command

enum class MotionType {
  INCLUSIVE,
  EXCLUSIVE
}

enum class CommandFlags {
  /**
   * Motion flags
   */
  FLAG_MOT_LINEWISE,
  FLAG_MOT_CHARACTERWISE,
  FLAG_MOT_BLOCKWISE,
  /**
   * Indicates that the cursor position should be saved prior to this motion command
   */
  FLAG_SAVE_JUMP,
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
  FLAG_IGNORE_SCROLL_JUMP,
  FLAG_IGNORE_SIDE_SCROLL_JUMP,
  /**
   * Indicates a command can accept a count in mid command
   * E.g. d2d, c2c
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
  FLAG_TEXT_BLOCK,
  /**
   * Some IDE actions do enable `typeahead` option for proper popups handling.
   *   There actions are GoToClass, GoToFile, SearchEverywhere and so on. With this options enabled if vim-action is
   *   bound to the same shortcut as one of actions that are listed above, user will face significant UI freezes.
   *   To avoid there freezes, `IdeEventQueue.getInstance().flushDelayedKeyEvents();` should be called. This
   *   function is called automatically from [com.maddyhome.idea.vim.KeyHandler], but it will not be called if
   *   vim-action has this flag. In that case this action should call function by itself.
   *
   * This flag is created for more convenience and used in [com.maddyhome.idea.vim.action.window.LookupUpAction]
   *   and [com.maddyhome.idea.vim.action.window.LookupDownAction] because there actions have custom handler
   *   only if lookup is active.
   */
  FLAG_TYPEAHEAD_SELF_MANAGE
}
