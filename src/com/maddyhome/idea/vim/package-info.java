/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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

/**
 * IdeaVim command index.
 *
 *
 * 1. Insert mode
 *
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * |i_<Esc>|                {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-[|               ...
 * |i_CTRL-C|               ...
 * |i_CTRL-\_CTRL-N|        ...
 *
 *
 * 2. Normal mode
 *
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * |i|                      {@link com.maddyhome.idea.vim.action.change.insert.InsertBeforeCursorAction}
 * |<Insert>|               ...
 * |v|                      {@link com.maddyhome.idea.vim.action.motion.visual.VisualToggleCharacterModeAction}
 * |gv|                     {@link com.maddyhome.idea.vim.action.motion.visual.VisualSelectPreviousAction}
 *
 *
 * 2.2. Window commands
 *
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * |CTRL-W_+|               TODO
 * |CTRL-W_-|               TODO
 * |CTRL-W_<|               TODO
 * |CTRL-W_=|               TODO
 * |CTRL-W_>|               TODO
 * |CTRL-W_H|               TODO
 * |CTRL-W_J|               TODO
 * |CTRL-W_K|               TODO
 * |CTRL-W_L|               TODO
 * |CTRL-W_R|               TODO
 * |CTRL-W_W|               {@link com.maddyhome.idea.vim.action.window.WindowPrevAction}
 * |CTRL-W_]|               TODO
 * |CTRL-W_^|               TODO
 * |CTRL-W__|               TODO
 * |CTRL-W_b|               TODO
 * |CTRL-W_c|               {@link com.maddyhome.idea.vim.action.window.CloseWindowAction}
 * |CTRL-W_h|               {@link com.maddyhome.idea.vim.action.window.WindowLeftAction}
 * |CTRL-W_<Left>|          ...
 * |CTRL-W_j|               {@link com.maddyhome.idea.vim.action.window.WindowDownAction}
 * |CTRL-W_<Down>|          ...
 * |CTRL-W_k|               {@link com.maddyhome.idea.vim.action.window.WindowUpAction}
 * |CTRL-W_<Up>|            ...
 * |CTRL-W_l|               {@link com.maddyhome.idea.vim.action.window.WindowRightAction}
 * |CTRL-W_<Right>|         ...
 * |CTRL-W_n|               TODO
 * |CTRL-W_o|               {@link com.maddyhome.idea.vim.action.window.WindowOnlyAction}
 * |CTRL-W_CTRL-O|          ...
 * |CTRL-W_p|               TODO
 * |CTRL-W_q|               TODO
 * |CTRL-W_r|               TODO
 * |CTRL-W_s|               {@link com.maddyhome.idea.vim.action.window.HorizontalSplitAction}
 * |CTRL-W_S|               ...
 * |CTRL-W_CTRL-S|          ...
 * |CTRL-W_t|               TODO
 * |CTRL-W_v|               {@link com.maddyhome.idea.vim.action.window.VerticalSplitAction}
 * |CTRL-W_CTRL-V|          ...
 * |CTRL-W_w|               {@link com.maddyhome.idea.vim.action.window.WindowNextAction}
 * |CTRL-W_CTRL-W|          ...
 * |CTRL-W_z|               TODO
 * |CTRL-W_bar|             TODO
 *
 *
 * 2.4. Commands starting with 'g'
 *
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * |g@|                     {@link com.maddyhome.idea.vim.action.change.OperatorAction}
 *
 *
 * 3. Visual mode
 *
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * |v_<Esc>|                {@link com.maddyhome.idea.vim.action.motion.visual.VisualExitModeAction}
 * |v_CTRL-C|               ...
 * |v_CTRL-\_CTRL-N|        ...
 * |v_<BS>|                 NVO mapping
 * |v_CTRL-H|               ...
 * |v_CTRL-V|               NVO mapping
 * |v_!|                    {@link com.maddyhome.idea.vim.action.change.change.FilterVisualLinesAction}
 * |v_:|                    NVO mapping
 * |v_<|                    {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftVisualAction}
 * |v_=|                    {@link com.maddyhome.idea.vim.action.change.change.AutoIndentLinesVisualAction}
 * |v_>|                    {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightVisualAction}
 * |v_[p|                   {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |v_]p|                   ...
 * |v_[P|                   ...
 * |v_]P|                   ...
 * |v_A|                    {@link com.maddyhome.idea.vim.action.change.insert.VisualBlockAppendAction}
 * |v_C|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualLinesEndAction}
 * |v_D|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualLinesEndAction}
 * |v_I|                    {@link com.maddyhome.idea.vim.action.change.insert.VisualBlockInsertAction}
 * |v_J|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinVisualLinesSpacesAction}
 * |v_K|                    TODO
 * |v_O|                    {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapEndsBlockAction}
 * |v_R|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualLinesAction}
 * |v_S|                    ...
 * |v_U|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseUpperVisualAction}
 * |v_V|                    NV mapping
 * |v_X|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualLinesAction}
 * |v_Y|                    {@link com.maddyhome.idea.vim.action.copy.YankVisualLinesAction}
 * |v_a"|                   VO mapping
 * |v_a'|                   VO mapping
 * |v_ab|                   VO mapping
 * |v_a(|                   ...
 * |v_a)|                   ...
 * |v_at|                   {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockTagAction}
 * |v_a<|                   ...
 * |v_a>|                   ...
 * |v_aB|                   VO mapping
 * |v_a{|                   ...
 * |v_a}|                   ...
 * |v_aW|                   VO mapping
 * |v_a[|                   VO mapping
 * |v_a]|                   ...
 * |v_a`|                   VO mapping
 * |v_ap|                   VO mapping
 * |v_as|                   VO mapping
 * |v_aw|                   VO mapping
 * |v_c|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualAction}
 * |v_s|                    ...
 * |v_d|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualAction}
 * |v_x|                    ...
 * |v_<Del>|                ...
 * |v_gJ|                   {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinVisualLinesAction}
 * |v_gp|                   {@link com.maddyhome.idea.vim.action.copy.PutVisualTextMoveCursorAction}
 * |v_gP|                   ...
 * |v_gq|                   {@link com.maddyhome.idea.vim.action.change.change.ReformatCodeVisualAction}
 * |v_gv|                   {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapSelectionsAction}
 * |v_i"|                   VO mapping
 * |v_i'|                   VO mapping
 * |v_ib|                   VO mapping
 * |v_i(|                   ...
 * |v_i)|                   ...
 * |v_it|                   {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockTagAction}
 * |v_i<|                   ...
 * |v_i>|                   ...
 * |v_iB|                   VO mapping
 * |v_i{|                   ...
 * |v_i}|                   ...
 * |v_iW|                   VO mapping
 * |v_i[|                   VO mapping
 * |v_i]|                   ...
 * |v_i`|                   VO mapping
 * |v_ip|                   VO mapping
 * |v_is|                   VO mapping
 * |v_iw|                   VO mapping
 * |v_o|                    {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapEndsAction}
 * |v_p|                    {@link com.maddyhome.idea.vim.action.copy.PutVisualTextAction}
 * |v_P|                    ...
 * |v_r|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualCharacterAction}
 * |v_u|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseLowerVisualAction}
 * |v_v|                    NV mapping
 * |v_y|                    {@link com.maddyhome.idea.vim.action.copy.YankVisualAction}
 * |v_~|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseToggleVisualAction}
 *
 * TODO: Support Select mode and commands associated with it, such as |v_CTRL-G|, |v_CTRL-O|
 *
 *
 * 5. Ex commands
 *
 * tag                      handler
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * |:map|                   {@link com.maddyhome.idea.vim.ex.handler.MapHandler}
 * |:nmap|                  ...
 * |:vmap|                  ...
 * |:omap|                  ...
 * |:imap|                  ...
 * |:cmap|                  ...
 * |:noremap|               ...
 * |:nnoremap|              ...
 * |:vnoremap|              ...
 * |:onoremap|              ...
 * |:inoremap|              ...
 * |:cnoremap|              ...
 * |:sort|                  {@link com.maddyhome.idea.vim.ex.handler.SortHandler}
 * |:source|                {@link com.maddyhome.idea.vim.ex.handler.SourceHandler}
 *
 * @see :help index.
 *
 * @author vlan
 */
package com.maddyhome.idea.vim;
