/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
package com.maddyhome.idea.vim.ex

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointChangeListener
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.ThrowableComputable
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.handler.GotoLineHandler
import com.maddyhome.idea.vim.ex.ranges.Range.Companion.createRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.group.HistoryGroup
import com.maddyhome.idea.vim.group.RegisterGroup
import com.maddyhome.idea.vim.helper.MessageHelper.message
import com.maddyhome.idea.vim.helper.Msg
import java.util.regex.Pattern

/**
 * Maintains a tree of Ex commands based on the required and optional parts of the command names. Parses and
 * executes Ex commands entered by the user.
 */
object CommandParser {
  private const val MAX_RECURSION = 100
  private val TRIM_WHITESPACE = Pattern.compile("[ \\t]*(.*)[ \\t\\n\\r]+", Pattern.DOTALL)
  val EX_COMMAND_EP = ExtensionPointName.create<ExBeanClass>("IdeaVIM.vimExCommand")
  private val logger = logger<CommandParser>()

  private val root = CommandNode()

  fun unregisterHandlers() {
    root.clear()
  }

  /**
   * Registers all the supported Ex commands
   */
  fun registerHandlers() {
    EX_COMMAND_EP.extensions().forEach(ExBeanClass::register)
    registerEpListener()
  }

  private fun registerEpListener() {
    // IdeaVim doesn't support contribution to ex_command_ep extension point, so technically we can skip this update,
    //   but let's support dynamic plugins in a more classic way and reload handlers on every EP change.
    EX_COMMAND_EP.getPoint(null).addExtensionPointListener(ExtensionPointChangeListener {
      unregisterHandlers()
      registerHandlers()
    }, false, VimPlugin.getInstance())
  }

  /**
   * Used to rerun the last Ex command, if any
   *
   * @param editor  The editor to run the command in
   * @param context The data context
   * @param count   The number of times to run the command
   * @return True if the command succeeded, false if it failed or there was no previous command
   * @throws ExException if any part of the command was invalid
   */
  @kotlin.jvm.Throws(ExException::class)
  fun processLastCommand(editor: Editor, context: DataContext, count: Int): Boolean {
    val reg = VimPlugin.getRegister().getRegister(':') ?: return false
    val text = reg.text ?: return false
    processCommand(editor, context, text, count)
    return true
  }

  /**
   * Parse and execute an Ex command entered by the user
   *
   * @param editor  The editor to run the command in
   * @param context The data context
   * @param cmd     The text entered by the user
   * @param count   The count entered before the colon
   * @throws ExException if any part of the command is invalid or unknown
   */
  @kotlin.jvm.Throws(ExException::class)
  fun processCommand(editor: Editor, context: DataContext, cmd: String, count: Int) {
    processCommand(editor, context, cmd, count, MAX_RECURSION)
  }

  @kotlin.jvm.Throws(ExException::class)
  private fun processCommand(
    editor: Editor, context: DataContext, cmd: String,
    count: Int, aliasCountdown: Int
  ) {
    // Nothing entered
    if (cmd.isEmpty()) {
      logger.warn("CMD is empty")
      return
    }

    // Only save the command to the history if it is at the top of the stack.
    // We don't want to save the aliases that will be executed, only the actual
    // user input.
    if (aliasCountdown == MAX_RECURSION) {
      // Save the command history
      VimPlugin.getHistory().addEntry(HistoryGroup.COMMAND, cmd)
    }

    // If there is a command alias for the entered text, then process the alias and return that
    // instead of the original command.
    if (VimPlugin.getCommand().isAlias(cmd)) {
      if (aliasCountdown > 0) {
        val commandAlias = VimPlugin.getCommand().getAliasCommand(cmd, count)
        if (commandAlias.isEmpty()) {
          logger.warn("Command alias is empty")
          return
        }
        processCommand(editor, context, commandAlias, count, aliasCountdown - 1)
      } else {
        VimPlugin.showMessage(message("recursion.detected.maximum.alias.depth.reached"))
        VimPlugin.indicateError()
        logger.warn("Recursion detected, maximum alias depth reached. ")
      }
      return
    }

    // Parse the command
    val command = parse(cmd)
    val handler = getCommandHandler(command)
    if (handler == null) {
      val message = message(Msg.NOT_EX_CMD, command.command)
      throw InvalidCommandException(message, cmd)
    }
    if (handler.argFlags.access === CommandHandler.Access.WRITABLE && !editor.document.isWritable) {
      VimPlugin.indicateError()
      logger.info("Trying to modify readonly document")
      return
    }

    // Run the command
    val runCommand = ThrowableComputable<Any?, ExException> {
      val ok = handler.process(editor, context, command, count)
      if (ok && !handler.argFlags.flags.contains(CommandHandler.Flag.DONT_SAVE_LAST)) {
        VimPlugin.getRegister().storeTextSpecial(RegisterGroup.LAST_COMMAND_REGISTER, cmd)
      }
      null
    }
    when (handler.argFlags.access) {
      CommandHandler.Access.WRITABLE -> ApplicationManager.getApplication().runWriteAction(runCommand)
      CommandHandler.Access.READ_ONLY -> ApplicationManager.getApplication().runReadAction(runCommand)
      CommandHandler.Access.SELF_SYNCHRONIZED -> runCommand.compute()
    }
  }

