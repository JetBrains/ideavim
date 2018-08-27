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
 * tag                    action
 * -------------------------------------------------------------------------------------------------------------------
 *
 * |i_CTRL-@|             {@link com.maddyhome.idea.vim.action.change.insert.InsertPreviousInsertExitAction}
 * |i_CTRL-A|             {@link com.maddyhome.idea.vim.action.change.insert.InsertPreviousInsertAction}
 * |i_CTRL-C|             {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-D|             {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftLinesAction}
 * |i_CTRL-E|             {@link com.maddyhome.idea.vim.action.change.insert.InsertCharacterBelowCursorAction}
 * |i_CTRL-G_j|           TODO
 * |i_CTRL-G_k|           TODO
 * |i_CTRL-G_u|           TODO
 * |i_<BS>|               IntelliJ editor backspace
 * |i_digraph|            IdeaVim enter digraph
 * |i_CTRL-H|             IntelliJ editor backspace
 * |i_<Tab>|              IntelliJ editor tab
 * |i_CTRL-I|             IntelliJ editor tab
 * |i_<NL>|               {@link com.maddyhome.idea.vim.action.change.insert.InsertEnterAction}
 * |i_CTRL-J|             TODO
 * |i_CTRL-K|             IdeaVim enter digraph
 * |i_CTRL-L|             TODO
 * |i_<CR>|               {@link com.maddyhome.idea.vim.action.change.insert.InsertEnterAction}
 * |i_CTRL-M|             {@link com.maddyhome.idea.vim.action.change.insert.InsertEnterAction}
 * |i_CTRL-N|             TODO
 * |i_CTRL-O|             {@link com.maddyhome.idea.vim.action.change.insert.InsertSingleCommandAction}
 * |i_CTRL-P|             TODO
 * |i_CTRL-Q|             TODO
 * |i_CTRL-R|             {@link com.maddyhome.idea.vim.action.change.insert.InsertRegisterAction}
 * |i_CTRL-R_CTRL-R|      TODO
 * |i_CTRL-R_CTRL-O|      TODO
 * |i_CTRL-R_CTRL-P|      TODO
 * |i_CTRL-T|             {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightLinesAction}
 * |i_CTRL-U|             {@link com.maddyhome.idea.vim.action.change.insert.InsertDeleteInsertedTextAction}
 * |i_CTRL-V|             TODO
 * |i_CTRL-V_digit|       TODO
 * |i_CTRL-W|             {@link com.maddyhome.idea.vim.action.change.insert.InsertDeletePreviousWordAction}
 * |i_CTRL-X|             TODO
 * |i_CTRL-Y|             {@link com.maddyhome.idea.vim.action.change.insert.InsertCharacterAboveCursorAction}
 * |i_CTRL-Z|             TODO
 * |i_<Esc>|              {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-[|             {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-\_CTRL-N|      {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-\_CTRL-G|      TODO
 * |i_CTRL-]}             TODO
 * |i_CTRL-^|             TODO
 * |i_CTRL-_|             TODO
 * |i_0_CTRL-D|           TODO
 * |i_^_CTRL-D|           TODO
 * |i_<Del>|              IntelliJ editor delete
 * |i_<Left>|             {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftAction}
 * |i_<S-Left>|           {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |i_<C-Left>|           {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |i_<Right>|            {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightAction}
 * |i_<S-Right>|          {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |i_<C-Right>|          {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |i_<Up>|               {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpAction}
 * |i_<S-Up>|             {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |i_<Down>|             {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |i_<S-Down>|           {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |i_<Home>|             {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstColumnAction}
 * |i_<C-Home>|           {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineFirstAction}
 * |i_<End>|              {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastColumnAction}
 * |i_<C-End>|            {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineLastEndAction}
 * |i_<Insert>|           {@link com.maddyhome.idea.vim.action.change.insert.InsertInsertAction}
 * |i_<PageUp>|           {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |i_<PageDown>|         {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |i_<F1>|               IntelliJ help
 * |i_<Insert>|           IntelliJ editor toggle insert/replace
 * |i_CTRL-X_index|       TODO
 *
 *
 * 2. Normal mode
 *
 * tag                    action
 * -------------------------------------------------------------------------------------------------------------------
 *
 * |CTRL-A|               {@link com.maddyhome.idea.vim.action.change.change.ChangeNumberIncAction}
 * |CTRL-B|               {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |CTRL-C|               TODO
 * |CTRL-D|               {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollHalfPageDownAction}
 * |CTRL-E|               {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLineDownAction}
 * |CTRL-F|               {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |CTRL-G|               {@link com.maddyhome.idea.vim.action.file.FileGetFileInfoAction}
 * |<BS>|                 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftWrapAction}
 * |CTRL-H|               {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftWrapAction}
 * |<Tab>|                TODO
 * |CTRL-I|               {@link com.maddyhome.idea.vim.action.motion.mark.MotionJumpNextAction}
 * |<NL>|                 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |CTRL-J|               TODO
 * |CTRL-L|               not applicable
 * |<CR>|                 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownFirstNonSpaceAction}
 * |CTRL-M|               {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownFirstNonSpaceAction}
 * |CTRL-N|               {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |CTRL-O|               {@link com.maddyhome.idea.vim.action.motion.mark.MotionJumpPreviousAction}
 * |CTRL-P|               {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpAction}
 * |CTRL-R|               {@link com.maddyhome.idea.vim.action.change.RedoAction}
 * |CTRL-T|               TODO
 * |CTRL-U|               {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollHalfPageUpAction}
 * |CTRL-V|               {@link com.maddyhome.idea.vim.action.motion.visual.VisualToggleBlockModeAction}
 * |CTRL-W|               see window commands
 * |CTRL-X|               {@link com.maddyhome.idea.vim.action.change.change.ChangeNumberDecAction}
 * |CTRL-Y|               {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLineUpAction}
 * |CTRL-Z|               TODO
 * |CTRL-]|               {@link com.maddyhome.idea.vim.action.motion.search.GotoDeclarationAction}
 * |<Space>|              {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightWrapAction}
 * |!|                    {@link com.maddyhome.idea.vim.action.change.change.FilterMotionAction}
 * |!!|                   {@link com.maddyhome.idea.vim.action.change.change.FilterCountLinesAction}
 * |quote|                {@link com.maddyhome.idea.vim.action.copy.SelectRegisterAction}
 * |#|                    {@link com.maddyhome.idea.vim.action.motion.search.SearchWholeWordBackwardAction}
 * |$|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastColumnAction}
 * |%|                    {@link com.maddyhome.idea.vim.action.motion.updown.MotionPercentOrMatchAction}
 * |&|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeLastSearchReplaceAction}
 * |'|                    {@link com.maddyhome.idea.vim.action.motion.mark.MotionGotoFileMarkLineAction}
 * |'|                    {@link com.maddyhome.idea.vim.action.motion.mark.MotionGotoFileMarkAction}
 * |''|                   ?
 * ...
 * |(|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionSentencePreviousStartAction}
 * |)|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionSentenceNextStartAction}
 * |star|                 {@link com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction}
 * |+|                    {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownFirstNonSpaceAction}
 * |,|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastMatchCharReverseAction}
 * |-|                    {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpFirstNonSpaceAction}
 * |.|                    {@link com.maddyhome.idea.vim.action.change.RepeatChangeAction}
 * |/|                    {@link com.maddyhome.idea.vim.action.motion.search.SearchEntryFwdAction}
 * |:|                    {@link com.maddyhome.idea.vim.action.ExEntryAction}
 * |;|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastMatchCharAction}
 * |<|                    {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftMotionAction}
 * |<<|                   {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftLinesAction}
 * |=|                    {@link com.maddyhome.idea.vim.action.change.shift.AutoIndentMotionAction}
 * |==|                   {@link com.maddyhome.idea.vim.action.change.shift.AutoIndentLinesAction}
 * |>|                    {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightMotionAction}
 * |>>|                   {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightLinesAction}
 * |?|                    {@link com.maddyhome.idea.vim.action.motion.search.SearchEntryRevAction}
 * |@|                    {@link com.maddyhome.idea.vim.action.macro.PlaybackRegisterAction}
 * |@:|                   {@link com.maddyhome.idea.vim.action.change.RepeatExCommandAction}
 * |@@|                   {@link com.maddyhome.idea.vim.action.macro.PlaybackLastRegisterAction}
 * |A|                    {@link com.maddyhome.idea.vim.action.change.insert.InsertAfterLineEndAction}
 * |B|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionBigWordLeftAction}
 * |C|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeEndOfLineAction}
 * |D|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteEndOfLineAction}
 * |E|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionBigWordEndRightAction}
 * |F|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftMatchCharAction}
 * |G|                    {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineLastAction}
 * |H|                    {@link com.maddyhome.idea.vim.action.motion.screen.MotionFirstScreenLineAction}
 * |I|                    {@link com.maddyhome.idea.vim.action.change.insert.InsertBeforeFirstNonBlankAction}
 * |J|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinLinesSpacesAction}
 * |K|                    TODO
 * |L|                    {@link com.maddyhome.idea.vim.action.motion.screen.MotionLastScreenLineAction}
 * |M|                    {@link com.maddyhome.idea.vim.action.motion.screen.MotionMiddleScreenLineAction}
 * |N|                    {@link com.maddyhome.idea.vim.action.motion.search.SearchAgainPreviousAction}
 * |O|                    {@link com.maddyhome.idea.vim.action.change.insert.InsertNewLineAboveAction}
 * |P|                    {@link com.maddyhome.idea.vim.action.copy.PutTextBeforeCursorAction}
 * |Q|                    TODO
 * |R|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeReplaceAction}
 * |S|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeLineAction}
 * |T|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftTillMatchCharAction}
 * |U|                    ?
 * |V|                    {@link com.maddyhome.idea.vim.action.motion.visual.VisualToggleLineModeAction}
 * |W|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionBigWordRightAction}
 * |X|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteCharacterLeftAction}
 * |Y|                    {@link com.maddyhome.idea.vim.action.copy.YankLineAction}
 * |ZZ|                   {@link com.maddyhome.idea.vim.action.file.FileSaveCloseAction}
 * |ZQ|                   {@link com.maddyhome.idea.vim.action.file.FileSaveCloseAction}
 * |[|                    see bracket commands
 * |]|                    see bracket commands
 * |^|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstNonSpaceAction}
 * |_|                    {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownLess1FirstNonSpaceAction}
 * |`|                    {@link com.maddyhome.idea.vim.action.motion.mark.MotionGotoFileMarkAction}
 * |`|                    {@link com.maddyhome.idea.vim.action.motion.mark.MotionGotoMarkAction}
 * |``|                   ?
 * ...
 * |0|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstColumnAction}
 * |a|                    {@link com.maddyhome.idea.vim.action.change.insert.InsertAfterCursorAction}
 * |b|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |c|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeMotionAction}
 * |cc|                   {@link com.maddyhome.idea.vim.action.change.change.ChangeLineAction}
 * |d|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteMotionAction}
 * |dd|                   {@link com.maddyhome.idea.vim.action.change.delete.DeleteLineAction}
 * |do|                   TODO
 * |dp|                   TODO
 * |e|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionWordEndRightAction}
 * |f|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightMatchCharAction}
 * |g|                    see commands starting with 'g'
 * |h|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftAction}
 * |i|                    {@link com.maddyhome.idea.vim.action.change.insert.InsertBeforeCursorAction}
 * |j|                    {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |k|                    {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpAction}
 * |l|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightAction}
 * |m|                    {@link com.maddyhome.idea.vim.action.motion.mark.MotionMarkAction}
 * |n|                    {@link com.maddyhome.idea.vim.action.motion.search.SearchAgainNextAction}
 * |o|                    {@link com.maddyhome.idea.vim.action.change.insert.InsertNewLineBelowAction}
 * |p|                    {@link com.maddyhome.idea.vim.action.copy.PutTextAfterCursorAction}
 * |q|                    {@link com.maddyhome.idea.vim.action.macro.ToggleRecordingAction}
 * |r|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeCharacterAction}
 * |s|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeCharactersAction}
 * |t|                    {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightTillMatchCharAction}
 * |u|                    {@link com.maddyhome.idea.vim.action.change.UndoAction}
 * |v|                    {@link com.maddyhome.idea.vim.action.motion.visual.VisualToggleCharacterModeAction}
 * |w|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |x|                    {@link com.maddyhome.idea.vim.action.change.delete.DeleteCharacterRightAction}
 * |y|                    {@link com.maddyhome.idea.vim.action.copy.YankMotionAction}
 * |yy|                   {@link com.maddyhome.idea.vim.action.copy.YankLineAction}
 * |z|                    see commands starting with 'z'
 * |{|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionParagraphPreviousAction}
 * |bar|                  {@link com.maddyhome.idea.vim.action.motion.leftright.MotionColumnAction}
 * |}|                    {@link com.maddyhome.idea.vim.action.motion.text.MotionParagraphNextAction}
 * |~|                    {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseToggleCharacterAction}
 * |<C-End>|              {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineLastEndAction}
 * |<C-Home>|             {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineFirstAction}
 * |<C-Left>|             {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |<C-Right>|            {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |<Del>|                {@link com.maddyhome.idea.vim.action.change.delete.DeleteCharacterAction}
 * |<Down>|               {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |<End>|                {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastColumnAction}
 * |<F1>|                 IntelliJ help
 * |<Home>|               {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstColumnAction}
 * |<Insert>|             {@link com.maddyhome.idea.vim.action.change.insert.InsertBeforeCursorAction}
 * |<Left>|               {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftAction}
 * |<PageDown>|           {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |<PageUp>|             {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |<Right>|              {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightAction}
 * |<S-Down>|             {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |<S-Left>|             {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |<S-Right>|            {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |<S-Up>|               {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |<Up>|                 {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpAction}
 *
 *
 * 2.1. Text objects
 *
 * Text object commands are listed in the visual mode section.
 *
 *
 * 2.2. Window commands
 *
 * tag                    action
 * -------------------------------------------------------------------------------------------------------------------
 *
 * |CTRL-W_+|             TODO
 * |CTRL-W_-|             TODO
 * |CTRL-W_<|             TODO
 * |CTRL-W_=|             TODO
 * |CTRL-W_>|             TODO
 * |CTRL-W_H|             TODO
 * |CTRL-W_J|             TODO
 * |CTRL-W_K|             TODO
 * |CTRL-W_L|             TODO
 * |CTRL-W_P|             TODO
 * |CTRL-W_R|             TODO
 * |CTRL-W_S|             {@link com.maddyhome.idea.vim.action.window.HorizontalSplitAction}
 * |CTRL-W_T|             TODO
 * |CTRL-W_W|             {@link com.maddyhome.idea.vim.action.window.WindowPrevAction}
 * |CTRL-W_]|             TODO
 * |CTRL-W_^|             TODO
 * |CTRL-W__|             TODO
 * |CTRL-W_b|             TODO
 * |CTRL-W_c|             {@link com.maddyhome.idea.vim.action.window.CloseWindowAction}
 * |CTRL-W_d|             TODO
 * |CTRL-W_f|             TODO
 * |CTRL-W-F|             TODO
 * |CTRL-W-g]|            TODO
 * |CTRL-W-g}|            TODO
 * |CTRL-W-gf|            TODO
 * |CTRL-W-gF|            TODO
 * |CTRL-W_h|             {@link com.maddyhome.idea.vim.action.window.WindowLeftAction}
 * |CTRL-W_i|             TODO
 * |CTRL-W_j|             {@link com.maddyhome.idea.vim.action.window.WindowDownAction}
 * |CTRL-W_k|             {@link com.maddyhome.idea.vim.action.window.WindowUpAction}
 * |CTRL-W_l|             {@link com.maddyhome.idea.vim.action.window.WindowRightAction}
 * |CTRL-W_n|             TODO
 * |CTRL-W_o|             {@link com.maddyhome.idea.vim.action.window.WindowOnlyAction}
 * |CTRL-W_p|             TODO
 * |CTRL-W_q|             TODO
 * |CTRL-W_r|             TODO
 * |CTRL-W_s|             {@link com.maddyhome.idea.vim.action.window.HorizontalSplitAction}
 * |CTRL-W_t|             TODO
 * |CTRL-W_v|             {@link com.maddyhome.idea.vim.action.window.VerticalSplitAction}
 * |CTRL-W_w|             {@link com.maddyhome.idea.vim.action.window.WindowNextAction}
 * |CTRL-W_x|             TODO
 * |CTRL-W_z|             TODO
 * |CTRL-W_bar|           TODO
 * |CTRL-W_}|             TODO
 * |CTRL-W_<Down>|        {@link com.maddyhome.idea.vim.action.window.WindowDownAction}
 * |CTRL-W_<Up>|          {@link com.maddyhome.idea.vim.action.window.WindowUpAction}
 * |CTRL-W_<Left>|        {@link com.maddyhome.idea.vim.action.window.WindowLeftAction}
 * |CTRL-W_<Right>|       {@link com.maddyhome.idea.vim.action.window.WindowRightAction}
 *
 *
 * 2.3. Square bracket commands
 *
 * tag                    action
 * -------------------------------------------------------------------------------------------------------------------
 * |[_CTRL-D|             TODO
 * |[_CTRL-I|             TODO
 * |[#|                   TODO
 * |['|                   TODO
 * |[(|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionUnmatchedParenOpenAction}
 * |[star|                TODO
 * |[`|                   TODO
 * |[/|                   TODO
 * |[D|                   TODO
 * |[I|                   TODO
 * |[M|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionMethodPreviousEndAction}
 * |[P|                   {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |[P|                   {@link com.maddyhome.idea.vim.action.copy.PutTextBeforeCursorNoIndentAction}
 * |[[|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionSectionBackwardStartAction}
 * |[]|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionSectionBackwardEndAction}
 * |[c|                   TODO
 * |[d|                   TODO
 * |[f|                   TODO
 * |[i|                   TODO
 * |[m|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionMethodPreviousStartAction}
 * |[p|                   {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |[p|                   {@link com.maddyhome.idea.vim.action.copy.PutTextAfterCursorNoIndentAction}
 * |[s|                   TODO
 * |[z|                   TODO
 * |[{|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionUnmatchedBraceOpenAction}
 * |]_CTRL-D|             TODO
 * |]_CTRL-I|             TODO
 * |]#|                   TODO
 * |]'|                   TODO
 * |])|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionUnmatchedParenCloseAction}
 * |]star|                TODO
 * |]`|                   TODO
 * |]/|                   TODO
 * |]D|                   TODO
 * |]I|                   TODO
 * |]M|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionMethodNextEndAction}
 * |]P|                   {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |]P|                   {@link com.maddyhome.idea.vim.action.copy.PutTextBeforeCursorNoIndentAction}
 * |][|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionSectionForwardStartAction}
 * |]]|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionSectionForwardEndAction}
 * |]c|                   TODO
 * |]d|                   TODO
 * |]f|                   TODO
 * |]i|                   TODO
 * |]m|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionMethodNextStartAction}
 * |]p|                   {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |]p|                   {@link com.maddyhome.idea.vim.action.copy.PutTextAfterCursorNoIndentAction}
 * |]s|                   TODO
 * |]z|                   TODO
 * |]}|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionUnmatchedBraceCloseAction}
 *
 *
 * 2.4. Commands starting with 'g'
 *
 * tag                    action
 * -------------------------------------------------------------------------------------------------------------------
 *
 * |g_CTRL-A|             not applicable
 * |g_CTRL-G|             {@link com.maddyhome.idea.vim.action.file.FileGetLocationInfoAction}
 * |g_CTRL-H|             TODO
 * |g_CTRL-]|             TODO
 * |g#|                   {@link com.maddyhome.idea.vim.action.motion.search.SearchWordBackwardAction}
 * |g$|                   {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastScreenColumnAction}
 * |g&|                   {@link com.maddyhome.idea.vim.action.change.change.ChangeLastGlobalSearchReplaceAction}
 * |g'|                   TODO
 * |g`|                   TODO
 * |gstar|                {@link com.maddyhome.idea.vim.action.motion.search.SearchWordForwardAction}
 * |g+|                   TODO
 * |g,|                   TODO
 * |g-|                   TODO
 * |g0|                   {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstScreenColumnAction}
 * |g8|                   {@link com.maddyhome.idea.vim.action.file.FileGetHexAction}
 * |g;|                   TODO
 * |g<|                   TODO
 * |g?|                   TODO
 * |g?g?|                 TODO
 * |gD|                   {@link com.maddyhome.idea.vim.action.motion.search.GotoDeclarationAction}
 * |gE|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionBigWordEndLeftAction}
 * |gH|                   TODO
 * |gI|                   {@link com.maddyhome.idea.vim.action.change.insert.InsertLineStartAction}
 * |gJ|                   {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinLinesAction}
 * |gN|                   TODO
 * |gP|                   {@link com.maddyhome.idea.vim.action.copy.PutTextBeforeCursorActionMoveCursor}
 * |gQ|                   TODO
 * |gR|                   TODO
 * |gT|                   {@link com.maddyhome.idea.vim.action.motion.tabs.MotionPreviousTabAction}
 * |gU|                   {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseUpperMotionAction}
 * |gV|                   TODO
 * |g]|                   TODO
 * |g^|                   {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstScreenNonSpaceAction}
 * |g_|                   {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastNonSpaceAction}
 * |ga|                   {@link com.maddyhome.idea.vim.action.file.FileGetAsciiAction}
 * |gd|                   {@link com.maddyhome.idea.vim.action.motion.search.GotoDeclarationAction}
 * |ge|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionWordEndLeftAction}
 * |gf|                   TODO
 * |gF|                   TODO
 * |gg|                   {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineFirstAction}
 * |gi|                   {@link com.maddyhome.idea.vim.action.change.insert.InsertAtPreviousInsertAction}
 * |gj|                   TODO
 * |gk|                   TODO
 * |gn|                   TODO
 * |gm|                   {@link com.maddyhome.idea.vim.action.motion.leftright.MotionMiddleColumnAction}
 * |go|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionNthCharacterAction}
 * |gp|                   {@link com.maddyhome.idea.vim.action.copy.PutVisualTextMoveCursorAction}
 * |gp|                   {@link com.maddyhome.idea.vim.action.copy.PutTextAfterCursorActionMoveCursor}
 * |gq|                   TODO
 * |gr|                   TODO
 * |gs|                   TODO
 * |gt|                   {@link com.maddyhome.idea.vim.action.motion.tabs.MotionNextTabAction}
 * |gu|                   {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseLowerMotionAction}
 * |gv|                   {@link com.maddyhome.idea.vim.action.motion.visual.VisualSelectPreviousAction}
 * |gw|                   TODO
 * |g@|                   {@link com.maddyhome.idea.vim.action.change.OperatorAction}
 * |g~|                   {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseToggleMotionAction}
 * |g<Down>|              TODO
 * |g<End>|               {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastScreenColumnAction}
 * |g<Home>|              {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstScreenColumnAction}
 * |g<Up>|                TODO
 *
 *
 * 2.5. Commands starting with 'z'
 *
 * tag                    action
 * -------------------------------------------------------------------------------------------------------------------
 * |z<CR>|                {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollFirstScreenLineStartAction}
 * |z+|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollFirstScreenLinePageStartAction}
 * |z-|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLastScreenLineStartAction}
 * |z.|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollMiddleScreenLineStartAction}
 * |z=|                   TODO
 * |zA|                   TODO
 * |zC|                   IntelliJ collapse region recursively
 * |zD|                   TODO
 * |zE|                   TODO
 * |zF|                   TODO
 * |zG|                   TODO
 * |zH|                   TODO
 * |zL|                   TODO
 * |zM|                   IntelliJ collapse all regions
 * |zN|                   TODO
 * |zO|                   IntelliJ expand region recursively
 * |zR|                   IntelliJ expand all regions
 * |zW|                   TODO
 * |zX|                   TODO
 * |z^|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLastScreenLinePageStartAction}
 * |za|                   TODO
 * |zb|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLastScreenLineAction}
 * |zc|                   IntelliJ collapse region
 * |zd|                   not applicable
 * |ze|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLastScreenColumnAction}
 * |zf|                   not applicable
 * |zg|                   TODO
 * |zh|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollColumnRightAction}
 * |zi|                   TODO
 * |zj|                   TODO
 * |zk|                   TODO
 * |zl|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollColumnLeftAction}
 * |zm|                   TODO
 * |zn|                   TODO
 * |zo|                   IntelliJ expand region
 * |zr|                   TODO
 * |zs|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollFirstScreenColumnAction}
 * |zt|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollFirstScreenLineAction}
 * |zv|                   TODO
 * |zw|                   TODO
 * |zx|                   TODO
 * |zz|                   {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollMiddleScreenLineAction}
 * |z<Left>|              {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollColumnRightAction}
 * |z<Right>|             {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollColumnLeftAction}
 *
 *
 * 3. Visual mode
 *
 * tag                    action
 * -------------------------------------------------------------------------------------------------------------------
 *
 * |v_CTRL-\_CTRL-N|      {@link com.maddyhome.idea.vim.action.motion.visual.VisualExitModeAction}
 * |v_CTRL-\_CTRL-G|      TODO
 * |v_CTRL-C|             {@link com.maddyhome.idea.vim.action.motion.visual.VisualExitModeAction}
 * |v_CTRL-G|             TODO
 * |v_<BS>|               NVO mapping
 * |v_CTRL-H|             NVO mapping
 * |v_CTRL-O|             TODO
 * |v_CTRL-V|             NVO mapping
 * |v_<Esc>|              {@link com.maddyhome.idea.vim.action.motion.visual.VisualExitModeAction}
 * |v_CTRL-]|             TODO
 * |v_!|                  {@link com.maddyhome.idea.vim.action.change.change.FilterVisualLinesAction}
 * |v_:|                  NVO mapping
 * |v_<|                  {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftVisualAction}
 * |v_=|                  {@link com.maddyhome.idea.vim.action.change.change.AutoIndentLinesVisualAction}
 * |v_>|                  {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightVisualAction}
 * |v_b_A|                {@link com.maddyhome.idea.vim.action.change.insert.VisualBlockAppendAction}
 * |v_C|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualLinesEndAction}
 * |v_D|                  {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualLinesEndAction}
 * |v_b_I|                {@link com.maddyhome.idea.vim.action.change.insert.VisualBlockInsertAction}
 * |v_J|                  {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinVisualLinesSpacesAction}
 * |v_K|                  TODO
 * |v_O|                  {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapEndsBlockAction}
 * |v_P|                  {@link com.maddyhome.idea.vim.action.copy.PutVisualTextAction}
 * |v_R|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualLinesAction}
 * |v_S|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualLinesAction}
 * |v_U|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseUpperVisualAction}
 * |v_V|                  NV mapping
 * |v_X|                  {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualLinesAction}
 * |v_Y|                  {@link com.maddyhome.idea.vim.action.copy.YankVisualLinesAction}
 * |v_aquote|             {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockDoubleQuoteAction}
 * |v_a'|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockSingleQuoteAction}
 * |v_a(|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockParenAction}
 * |v_a)|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockParenAction}
 * |v_a<|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockAngleAction}
 * |v_a>|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockAngleAction}
 * |v_aB|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBraceAction}
 * |v_aW|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBigWordAction}
 * |v_a[|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBracketAction}
 * |v_a]|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBracketAction}
 * |v_a`|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBackQuoteAction}
 * |v_ab|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockParenAction}
 * |v_ap|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterParagraphAction}
 * |v_as|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterSentenceAction}
 * |v_at|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockTagAction}
 * |v_aw|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterWordAction}
 * |v_a{|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBraceAction}
 * |v_a}|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBraceAction}
 * |v_c|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualAction}
 * |v_d|                  {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualAction}
 * |v_gJ|                 {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinVisualLinesAction}
 * |v_gq|                 {@link com.maddyhome.idea.vim.action.change.change.ReformatCodeVisualAction}
 * |v_gv|                 {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapSelectionsAction}
 * |v_iquote|             {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockDoubleQuoteAction}
 * |v_i'|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockSingleQuoteAction}
 * |v_i(|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockParenAction}
 * |v_i)|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockParenAction}
 * |v_i<|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBackQuoteAction}
 * |v_i>|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBackQuoteAction}
 * |v_iB|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBraceAction}
 * |v_iW|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBigWordAction}
 * |v_i[|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBracketAction}
 * |v_i]|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBracketAction}
 * |v_i`|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBackQuoteAction}
 * |v_ib|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockParenAction}
 * |v_ip|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerParagraphAction}
 * |v_is|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerSentenceAction}
 * |v_it|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockTagAction}
 * |v_iw|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerWordAction}
 * |v_i{|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBraceAction}
 * |v_i}|                 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBraceAction}
 * |v_o|                  {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapEndsAction}
 * |v_p|                  {@link com.maddyhome.idea.vim.action.copy.PutVisualTextAction}
 * |v_r|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualCharacterAction}
 * |v_s|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualAction}
 * |v_u|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseLowerVisualAction}
 * |v_v|                  NV mapping
 * |v_x|                  {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualAction}
 * |v_y|                  {@link com.maddyhome.idea.vim.action.copy.YankVisualAction}
 * |v_~|                  {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseToggleVisualAction}
 *
 *
 * 4. Command line editing
 *
 * There is no up-to-date list of supported command line editing commands.
 *
 * 5. Ex commands
 *
 * tag                    handler
 * -------------------------------------------------------------------------------------------------------------------
 *
 * |:map|                 {@link com.maddyhome.idea.vim.ex.handler.MapHandler}
 * |:nmap|                ...
 * |:vmap|                ...
 * |:omap|                ...
 * |:imap|                ...
 * |:cmap|                ...
 * |:noremap|             ...
 * |:nnoremap|            ...
 * |:vnoremap|            ...
 * |:onoremap|            ...
 * |:inoremap|            ...
 * |:cnoremap|            ...
 * |:sort|                {@link com.maddyhome.idea.vim.ex.handler.SortHandler}
 * |:source|              {@link com.maddyhome.idea.vim.ex.handler.SourceHandler}
 * ...
 *
 * The list of supported Ex commands is incomplete.
 *
 *
 * A. Misc commands
 *
 * tag                    handler
 * -------------------------------------------------------------------------------------------------------------------
 * |]b|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionCamelEndLeftAction}
 * |]w|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionCamelEndRightAction}
 * |[b|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionCamelLeftAction}
 * |[w|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionCamelRightAction}
 * |g(|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionSentencePreviousEndAction}
 * |g)|                   {@link com.maddyhome.idea.vim.action.motion.text.MotionSentenceNextEndAction}
 *
 *
 * See also :help index.
 *
 * @author vlan
 */
package com.maddyhome.idea.vim;
