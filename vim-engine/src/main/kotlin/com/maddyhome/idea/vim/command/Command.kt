/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.handler.ExternalActionHandler
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import java.util.*

/**
 * This represents a single Vim command to be executed (operator, motion, text object, etc.). It may optionally include
 * an argument if appropriate for the command. The command has a count and a type.
 */
data class Command(
  var rawCount: Int,
  var action: EditorActionHandlerBase,
  val type: Type,
  var flags: EnumSet<CommandFlags>,
) {

  constructor(rawCount: Int, register: Char) : this(
    rawCount,
    NonExecutableActionHandler,
    Type.SELECT_REGISTER,
    EnumSet.of(CommandFlags.FLAG_EXPECT_MORE),
  ) {
    this.register = register
  }

  init {
    action.process(this)
  }

  val count: Int
    get() = rawCount.coerceAtLeast(1)

  var argument: Argument? = null
  var register: Char? = null

  fun isLinewiseMotion(): Boolean {
    return action.let {
      when (it) {
        is TextObjectActionHandler -> it.visualType == TextObjectVisualType.LINE_WISE
        is MotionActionHandler -> it.motionType == MotionType.LINE_WISE
        is ExternalActionHandler -> it.isLinewiseMotion
        else -> error("Command is not a motion: $action")
      }
    }
  }

  override fun toString(): String {
    return "Action = ${action.id}"
  }

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

    /**
     * Represents commands that select the register.
     */
    SELECT_REGISTER,
    OTHER_READONLY,
    OTHER_WRITABLE,

    /**
     * Represent commands that don't require an outer read or write action for synchronization.
     */
    OTHER_SELF_SYNCHRONIZED,

    ;

    val isRead: Boolean
      get() = when (this) {
        MOTION, COPY, OTHER_READONLY -> true
        else -> false
      }

    val isWrite: Boolean
      get() = when (this) {
        INSERT, DELETE, CHANGE, PASTE, OTHER_WRITABLE -> true
        else -> false
      }
  }
}

private object NonExecutableActionHandler : EditorActionHandlerBase(false) {
  override val type: Command.Type
    get() = error("This action should not be executed")

  override fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    error("This action should not be executed")
  }
}
