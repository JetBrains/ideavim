/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.state.VimStateMachine
import org.jetbrains.annotations.NonNls
import java.util.*
import javax.swing.KeyStroke

/**
 * All the commands in IdeaVim should implement one of the following handlers and be registered in VimActions.xml
 * Check the KtDocs of handlers for the details.
 *
 * Structure of handlers:
 *
 * - [EditorActionHandlerBase]: Base handler for all handlers. Please don't use it directly.
 *  - [VimActionHandler]: .............. Common vim commands.. E.g.: u, <C-W>s, <C-D>.
 *  - [TextObjectActionHandler]: ....... Text objects. ....... E.g.: iw, a(, i>
 *  - [MotionActionHandler]: ........... Motion commands. .... E.g.: k, w, <Up>
 *  - [ChangeEditorActionHandler]: ..... Change commands. .... E.g.: s, r, gU
 *  - [VisualOperatorActionHandler]: ... Visual commands.
 *  - [IdeActionHandler]: .............. Commands handled by existing IDE actions.
 *
 *  SpecialKeyHandlers are not presented here because these handlers are created to a limited set of commands and they
 *    are already implemented.
 */
public abstract class EditorActionHandlerBase(private val myRunForEachCaret: Boolean) {
  public val id: String = getActionId(this::class.java.name)

  /**
   * Note: I don't like this field because it controls RW lock. In case we process RW lock inside the command,
   *   we change this type to OTHER_SELF_SYNCHRONIZED. This may cause problems because some logic may depend on,
   *   let's say, INSERT type of the command.
   * At the moment of this comment writing I discovered a deadlock inside the
   *   [com.maddyhome.idea.vim.action.change.insert.InsertRegisterAction] and I have to change this from INSERT to
   *   OTHER_SELF_SYNCHRONIZED. This should cause no problems because I don't see any logic depending on INSERT,
   *   but there is some logic depending on other command types and such change may cause bugs.
   * It looks like it'll be needed to split command type and synchronization type.
   */
  public abstract val type: Command.Type

  public open val argumentType: Argument.Type? = null

  /**
   * Returns various binary flags for the command.
   *
   * These legacy flags will be refactored in future releases.
   *
   * @see com.maddyhome.idea.vim.command.Command
   */
  public open val flags: EnumSet<CommandFlags> = noneOfEnum()

  public abstract fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean

  /**
   * Post execute is executed only one time after the main execute method
   */
  public open fun postExecute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ) {
  }

  public fun execute(editor: VimEditor, context: ExecutionContext.Editor, operatorArguments: OperatorArguments) {
    val action = { caret: VimCaret -> doExecute(editor, caret, context, operatorArguments) }

    // IJ platform has one issue - recursive `runForEachCaret` is not allowed. Strictly speaking, at this moment
    //   we don't know if we run this action inside of this run or not.
    val currentCaret = editor.currentCaret()
    val primaryCaret = editor.primaryCaret()
    if (editor.isInForEachCaretScope()) {
      if (myRunForEachCaret) {
        action(currentCaret)
      } else {
        if (currentCaret == primaryCaret) {
          action(primaryCaret)
        }
      }
    } else {
      if (myRunForEachCaret) {
        editor.forEachCaret(action)
      } else {
        action(primaryCaret)
      }
    }

    if (currentCaret == primaryCaret) {
      val cmd = VimStateMachine.getInstance(editor).executingCommand ?: run {
        injector.messages.indicateError()
        return
      }
      postExecute(editor, context, cmd, operatorArguments)
    }
  }

  private fun doExecute(editor: VimEditor, caret: VimCaret, context: ExecutionContext.Editor, operatorArguments: OperatorArguments) {
    if (!injector.enabler.isEnabled()) return

    logger.debug("Execute command with handler: " + this.javaClass.name)

    val cmd = VimStateMachine.getInstance(editor).executingCommand ?: run {
      injector.messages.indicateError()
      return
    }

    if (!baseExecute(
        editor,
        caret,
        injector.executionContextManager.onCaret(caret, context),
        cmd,
        operatorArguments,
      )
    ) {
      injector.messages.indicateError()
    }
  }

  public open fun process(cmd: Command) {
    // No-op
  }

  public companion object {
    private val logger = vimLogger<EditorActionHandlerBase>()

    public fun parseKeysSet(keyStrings: List<String>): Set<List<KeyStroke>> = keyStrings.map { injector.parser.parseKeys(it) }.toSet()

    @JvmStatic
    public fun parseKeysSet(@NonNls vararg keyStrings: String): Set<List<KeyStroke>> = List(keyStrings.size) {
      injector.parser.parseKeys(keyStrings[it])
    }.toSet()

    @NonNls
    private const val VimActionPrefix = "Vim"

    @NonNls
    public fun getActionId(classFullName: String): String {
      return classFullName
        .takeLastWhile { it != '.' }
        .let { if (it.startsWith(VimActionPrefix, true)) it else "$VimActionPrefix$it" }
    }
  }
}