  fun getCommandHandler(command: ExCommand): CommandHandler? {
    val cmd = command.command
    // If there is no command, just a range, use the 'goto line' handler
    if (cmd.isEmpty()) {
      return GotoLineHandler()
    }
    // See if the user entered a supported command by checking each character entered
    var node: CommandNode = root
    for (element in cmd) {
      node = node.getChild(element) ?: return null
    }
    val handlerHolder = node.commandHandler
    return handlerHolder?.instance
  }

  /**
   * Parse the text entered by the user. This does not include the leading colon.
   *
   * @param cmd The user entered text
   * @return The parse result
   * @throws ExException if the text is syntactically incorrect
   */
  @kotlin.jvm.Throws(ExException::class)
  fun parse(cmd: String): ExCommand {
    // This is a complicated state machine that should probably be rewritten
    logger.debug { "processing `$cmd'" }

    var state = State.START
    val ranges = Ranges() // The list of ranges
    val command = StringBuilder() // The command
    val argument = StringBuilder() // The command's argument(s)
    var location: StringBuffer? = null // The current range text
    var offsetSign = 1 // Sign of current range offset
    var offsetNumber = 0 // The value of the current range offset
    var offsetTotal = 0 // The sum of all the current range offsets
    var move = false // , vs. ; separated ranges (true=; false=,)
    var patternType = 0.toChar() // ? or /
    var backCount = 0 // Number of backslashes in a row in a pattern
    var inBrackets = false // If inside [ ] range in a pattern
    var error = ""

    // Loop through each character. Treat the end of the string as a newline character
    for (i in 0..cmd.length) {
      var reprocess = true // Should the current character be reprocessed after a state change?
      val ch = if (i == cmd.length) '\n' else cmd[i]
      loop@ while (reprocess) {
        when (state) {
          State.START -> if (Character.isLetter(ch) || "~<>@=#*&!".indexOf(ch) >= 0) {
            state = State.COMMAND
          } else if (ch == ' ') {
            state = State.START
            reprocess = false
          } else {
            state = State.RANGE
          }
          State.COMMAND ->             // For commands that start with a non-letter, treat other non-letter characters as part of
            // the argument except for !, <, or >
            if (Character.isLetter(ch) ||
              command.isEmpty() && "~<>@=#*&!".indexOf(ch) >= 0 ||
              command.isNotEmpty() && ch == command[command.length - 1] && "<>".indexOf(ch) >= 0
            ) {
              command.append(ch)
              reprocess = false
              if (!Character.isLetter(ch) && "<>".indexOf(ch) < 0) {
                state = State.CMD_ARG
              }
            } else {
              state = State.CMD_ARG
            }
          State.CMD_ARG -> {
            argument.append(ch)
            reprocess = false
          }
          State.RANGE -> {
            location = StringBuffer()
            offsetTotal = 0
            offsetNumber = 0
            move = false
            if (ch in '0'..'9') {
              state = State.RANGE_LINE
            } else if (ch == '.') {
              state = State.RANGE_CURRENT
            } else if (ch == '$') {
              state = State.RANGE_LAST
            } else if (ch == '%') {
              state = State.RANGE_ALL
            } else if (ch == '\'') {
              state = State.RANGE_MARK
            } else if (ch == '+' || ch == '-') {
              location.append('.')
              state = State.RANGE_OFFSET
            } else if (ch == '\\') {
              location.append(ch)
              state = State.RANGE_SHORT_PATTERN
              reprocess = false
            } else if (ch == ',') {
              location.append('.')
              state = State.RANGE_MAYBE_DONE
            } else if (ch == '/' || ch == '?') {
              location.append(ch)
              patternType = ch
              backCount = 0
              inBrackets = false
              state = State.RANGE_PATTERN
              reprocess = false
            } else {
              error = message(Msg.e_badrange, Character.toString(ch))
              state = State.ERROR
              reprocess = false
            }
          }
          State.RANGE_SHORT_PATTERN -> {
            if (ch == '/' || ch == '?' || ch == '&') {
              location!!.append(ch)
              state = State.RANGE_PATTERN_MAYBE_DONE
            } else {
              error = message(Msg.e_backslash)
              state = State.ERROR
            }
            reprocess = false
          }
          State.RANGE_PATTERN ->             // No trailing / or ? required if there is no command so look for newline to tell us we are done
            if (ch == '\n') {
              location!!.append(patternType)
              state = State.RANGE_MAYBE_DONE
            } else {
              // We need to skip over [ ] ranges. The ] is valid right after the [ or [^
              location!!.append(ch)
              if (ch == '[' && !inBrackets) {
                inBrackets = true
              } else if (ch == ']' && inBrackets && !(location[location.length - 2] == '[' ||
                  location.length >= 3 && location.substring(location.length - 3) == "[^]")
              ) {
                inBrackets = false
              } else if (ch == '\\') {
                backCount++
              } else if (ch == patternType && !inBrackets &&
                (location[location.length - 2] != '\\' || backCount % 2 == 0)
              ) {
                state = State.RANGE_PATTERN_MAYBE_DONE
              }

              // No more backslashes
              if (ch != '\\') {
                backCount = 0
              }
              reprocess = false
            }
          State.RANGE_PATTERN_MAYBE_DONE -> if (ch == '/' || ch == '?') {
            // Use a special character to separate pattern for later, easier, parsing
            location!!.append('\u0000')
            location.append(ch)
            patternType = ch
            backCount = 0
            inBrackets = false
            state = State.RANGE_PATTERN
            reprocess = false
          } else {
            state = State.RANGE_MAYBE_DONE
          }
          State.RANGE_LINE -> if (ch in '0'..'9') {
            location!!.append(ch)
            state = State.RANGE_MAYBE_DONE
            reprocess = false
          } else {
            state = State.RANGE_MAYBE_DONE
          }
          State.RANGE_CURRENT -> {
            location!!.append(ch)
            state = State.RANGE_MAYBE_DONE
            reprocess = false
          }
          State.RANGE_LAST -> {
            location!!.append(ch)
            state = State.RANGE_MAYBE_DONE
            reprocess = false
          }
          State.RANGE_ALL -> {
            location!!.append(ch)
            state = State.RANGE_MAYBE_DONE
            reprocess = false
          }
          State.RANGE_MARK -> {
            location!!.append(ch)
            state = State.RANGE_MARK_CHAR
            reprocess = false
          }
          State.RANGE_MARK_CHAR -> {
            location!!.append(ch)
            state = State.RANGE_MAYBE_DONE
            reprocess = false
          }
          State.RANGE_DONE -> {
            val range = createRange(location.toString(), offsetTotal, move)
            if (range == null) {
              error = message(Msg.e_badrange, Character.toString(ch))
              state = State.ERROR
              reprocess = false
              break@loop
            }
            ranges.addRange(range)
            // Could there be more ranges - nope - at end, start command
            if (ch == ':' || ch == '\n') {
              state = State.COMMAND
              reprocess = false
            } else if (Character.isLetter(ch) || "~<>@=#*&!".indexOf(ch) >= 0 || ch == ' ') {
              state = State.START
            } else {
              state = State.RANGE
            }
          }
          State.RANGE_MAYBE_DONE ->             // The range has an offset after it
            state = if (ch == '+' || ch == '-') {
              State.RANGE_OFFSET
            } else if (ch == ',' || ch == ';') {
              State.RANGE_SEPARATOR
            } else if (ch in '0'..'9') {
              State.RANGE_LINE
            } else {
              State.RANGE_DONE
            }
          State.RANGE_OFFSET -> {
            // Figure out the sign of the offset and reset the offset value
            offsetNumber = 0
            if (ch == '+') {
              offsetSign = 1
            } else if (ch == '-') {
              offsetSign = -1
            }
            state = State.RANGE_OFFSET_MAYBE_DONE
            reprocess = false
          }
          State.RANGE_OFFSET_MAYBE_DONE ->             // We found an offset value
            state = if (ch in '0'..'9') {
              State.RANGE_OFFSET_NUM
            } else {
              State.RANGE_OFFSET_DONE
            }
          State.RANGE_OFFSET_DONE -> {
            // No number implies a one
            if (offsetNumber == 0) {
              offsetNumber = 1
            }
            // Update offset total for this range
            offsetTotal += offsetNumber * offsetSign

            // Another offset
            state = if (ch == '+' || ch == '-') {
              State.RANGE_OFFSET
            } else {
              State.RANGE_MAYBE_DONE
            }
          }
          State.RANGE_OFFSET_NUM ->             // Update the value of the current offset
            if (ch in '0'..'9') {
              offsetNumber = offsetNumber * 10 + (ch - '0')
              state = State.RANGE_OFFSET_MAYBE_DONE
              reprocess = false
            } else if (ch == '+' || ch == '-') {
              state = State.RANGE_OFFSET_DONE
            } else {
              state = State.RANGE_OFFSET_MAYBE_DONE
            }
          State.RANGE_SEPARATOR -> {
            if (ch == ',') {
              move = false
            } else if (ch == ';') {
              move = true
            }
            state = State.RANGE_DONE
            reprocess = false
          }
          State.ERROR -> {
          }
        }
      }

      // Oops - bad command string
      if (state == State.ERROR) {
        throw InvalidCommandException(error, cmd)
      }
    }
    if (logger.isDebugEnabled) {
      logger.debug("ranges = $ranges")
      logger.debug("command = $command")
      logger.debug("argument = $argument")
    }
    var argumentString = argument.toString()
    val matcher = TRIM_WHITESPACE.matcher(argumentString)
    if (matcher.matches()) {
      argumentString = matcher.group(1)
    }
    return ExCommand(ranges, command.toString(), argumentString)
  }

