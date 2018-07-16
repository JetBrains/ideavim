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
 * <p>
 * <p>
 * 1. Insert mode
 * <p>
 * tag                    M action
 * ---------------------------------------------------------------------------------------------------------------------
 * <p>
 * |i_CTRL-@|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertPreviousInsertExitAction}
 * |i_CTRL-A|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertPreviousInsertAction}
 * |i_CTRL-C|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-D|             1 {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftLinesAction}
 * |i_CTRL-E|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertCharacterBelowCursorAction}
 * |i_CTRL-G_j|             TODO
 * |i_CTRL-G_k|             TODO
 * |i_CTRL-G_u|             TODO
 * |i_<BS>|               1 IntelliJ editor backspace
 * |i_digraph|            1 IdeaVim enter digraph
 * |i_CTRL-H|             1 IntelliJ editor backspace
 * |i_<Tab>|              1 IntelliJ editor tab
 * |i_CTRL-I|             1 IntelliJ editor tab
 * |i_<NL>|               1 {@link com.maddyhome.idea.vim.action.change.insert.InsertEnterAction}
 * |i_CTRL-J|               TODO
 * |i_CTRL-K|             1 IdeaVim enter digraph
 * |i_CTRL-L|               TODO
 * |i_<CR>|               1 {@link com.maddyhome.idea.vim.action.change.insert.InsertEnterAction}
 * |i_CTRL-M|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertEnterAction}
 * |i_CTRL-N|               TODO
 * |i_CTRL-O|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertSingleCommandAction}
 * |i_CTRL-P|               TODO
 * |i_CTRL-Q|               TODO
 * |i_CTRL-R|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertRegisterAction}
 * |i_CTRL-R_CTRL-R|        TODO
 * |i_CTRL-R_CTRL-O|        TODO
 * |i_CTRL-R_CTRL-P|        TODO
 * |i_CTRL-T|               {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightLinesAction}
 * |i_CTRL-U|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertDeleteInsertedTextAction}
 * |i_CTRL-V|               TODO
 * |i_CTRL-V_digit|         TODO
 * |i_CTRL-W|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertDeletePreviousWordAction}
 * |i_CTRL-X|               TODO
 * |i_CTRL-Y|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertCharacterAboveCursorAction}
 * |i_CTRL-Z|               TODO
 * |i_<Esc>|              1 {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-[|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-\_CTRL-N|      1 {@link com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction}
 * |i_CTRL-\_CTRL-G|        TODO
 * |i_CTRL-]}               TODO
 * |i_CTRL-^|               TODO
 * |i_CTRL-_|               TODO
 * |i_0_CTRL-D|             TODO
 * |i_^_CTRL-D|             TODO
 * |i_<Del>|              1 IntelliJ editor delete
 * |i_<Left>|             1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftAction}
 * |i_<S-Left>|           1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |i_<C-Left>|           1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |i_<Right>|            1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightAction}
 * |i_<S-Right>|          1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |i_<C-Right>|          1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |i_<Up>|               1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpAction}
 * |i_<S-Up>|             ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |i_<Down>|             1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |i_<S-Down>|           ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |i_<Home>|             1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstColumnAction}
 * |i_<C-Home>|           1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineFirstAction}
 * |i_<End>|              1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastColumnAction}
 * |i_<C-End>|            1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineLastEndAction}
 * |i_<Insert>|           1 {@link com.maddyhome.idea.vim.action.change.insert.InsertInsertAction}
 * |i_<PageUp>|           ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |i_<PageDown>|         ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |i_<F1>|                 IntelliJ help
 * |i_<Insert>|           1 IntelliJ editor toggle insert/replace
 * |i_CTRL-X_index|         TODO
 * <p>
 * <p>
 * 2. Normal mode
 * <p>
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 * <p>
 * |CTRL-A|               1 {@link com.maddyhome.idea.vim.action.change.change.ChangeNumberIncAction}
 * |CTRL-B|               ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |CTRL-C|                 TODO
 * |CTRL-D|               ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollHalfPageDownAction}
 * |CTRL-E|               ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLineDownAction}
 * |CTRL-F|               P {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |CTRL-G|               P {@link com.maddyhome.idea.vim.action.file.FileGetFileInfoAction}
 * |<BS>|                 1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftWrapAction}
 * |CTRL-H|               1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftWrapAction}
 * |<Tab>|                  TODO
 * |CTRL-I|               1 {@link com.maddyhome.idea.vim.action.motion.mark.MotionJumpNextAction}
 * |<NL>|                 1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |CTRL-J|                 TODO
 * |CTRL-L|                 not applicable
 * |<CR>|                 1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownFirstNonSpaceAction}
 * |CTRL-M|               1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownFirstNonSpaceAction}
 * |CTRL-N|               1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |CTRL-O|               1 {@link com.maddyhome.idea.vim.action.motion.mark.MotionJumpPreviousAction}
 * |CTRL-P|                 {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpAction}
 * |CTRL-R|               1 {@link com.maddyhome.idea.vim.action.change.RedoAction}
 * |CTRL-T|                 TODO
 * |CTRL-U|               ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollHalfPageUpAction}
 * |CTRL-V|               P {@link com.maddyhome.idea.vim.action.motion.visual.VisualToggleBlockModeAction}
 * |CTRL-W|                 see window commands
 * |CTRL-X|               1 {@link com.maddyhome.idea.vim.action.change.change.ChangeNumberDecAction}
 * |CTRL-Y|               P {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLineUpAction}
 * |CTRL-Z|                 TODO
 * |CTRL-]|               P {@link com.maddyhome.idea.vim.action.motion.search.GotoDeclarationAction}
 * |<Space>|              1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightWrapAction}
 * |!|                    P {@link com.maddyhome.idea.vim.action.change.change.FilterMotionAction}
 * |!!|                   P {@link com.maddyhome.idea.vim.action.change.change.FilterCountLinesAction}
 * |quote|                P {@link com.maddyhome.idea.vim.action.copy.SelectRegisterAction}
 * |#|                    1 {@link com.maddyhome.idea.vim.action.motion.search.SearchWholeWordBackwardAction}
 * |$|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastColumnAction}
 * |%|                    1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionPercentOrMatchAction}
 * |&|                    ? {@link com.maddyhome.idea.vim.action.change.change.ChangeLastSearchReplaceAction}
 * |'|                    1 {@link com.maddyhome.idea.vim.action.motion.mark.MotionGotoFileMarkLineAction}
 * |'|                    1 {@link com.maddyhome.idea.vim.action.motion.mark.MotionGotoFileMarkAction}
 * |''|                     ?
 * ...
 * |(|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionSentencePreviousStartAction}
 * |)|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionSentenceNextStartAction}
 * |star|                 1 {@link com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction}
 * |+|                    1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownFirstNonSpaceAction}
 * |,|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastMatchCharReverseAction}
 * |-|                    1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpFirstNonSpaceAction}
 * |.|                    E {@link com.maddyhome.idea.vim.action.change.RepeatChangeAction}
 * |/|                    1 {@link com.maddyhome.idea.vim.action.motion.search.SearchEntryFwdAction}
 * |:|                    1 {@link com.maddyhome.idea.vim.action.ExEntryAction}
 * |;|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastMatchCharAction}
 * |<|                    1 {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftMotionAction}
 * |<<|                   1 {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftLinesAction}
 * |=|                    1 {@link com.maddyhome.idea.vim.action.change.shift.AutoIndentMotionAction}
 * |==|                   1 {@link com.maddyhome.idea.vim.action.change.shift.AutoIndentLinesAction}
 * |>|                    1 {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightMotionAction}
 * |>>|                   1 {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightLinesAction}
 * |?|                    1 {@link com.maddyhome.idea.vim.action.motion.search.SearchEntryRevAction}
 * |@|                    1 {@link com.maddyhome.idea.vim.action.macro.PlaybackRegisterAction}
 * |@:|                   ? {@link com.maddyhome.idea.vim.action.change.RepeatExCommandAction}
 * |@@|                   1 {@link com.maddyhome.idea.vim.action.macro.PlaybackLastRegisterAction}
 * |A|                    1 {@link com.maddyhome.idea.vim.action.change.insert.InsertAfterLineEndAction}
 * |B|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionBigWordLeftAction}
 * |C|                    1 {@link com.maddyhome.idea.vim.action.change.change.ChangeEndOfLineAction}
 * |D|                    1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteEndOfLineAction}
 * |E|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionBigWordEndRightAction}
 * |F|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftMatchCharAction}
 * |G|                    1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineLastAction}
 * |H|                    1 {@link com.maddyhome.idea.vim.action.motion.screen.MotionFirstScreenLineAction}
 * |I|                    1 {@link com.maddyhome.idea.vim.action.change.insert.InsertBeforeFirstNonBlankAction}
 * |J|                    1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinLinesSpacesAction}
 * |K|                      TODO
 * |L|                    1 {@link com.maddyhome.idea.vim.action.motion.screen.MotionLastScreenLineAction}
 * |M|                    1 {@link com.maddyhome.idea.vim.action.motion.screen.MotionMiddleScreenLineAction}
 * |N|                    1 {@link com.maddyhome.idea.vim.action.motion.search.SearchAgainPreviousAction}
 * |O|                    1 {@link com.maddyhome.idea.vim.action.change.insert.InsertNewLineAboveAction}
 * |P|                    1 {@link com.maddyhome.idea.vim.action.copy.PutTextBeforeCursorAction}
 * |Q|                      TODO
 * |R|                    1 {@link com.maddyhome.idea.vim.action.change.change.ChangeReplaceAction}
 * |S|                    1 {@link com.maddyhome.idea.vim.action.change.change.ChangeLineAction}
 * |T|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftTillMatchCharAction}
 * |U|                      ?
 * |V|                    1 {@link com.maddyhome.idea.vim.action.motion.visual.VisualToggleLineModeAction}
 * |W|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionBigWordRightAction}
 * |X|                    1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteCharacterLeftAction}
 * |Y|                    P {@link com.maddyhome.idea.vim.action.copy.YankLineAction}
 * |ZZ|                   P {@link com.maddyhome.idea.vim.action.file.FileSaveCloseAction}
 * |ZQ|                   P {@link com.maddyhome.idea.vim.action.file.FileSaveCloseAction}
 * |[|                      see bracket commands
 * |]|                      see bracket commands
 * |^|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstNonSpaceAction}
 * |_|                    1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownLess1FirstNonSpaceAction}
 * |`|                    1 {@link com.maddyhome.idea.vim.action.motion.mark.MotionGotoFileMarkAction}
 * |`|                    1 {@link com.maddyhome.idea.vim.action.motion.mark.MotionGotoMarkAction}
 * |``|                     ?
 * ...
 * |0|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstColumnAction}
 * |a|                    1 {@link com.maddyhome.idea.vim.action.change.insert.InsertAfterCursorAction}
 * |b|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |c|                    1 {@link com.maddyhome.idea.vim.action.change.change.ChangeMotionAction}
 * |cc|                   1 {@link com.maddyhome.idea.vim.action.change.change.ChangeLineAction}
 * |d|                    1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteMotionAction}
 * |dd|                   1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteLineAction}
 * |do|                     TODO
 * |dp|                     TODO
 * |e|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordEndRightAction}
 * |f|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightMatchCharAction}
 * |g|                      see commands starting with 'g'
 * |h|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftAction}
 * |i|                    1 {@link com.maddyhome.idea.vim.action.change.insert.InsertBeforeCursorAction}
 * |j|                    1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |k|                    1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpAction}
 * |l|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightAction}
 * |m|                    P {@link com.maddyhome.idea.vim.action.motion.mark.MotionMarkAction}
 * |n|                    1 {@link com.maddyhome.idea.vim.action.motion.search.SearchAgainNextAction}
 * |o|                    1 {@link com.maddyhome.idea.vim.action.change.insert.InsertNewLineBelowAction}
 * |p|                    1 {@link com.maddyhome.idea.vim.action.copy.PutTextAfterCursorAction}
 * |q|                    1 {@link com.maddyhome.idea.vim.action.macro.ToggleRecordingAction}
 * |r|                    1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCharacterAction}
 * |s|                    1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCharactersAction}
 * |t|                    1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightTillMatchCharAction}
 * |u|                    1 {@link com.maddyhome.idea.vim.action.change.UndoAction}
 * |v|                    1 {@link com.maddyhome.idea.vim.action.motion.visual.VisualToggleCharacterModeAction}
 * |w|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |x|                    1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteCharacterRightAction}
 * |y|                    P {@link com.maddyhome.idea.vim.action.copy.YankMotionAction}
 * |yy|                   P {@link com.maddyhome.idea.vim.action.copy.YankLineAction}
 * |z|                      see commands starting with 'z'
 * |{|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionParagraphPreviousAction}
 * |bar|                  1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionColumnAction}
 * |}|                    1 {@link com.maddyhome.idea.vim.action.motion.text.MotionParagraphNextAction}
 * |~|                    1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseToggleCharacterAction}
 * |<C-End>|              1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineLastEndAction}
 * |<C-Home>|             1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineFirstAction}
 * |<C-Left>|             1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |<C-Right>|            1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |<Del>|                1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteCharacterAction}
 * |<Down>|               1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionDownAction}
 * |<End>|                1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastColumnAction}
 * |<F1>|                   IntelliJ help
 * |<Home>|               1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstColumnAction}
 * |<Insert>|             1 {@link com.maddyhome.idea.vim.action.change.insert.InsertBeforeCursorAction}
 * |<Left>|               1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLeftAction}
 * |<PageDown>|           P {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |<PageUp>|             P {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |<Right>|              1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionRightAction}
 * |<S-Down>|             ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageDownAction}
 * |<S-Left>|             1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordLeftAction}
 * |<S-Right>|            1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordRightAction}
 * |<S-Up>|               ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollPageUpAction}
 * |<Up>|                 1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionUpAction}
 * <p>
 * <p>
 * 2.1. Text objects
 * <p>
 * Text object commands are listed in the visual mode section.
 * <p>
 * <p>
 * 2.2. Window commands
 * <p>
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 * <p>
 * |CTRL-W_+|               TODO
 * |CTRL-W_-|               TODO
 * |CTRL-W_<|               TODO
 * |CTRL-W_=|               TODO
 * |CTRL-W_>|               TODO
 * |CTRL-W_H|               TODO
 * |CTRL-W_J|               TODO
 * |CTRL-W_K|               TODO
 * |CTRL-W_L|               TODO
 * |CTRL-W_P|               TODO
 * |CTRL-W_R|               TODO
 * |CTRL-W_S|             P {@link com.maddyhome.idea.vim.action.window.HorizontalSplitAction}
 * |CTRL-W_T|               TODO
 * |CTRL-W_W|             P {@link com.maddyhome.idea.vim.action.window.WindowPrevAction}
 * |CTRL-W_]|               TODO
 * |CTRL-W_^|               TODO
 * |CTRL-W__|               TODO
 * |CTRL-W_b|               TODO
 * |CTRL-W_c|             P {@link com.maddyhome.idea.vim.action.window.CloseWindowAction}
 * |CTRL-W_d|               TODO
 * |CTRL-W_f|               TODO
 * |CTRL-W-F|               TODO
 * |CTRL-W-g]|              TODO
 * |CTRL-W-g}|              TODO
 * |CTRL-W-gf|              TODO
 * |CTRL-W-gF|              TODO
 * |CTRL-W_h|             P {@link com.maddyhome.idea.vim.action.window.WindowLeftAction}
 * |CTRL-W_i|               TODO
 * |CTRL-W_j|             P {@link com.maddyhome.idea.vim.action.window.WindowDownAction}
 * |CTRL-W_k|             P {@link com.maddyhome.idea.vim.action.window.WindowUpAction}
 * |CTRL-W_l|             P {@link com.maddyhome.idea.vim.action.window.WindowRightAction}
 * |CTRL-W_n|               TODO
 * |CTRL-W_o|             P {@link com.maddyhome.idea.vim.action.window.WindowOnlyAction}
 * |CTRL-W_p|               TODO
 * |CTRL-W_q|               TODO
 * |CTRL-W_r|               TODO
 * |CTRL-W_s|             P {@link com.maddyhome.idea.vim.action.window.HorizontalSplitAction}
 * |CTRL-W_t|               TODO
 * |CTRL-W_v|             P {@link com.maddyhome.idea.vim.action.window.VerticalSplitAction}
 * |CTRL-W_w|             P {@link com.maddyhome.idea.vim.action.window.WindowNextAction}
 * |CTRL-W_x|               TODO
 * |CTRL-W_z|               TODO
 * |CTRL-W_bar|             TODO
 * |CTRL-W_}|               TODO
 * |CTRL-W_<Down>|        P {@link com.maddyhome.idea.vim.action.window.WindowDownAction}
 * |CTRL-W_<Up>|          P {@link com.maddyhome.idea.vim.action.window.WindowUpAction}
 * |CTRL-W_<Left>|        P {@link com.maddyhome.idea.vim.action.window.WindowLeftAction}
 * |CTRL-W_<Right>|       P {@link com.maddyhome.idea.vim.action.window.WindowRightAction}
 * <p>
 * <p>
 * 2.3. Square bracket commands
 * <p>
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 * |[_CTRL-D|               TODO
 * |[_CTRL-I|               TODO
 * |[#|                     TODO
 * |['|                     TODO
 * |[(|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionUnmatchedParenOpenAction}
 * |[star|                  TODO
 * |[`|                     TODO
 * |[/|                     TODO
 * |[D|                     TODO
 * |[I|                     TODO
 * |[M|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionMethodPreviousEndAction}
 * |[P|                   1 {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |[P|                   1 {@link com.maddyhome.idea.vim.action.copy.PutTextBeforeCursorNoIndentAction}
 * |[[|                   ? {@link com.maddyhome.idea.vim.action.motion.text.MotionSectionBackwardStartAction}
 * |[]|                   ? {@link com.maddyhome.idea.vim.action.motion.text.MotionSectionBackwardEndAction}
 * |[c|                     TODO
 * |[d|                     TODO
 * |[f|                     TODO
 * |[i|                     TODO
 * |[m|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionMethodPreviousStartAction}
 * |[p|                   1 {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |[p|                   1 {@link com.maddyhome.idea.vim.action.copy.PutTextAfterCursorNoIndentAction}
 * |[s|                     TODO
 * |[z|                     TODO
 * |[{|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionUnmatchedBraceOpenAction}
 * |]_CTRL-D|               TODO
 * |]_CTRL-I|               TODO
 * |]#|                     TODO
 * |]'|                     TODO
 * |])|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionUnmatchedParenCloseAction}
 * |]star|                  TODO
 * |]`|                     TODO
 * |]/|                     TODO
 * |]D|                     TODO
 * |]I|                     TODO
 * |]M|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionMethodNextEndAction}
 * |]P|                   1 {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |]P|                   1 {@link com.maddyhome.idea.vim.action.copy.PutTextBeforeCursorNoIndentAction}
 * |][|                   ? {@link com.maddyhome.idea.vim.action.motion.text.MotionSectionForwardStartAction}
 * |]]|                   ? {@link com.maddyhome.idea.vim.action.motion.text.MotionSectionForwardEndAction}
 * |]c|                     TODO
 * |]d|                     TODO
 * |]f|                     TODO
 * |]i|                     TODO
 * |]m|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionMethodNextStartAction}
 * |]p|                   1 {@link com.maddyhome.idea.vim.action.copy.PutVisualTextNoIndentAction}
 * |]p|                   1 {@link com.maddyhome.idea.vim.action.copy.PutTextAfterCursorNoIndentAction}
 * |]s|                     TODO
 * |]z|                     TODO
 * |]}|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionUnmatchedBraceCloseAction}
 * <p>
 * <p>
 * 2.4. Commands starting with 'g'
 * <p>
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 * <p>
 * |g_CTRL-A|               not applicable
 * |g_CTRL-G|             P {@link com.maddyhome.idea.vim.action.file.FileGetLocationInfoAction}
 * |g_CTRL-H|               TODO
 * |g_CTRL-]|               TODO
 * |g#|                   1 {@link com.maddyhome.idea.vim.action.motion.search.SearchWordBackwardAction}
 * |g$|                   1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastScreenColumnAction}
 * |g&|                   ? {@link com.maddyhome.idea.vim.action.change.change.ChangeLastGlobalSearchReplaceAction}
 * |g'|                     TODO
 * |g`|                     TODO
 * |gstar|                1 {@link com.maddyhome.idea.vim.action.motion.search.SearchWordForwardAction}
 * |g+|                     TODO
 * |g,|                     TODO
 * |g-|                     TODO
 * |g0|                   1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstScreenColumnAction}
 * |g8|                   P {@link com.maddyhome.idea.vim.action.file.FileGetHexAction}
 * |g;|                     TODO
 * |g<|                     TODO
 * |g?|                     TODO
 * |g?g?|                   TODO
 * |gD|                   P {@link com.maddyhome.idea.vim.action.motion.search.GotoDeclarationAction}
 * |gE|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionBigWordEndLeftAction}
 * |gH|                     TODO
 * |gI|                   1 {@link com.maddyhome.idea.vim.action.change.insert.InsertLineStartAction}
 * |gJ|                   1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinLinesAction}
 * |gN|                     TODO
 * |gP|                   1 {@link com.maddyhome.idea.vim.action.copy.PutTextBeforeCursorActionMoveCursor}
 * |gQ|                     TODO
 * |gR|                     TODO
 * |gT|                   P {@link com.maddyhome.idea.vim.action.motion.tabs.MotionPreviousTabAction}
 * |gU|                   1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseUpperMotionAction}
 * |gV|                     TODO
 * |g]|                     TODO
 * |g^|                   1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstScreenNonSpaceAction}
 * |g_|                   1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastNonSpaceAction}
 * |ga|                   P {@link com.maddyhome.idea.vim.action.file.FileGetAsciiAction}
 * |gd|                   P {@link com.maddyhome.idea.vim.action.motion.search.GotoDeclarationAction}
 * |ge|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionWordEndLeftAction}
 * |gf|                     TODO
 * |gF|                     TODO
 * |gg|                   1 {@link com.maddyhome.idea.vim.action.motion.updown.MotionGotoLineFirstAction}
 * |gi|                   P {@link com.maddyhome.idea.vim.action.change.insert.InsertAtPreviousInsertAction}
 * |gj|                     TODO
 * |gk|                     TODO
 * |gn|                     TODO
 * |gm|                   1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionMiddleColumnAction}
 * |go|                   P {@link com.maddyhome.idea.vim.action.motion.text.MotionNthCharacterAction}
 * |gp|                   1 {@link com.maddyhome.idea.vim.action.copy.PutVisualTextMoveCursorAction}
 * |gp|                   1 {@link com.maddyhome.idea.vim.action.copy.PutTextAfterCursorActionMoveCursor}
 * |gq|                     TODO
 * |gr|                     TODO
 * |gs|                     TODO
 * |gt|                   P {@link com.maddyhome.idea.vim.action.motion.tabs.MotionNextTabAction}
 * |gu|                   1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseLowerMotionAction}
 * |gv|                   E {@link com.maddyhome.idea.vim.action.motion.visual.VisualSelectPreviousAction}
 * |gw|                     TODO
 * |g@|                   ? {@link com.maddyhome.idea.vim.action.change.OperatorAction}
 * |g~|                   1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseToggleMotionAction}
 * |g<Down>|                TODO
 * |g<End>|               1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionLastScreenColumnAction}
 * |g<Home>|              1 {@link com.maddyhome.idea.vim.action.motion.leftright.MotionFirstScreenColumnAction}
 * |g<Up>|                  TODO
 * <p>
 * <p>
 * 2.5. Commands starting with 'z'
 * <p>
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 * |z<CR>|                ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollFirstScreenLineStartAction}
 * |z+|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollFirstScreenLinePageStartAction}
 * |z-|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLastScreenLineStartAction}
 * |z.|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollMiddleScreenLineStartAction}
 * |z=|                     TODO
 * |zA|                     TODO
 * |zC|                     IntelliJ collapse region recursively
 * |zD|                     TODO
 * |zE|                     TODO
 * |zF|                     TODO
 * |zG|                     TODO
 * |zH|                     TODO
 * |zL|                     TODO
 * |zM|                     IntelliJ collapse all regions
 * |zN|                     TODO
 * |zO|                     IntelliJ expand region recursively
 * |zR|                     IntelliJ expand all regions
 * |zW|                     TODO
 * |zX|                     TODO
 * |z^|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLastScreenLinePageStartAction}
 * |za|                     TODO
 * |zb|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLastScreenLineAction}
 * |zc|                     IntelliJ collapse region
 * |zd|                     not applicable
 * |ze|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollLastScreenColumnAction}
 * |zf|                     not applicable
 * |zg|                     TODO
 * |zh|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollColumnRightAction}
 * |zi|                     TODO
 * |zj|                     TODO
 * |zk|                     TODO
 * |zl|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollColumnLeftAction}
 * |zm|                     TODO
 * |zn|                     TODO
 * |zo|                     IntelliJ expand region
 * |zr|                     TODO
 * |zs|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollFirstScreenColumnAction}
 * |zt|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollFirstScreenLineAction}
 * |zv|                     TODO
 * |zw|                     TODO
 * |zx|                     TODO
 * |zz|                   ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollMiddleScreenLineAction}
 * |z<Left>|              ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollColumnRightAction}
 * |z<Right>|             ? {@link com.maddyhome.idea.vim.action.motion.scroll.MotionScrollColumnLeftAction}
 * <p>
 * <p>
 * 3. Visual mode
 * <p>
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 * <p>
 * |v_CTRL-\_CTRL-N|      1 {@link com.maddyhome.idea.vim.action.motion.visual.VisualExitModeAction}
 * |v_CTRL-\_CTRL-G|        TODO
 * |v_CTRL-C|             1 {@link com.maddyhome.idea.vim.action.motion.visual.VisualExitModeAction}
 * |v_CTRL-G|               TODO
 * |v_<BS>|                 NVO mapping
 * |v_CTRL-H|               NVO mapping
 * |v_CTRL-O|               TODO
 * |v_CTRL-V|               NVO mapping
 * |v_<Esc>|              1 {@link com.maddyhome.idea.vim.action.motion.visual.VisualExitModeAction}
 * |v_CTRL-]|               TODO
 * |v_!|                  P {@link com.maddyhome.idea.vim.action.change.change.FilterVisualLinesAction}
 * |v_:|                    NVO mapping
 * |v_<|                  1 {@link com.maddyhome.idea.vim.action.change.shift.ShiftLeftVisualAction}
 * |v_=|                  1 {@link com.maddyhome.idea.vim.action.change.change.AutoIndentLinesVisualAction}
 * |v_>|                  1 {@link com.maddyhome.idea.vim.action.change.shift.ShiftRightVisualAction}
 * |v_b_A|                1 {@link com.maddyhome.idea.vim.action.change.insert.VisualBlockAppendAction}
 * |v_C|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualLinesEndAction}
 * |v_D|                  1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualLinesEndAction}
 * |v_b_I|                1 {@link com.maddyhome.idea.vim.action.change.insert.VisualBlockInsertAction}
 * |v_J|                  1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinVisualLinesSpacesAction}
 * |v_K|                    TODO
 * |v_O|                  1 {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapEndsBlockAction}
 * |v_P|                  1 {@link com.maddyhome.idea.vim.action.copy.PutVisualTextAction}
 * |v_R|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualLinesAction}
 * |v_S|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualLinesAction}
 * |v_U|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseUpperVisualAction}
 * |v_V|                    NV mapping
 * |v_X|                  1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualLinesAction}
 * |v_Y|                  E {@link com.maddyhome.idea.vim.action.copy.YankVisualLinesAction}
 * |v_aquote|             1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockDoubleQuoteAction}
 * |v_a'|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockSingleQuoteAction}
 * |v_a(|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockParenAction}
 * |v_a)|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockParenAction}
 * |v_a<|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockAngleAction}
 * |v_a>|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockAngleAction}
 * |v_aB|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBraceAction}
 * |v_aW|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBigWordAction}
 * |v_a[|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBracketAction}
 * |v_a]|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBracketAction}
 * |v_a`|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBackQuoteAction}
 * |v_ab|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockParenAction}
 * |v_ap|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterParagraphAction}
 * |v_as|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterSentenceAction}
 * |v_at|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockTagAction}
 * |v_aw|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterWordAction}
 * |v_a{|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBraceAction}
 * |v_a}|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionOuterBlockBraceAction}
 * |v_c|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualAction}
 * |v_d|                  1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualAction}
 * |v_gJ|                 1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteJoinVisualLinesAction}
 * |v_gq|                 ? {@link com.maddyhome.idea.vim.action.change.change.ReformatCodeVisualAction}
 * |v_gv|                 E {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapSelectionsAction}
 * |v_iquote|             1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockDoubleQuoteAction}
 * |v_i'|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockSingleQuoteAction}
 * |v_i(|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockParenAction}
 * |v_i)|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockParenAction}
 * |v_i<|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBackQuoteAction}
 * |v_i>|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBackQuoteAction}
 * |v_iB|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBraceAction}
 * |v_iW|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBigWordAction}
 * |v_i[|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBracketAction}
 * |v_i]|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBracketAction}
 * |v_i`|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBackQuoteAction}
 * |v_ib|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockParenAction}
 * |v_ip|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerParagraphAction}
 * |v_is|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerSentenceAction}
 * |v_it|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockTagAction}
 * |v_iw|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerWordAction}
 * |v_i{|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBraceAction}
 * |v_i}|                 1 {@link com.maddyhome.idea.vim.action.motion.object.MotionInnerBlockBraceAction}
 * |v_o|                  1 {@link com.maddyhome.idea.vim.action.motion.visual.VisualSwapEndsAction}
 * |v_p|                  1 {@link com.maddyhome.idea.vim.action.copy.PutVisualTextAction}
 * |v_r|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualCharacterAction}
 * |v_s|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeVisualAction}
 * |v_u|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseLowerVisualAction}
 * |v_v|                    NV mapping
 * |v_x|                  1 {@link com.maddyhome.idea.vim.action.change.delete.DeleteVisualAction}
 * |v_y|                  E {@link com.maddyhome.idea.vim.action.copy.YankVisualAction}
 * |v_~|                  1 {@link com.maddyhome.idea.vim.action.change.change.ChangeCaseToggleVisualAction}
 * <p>
 * <p>
 * 4. Command line editing
 * <p>
 * There is no up-to-date list of supported command line editing commands.
 * <p>
 * 5. Ex commands
 * <p>
 * tag                      handler
 * ---------------------------------------------------------------------------------------------------------------------
 * <p>
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
 * ...
 * <p>
 * The list of supported Ex commands is incomplete.
 * <p>
 * <p>
 * A. Misc commands
 * <p>
 * tag                      handler
 * ---------------------------------------------------------------------------------------------------------------------
 * |]b|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionCamelEndLeftAction}
 * |]w|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionCamelEndRightAction}
 * |[b|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionCamelLeftAction}
 * |[w|                   1 {@link com.maddyhome.idea.vim.action.motion.text.MotionCamelRightAction}
 * |g(|                   ? {@link com.maddyhome.idea.vim.action.motion.text.MotionSentencePreviousEndAction}
 * |g)|                   ? {@link com.maddyhome.idea.vim.action.motion.text.MotionSentenceNextEndAction}
 * <p>
 * <p>
 * See also :help index.
 * <p>
 * M - multi-caret support
 * P - primary caret handler
 * E - errors in the implementation
 * 1 - works for simple cases
 * ? - not checked properly
 *
 * @author vlan
 */
package com.maddyhome.idea.vim;
