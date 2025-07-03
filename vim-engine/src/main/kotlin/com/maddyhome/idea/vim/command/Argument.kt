/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.handler.ExternalActionHandler
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Represents an argument to a command's action
 *
 * A [Command] is made up of an optional register and count, and an action. That action might be a simple command such
 * as `i` to start Insert mode, or a motion `w` to move to the next word. Or it might require an argument, such as a
 * character like in the motion `fx` or an ex-string in the command `d/foo`. Or it might be another action, representing
 * a motion, such as `dw`. That motion argument's action might itself have an action (`dfx`).
 */
sealed class Argument {
  /** A simple character argument */
  class Character(val character: Char) : Argument()

  /** An argument representing the user's input from the Ex command line, typically a search string */
  class ExString(val label: Char, val string: String, val processing: ((String) -> Unit)?) : Argument()

  /**
   * Represents an argument that is a motion. Used by operator commands
   *
   * A command is either an action (like `i`), a motion (like `w`) or an operator that takes a motion as an argument
   * (like `dw`). A motion argument is a motion action handler with its own optional argument. The motion action handler
   * could be a [MotionActionHandler] or [TextObjectActionHandler], or even the [ExternalActionHandler] that tracks the
   * caret moves from an external action such as EasyMotion/AceJump. A motion might be a simple motion such as `w` to
   * move a word, or require a character argument (`f`), or even an ex-string (`/foo`).
   *
   * Note that a motion argument does not have a count - that is owned by the fully built command. When executing the
   * command, the count applies to the motion action, not the operator action. This just means the operator action
   * does not use the count. (`3i` means insert the following typed text three times. But `3cw` means change the next
   * three words with the following inserted text, rather than change the next word by inserting the following text
   * three times.)
   *
   * @see Command
   */
  class Motion private constructor(val motion: EditorActionHandlerBase, val argument: Argument? = null) : Argument() {
    constructor(motion: MotionActionHandler, argument: Argument?) : this(motion as EditorActionHandlerBase, argument)
    constructor(motion: TextObjectActionHandler) : this(motion as EditorActionHandlerBase)
    constructor(motion: ExternalActionHandler) : this(motion as EditorActionHandlerBase)

    fun getMotionType() = if (isLinewiseMotion()) SelectionType.LINE_WISE else SelectionType.CHARACTER_WISE

    fun isLinewiseMotion(): Boolean = when (motion) {
      is TextObjectActionHandler -> motion.visualType == TextObjectVisualType.LINE_WISE
      is MotionActionHandler -> motion.motionType == MotionType.LINE_WISE
      is ExternalActionHandler -> motion.isLinewiseMotion
      else -> error("Command is not a motion: $motion")
    }

    fun withArgument(argument: Argument) = Motion(motion, argument)
  }

  /**
   * Represents the type of argument, or the type of an expected argument while entering a command
   */
  enum class Type {

    /**
     * A motion argument used to complete an operator, such as `dw` or `diw`
     *
     * A motion argument will often have its own argument, such as when deleting up to the next occurrence of a
     * character, as in `dfx`.
     */
    MOTION,

    /** A character argument, such as the character to move to with the `f` command. */
    CHARACTER,

    /**
     * Used to represent an expected argument type rather than an actual argument type
     *
     * When building a command, an operator can say that it expects a digraph or literal argument, in which case the key
     * handler will allow `<C-K>`, `<C-V>` and `<C-Q>`, and start the digraph state machine. The finished digraph is
     * converted into a character, and a character argument is added to the operator action.
     */
    DIGRAPH
  }
}