  /** Adds a command handler to the parser  */
  fun addHandler(handlerHolder: ExBeanClass) {
    // Iterator through each command name alias
    val names: Array<CommandName> = when {
        handlerHolder.names != null -> {
          commands(*handlerHolder.names!!.split(",").toTypedArray())
        }
        handlerHolder.instance is ComplicatedNameExCommand -> {
          (handlerHolder.instance as ComplicatedNameExCommand).names
        }
        else -> throw RuntimeException("Cannot create an ex command: $handlerHolder")
    }
    for (name in names) {
      var node = root
      var text = name.required
      // Build a tree for each character in the required portion of the command name
      for (i in 0 until text.length - 1) {
        var cn = node.getChild(text[i])
        if (cn == null) {
          cn = node.addChild(text[i], null)
        }
        node = cn
      }

      // For the last character we need to add the actual handler
      var cn = node.getChild(text[text.length - 1])
      if (cn == null) {
        cn = node.addChild(text[text.length - 1], handlerHolder)
      } else {
        cn.commandHandler = handlerHolder
      }
      node = cn

      // Now add the handler for each character in the optional portion of the command name
      text = name.optional
      for (i in text.indices) {
        cn = node.getChild(text[i])
        if (cn == null) {
          cn = node.addChild(text[i], handlerHolder)
        } else if (cn.commandHandler == null) {
          cn.commandHandler = handlerHolder
        }
        node = cn
      }
    }
  }

  private enum class State {
    START, COMMAND,
    CMD_ARG,
    RANGE, RANGE_LINE, RANGE_CURRENT, RANGE_LAST, RANGE_MARK, RANGE_MARK_CHAR, RANGE_ALL, RANGE_PATTERN,
    RANGE_SHORT_PATTERN, RANGE_PATTERN_MAYBE_DONE, RANGE_OFFSET, RANGE_OFFSET_NUM, RANGE_OFFSET_DONE,
    RANGE_OFFSET_MAYBE_DONE, RANGE_SEPARATOR, RANGE_MAYBE_DONE, RANGE_DONE, ERROR
  }
}
