/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import java.util.*

/**
 * This represents a single Vim command to be executed (action, motion, operator+motion, v_textobject, etc.)
 *
 * A command is an action, with a type that determines how it is handled, such as [Type.MOTION], [Type.CHANGE],
 * [Type.OTHER_SELF_SYNCHRONIZED], etc. It also exposes the action's [CommandFlags] which are also used to help execute
 * the action.
 *
 * A command's action can require an argument, which can be either a character (e.g., `fx`) or the input from the Ex
 * command line. It can also be a motion, in which case the command is an operator+motion, such as `dw`. The motion
 * argument is an action that might also have an argument, such as `dfx` or `d/foo`.
 *
 * A command can optionally include a count and a register. More than one count can be entered, before an operator and
 * then before the motion argument, e.g. `2d3w`. This is intuitively "delete the next three words, twice", which is the
 * same as "delete the next six words". While both the operator and motion have a count while being built, the final
 * command has a single count that is the product of all count components. In this example, the command would have a
 * final count of `6`.
 *
 * Note that for a command that is an operator+motion command, the count applies to the motion, rather than the
 * operator. For example, `3i` will insert the following typed text three times, while `3cw` will change the next three
 * words with the following typed text, rather than changing the next word with the typed text three times. The command
 * still has a single count, and to handle this, the operator action should ignore the count, while the motion action
 * should use it when calculating the movement.
 *
 * As an additional interesting pathological edge case, it's possible to enter a count when selecting a register, and
 * it's possible to select multiple registers while building a command; the last register wins. This means that
 * `2"a3"b4"c5d6w` will delete 720 words and store the text in register `c`.
 *
 * @see OperatorArguments
 */
data class Command(
  val register: Char?,
  val rawCount: Int,
  val action: EditorActionHandlerBase,
  val argument: Argument?,
  val type: Type,
  val flags: EnumSet<CommandFlags>,
) {

  init {
    action.process(this)
  }

  val count: Int
    get() = rawCount.coerceAtLeast(1)

  override fun toString() = "Action = ${action.id}"

  enum class Type {
    /**
     * Represents commands that actually move the cursor and can be arguments to operators.
     */
    MOTION,

    /**
     * Represents commands that insert new text into the editor.
     */
    INSERT,

    /**
     * Represents commands that remove text from the editor.
     */
    DELETE,

    /**
     * Represents commands that change text in the editor.
     */
    CHANGE,

    /**
     * Represents commands that copy text in the editor.
     */
    COPY,
    PASTE,

    OTHER_READONLY,
    OTHER_WRITABLE,

    /**
     * Represent commands that don't require an outer read or write action for synchronization.
     */
    OTHER_SELF_SYNCHRONIZED,

    MODE_CHANGE,
    ;

    /**
     * Deprecated because not only this set of commands can be writable.
     * A different way of detecting if a command is going to write something is needed.
     */
    @Deprecated("")
    val isWrite: Boolean
      get() = when (this) {
        INSERT, DELETE, CHANGE, PASTE, OTHER_WRITABLE -> true
        else -> false
      }
  }
}
