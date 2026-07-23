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
  class ExString(val label: Char, val string: String, val incSearchOffset: Int, val processing: ((String) -> Unit)?) :
    Argument()

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
  class Motion private constructor(
    val motion: EditorActionHandlerBase,
    val argument: Argument? = null,
    /**
     * A forced motion type entered with the `v`/`V`/`CTRL-V` modifier between an operator and its motion
     * (`:help o_v`). `v` forces [SelectionType.CHARACTER_WISE], `V` forces [SelectionType.LINE_WISE] and `CTRL-V`
     * forces [SelectionType.BLOCK_WISE]. `null` means no forcing was requested.
     */
    val forcedMotion: SelectionType? = null,
  ) : Argument() {
    constructor(motion: MotionActionHandler, argument: Argument?, forcedMotion: SelectionType? = null)
      : this(motion as EditorActionHandlerBase, argument, forcedMotion)

    constructor(motion: TextObjectActionHandler, forcedMotion: SelectionType? = null)
      : this(motion as EditorActionHandlerBase, forcedMotion = forcedMotion)

    constructor(motion: ExternalActionHandler, forcedMotion: SelectionType? = null)
      : this(motion as EditorActionHandlerBase, forcedMotion = forcedMotion)

    fun getMotionType(): SelectionType = when (forcedMotion) {
      null -> if (isLinewiseMotion()) SelectionType.LINE_WISE else SelectionType.CHARACTER_WISE
      else -> forcedMotion
    }

    fun isLinewiseMotion(): Boolean {
      // A forced motion modifier (:help o_v / o_V) overrides the motion's own declared type.
      forcedMotion?.let { return it == SelectionType.LINE_WISE }
      return when (motion) {
        is TextObjectActionHandler -> motion.visualType == TextObjectVisualType.LINE_WISE
        is MotionActionHandler -> motion.motionType == MotionType.LINE_WISE
        is ExternalActionHandler -> motion.isLinewiseMotion
        else -> error("Command is not a motion: $motion")
      }
    }

    /**
     * The effective [MotionType] used to decide inclusive/exclusive behaviour, honouring a forced motion modifier
     * (`:help o_v`).
     *
     * `V` forces linewise. `v` forces characterwise and *toggles* the motion's inclusiveness: an inclusive motion
     * becomes exclusive and vice versa, while a linewise motion becomes exclusive characterwise (matching Vim's
     * "makes dvj work nice" behaviour). `CTRL-V` (blockwise) keeps the motion's own inclusiveness - the block's right
     * column is handled by [com.maddyhome.idea.vim.group.visual.VimBlockSelection]. Without a forced modifier, the
     * motion's own declared type is used.
     *
     * Returns `null` for motions that have no inclusive/exclusive concept and aren't forced (e.g. plain text objects).
     */
    fun getEffectiveMotionType(): MotionType? {
      val declared = (motion as? MotionActionHandler)?.motionType
      if (notForcedMotion()) {
        return declared
      }

      if (forcedMotion == SelectionType.LINE_WISE) return MotionType.LINE_WISE

      return when (declared) {
        MotionType.INCLUSIVE -> MotionType.EXCLUSIVE
        MotionType.LINE_WISE -> MotionType.EXCLUSIVE
        else -> MotionType.INCLUSIVE // EXCLUSIVE, or a text object with no declared type
      }
    }

    private fun notForcedMotion(): Boolean {
      return forcedMotion == null || forcedMotion == SelectionType.BLOCK_WISE
    }

    fun withArgument(argument: Argument) = Motion(motion, argument, forcedMotion)
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

    /**
     * A character argument, which is the name of a register
     *
     * The character will be mapped by `'langmap'`, from the user's input language to an ASCII value.
     */
    REGISTER,

    /**
     * A character argument, which is the name of a mark
     *
     * The character will be mapped by `'langmap'`, from the user's input language to an ASCII value.
     */
    MARK,

    /**
     * A character argument, such as the character to move to with the `f` command.
     *
     * The character is processed in the user's input language. As such, it can also be a digraph or literal value,
     * which means the handler will allow `<C-K>`, `<C-V>` and `<C-Q>` to start the digraph/literal state machine.
     * The final character will not be mapped by `'langmap'`.
     */
    CHARACTER;

    companion object {
      /**
       * Backwards-compatible alias for [CHARACTER].
       *
       * Returns the [CHARACTER] constant so that Java code comparing with `== Argument.Type.DIGRAPH` continues to work
       * after the `DIGRAPH` enum value was merged into `CHARACTER`.
       *
       * Note: this does NOT work as a `case` label in a Java `switch` statement, which requires a true enum constant.
       */
      @JvmField
      @Deprecated("Use CHARACTER instead", ReplaceWith("CHARACTER"))
      val DIGRAPH: Type = CHARACTER
    }
  }
}
