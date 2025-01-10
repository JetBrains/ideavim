/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.scroll

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_CLEAR_STROKES
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_IGNORE_SCROLL_JUMP
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

// The <S-Enter> mapping is interesting. Vim has multiple mappings to move the caret down: <Down>, obviously, but also
// <Enter>, <C-N>, `+` and `j`. While there are some differences (<Enter> and `+` have a flag that moves the caret to
// the start of the line), there is just one function to handle all of these keys and move the caret down.
// This function checks if Shift is held down, in which case it will scroll the page forward, because <S-Down> behaves
// the same as <C-F>. The side effect is that all shift+"down" shortcuts will now scroll forward, including <S-Enter>.
// However, Vim does not support shifted ctrl shortcuts because terminals only support simple ascii control characters.
// So <C-S-N> doesn't scroll forward. Shift+j becomes `J`, which joins multiple lines. And on a typical US/UK keyboard,
// `+` requires shift to type, so can only be typed with the numpad. Vim does not allow remapping <s-+>.
// (IdeaVim does not get shift+numpadPlus, only "typed +". We might get it for a keypress, but by the time it's
// converted to a typed char, we lose the modifier).
// The same logic holds for <Up> and shift - <C-P>, `k` and `-` should scroll backwards, but <C-S-P> isn't valid, `K` is
// a different action, and shift+numpadMinus works but can't be remapped (or handled by IdeaVim).
// Ironically, Vim registers separate functions for <S-Down> and <S-Up>, so the non-shifted functions don't actually
// need to check for shift. So this is all side effect...
// See https://github.com/vim/vim/issues/15107
// Note that IdeaVim handles <S-Down> separately because it behaves differently based on 'keymodel'
// TODO: Is there any way for IdeaVim to handle shift+numpadPlus?
@CommandOrMotion(keys = ["<C-F>", "<PageDown>", "<S-Enter>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionScrollPageDownAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(FLAG_IGNORE_SCROLL_JUMP)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return injector.scroll.scrollFullPage(editor, editor.primaryCaret(), cmd.count)
  }
}

@CommandOrMotion(keys = ["<PageDown>"], modes = [Mode.INSERT])
class MotionScrollPageDownInsertModeAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(FLAG_IGNORE_SCROLL_JUMP, FLAG_CLEAR_STROKES)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return injector.scroll.scrollFullPage(editor, editor.primaryCaret(), cmd.count)
  }
}
