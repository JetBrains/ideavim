/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
package com.maddyhome.idea.vim.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.ThrowableComputable;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.handler.GotoLineHandler;
import com.maddyhome.idea.vim.ex.range.AbstractRange;
import com.maddyhome.idea.vim.group.HistoryGroup;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maintains a tree of Ex commands based on the required and optional parts of the command names. Parses and
 * executes Ex commands entered by the user.
 */
public class CommandParser {
  private static final int MAX_RECURSION = 100;
  private static final Pattern TRIM_WHITESPACE = Pattern.compile("[ \\t]*(.*)[ \\t\\n\\r]+", Pattern.DOTALL);
  private final ExtensionPointName<ExBeanClass> EX_COMMAND_EP = ExtensionPointName.create("IdeaVIM.vimExCommand");

  private static class CommandParserHolder {
    static final CommandParser INSTANCE = new CommandParser();
  }

  /**
   * There is only one parser.
   *
   * @return The singleton instance
   */
  public synchronized static CommandParser getInstance() {
    return CommandParserHolder.INSTANCE;
  }

  /**
   * Don't let anyone create one of these.
   */
  private CommandParser() {
  }

  /**
   * Registers all the supported Ex commands
   */
  public void registerHandlers() {
    if (registered) return;
    registered = true;

    for (ExBeanClass handler : EX_COMMAND_EP.getExtensions()) {
      handler.register();
    }
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
  public boolean processLastCommand(@NotNull Editor editor, @NotNull DataContext context, int count) throws ExException {
    final Register reg = VimPlugin.getRegister().getRegister(':');
    if (reg != null) {
      final String text = reg.getText();
      if (text != null) {
        processCommand(editor, context, text, count);
        return true;
      }
    }
    return false;
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
  public void processCommand(@NotNull Editor editor, @NotNull DataContext context, @NotNull String cmd,
                            int count) throws ExException {
    processCommand(editor, context, cmd, count, MAX_RECURSION);
  }

  private void processCommand(@NotNull Editor editor, @NotNull DataContext context, @NotNull String cmd,
                             int count, int aliasCountdown) throws ExException {
    // Nothing entered
    int result = 0;
    if (cmd.length() == 0) {
      logger.warn("CMD is empty");
      return;
    }

    // Only save the command to the history if it is at the top of the stack.
    // We don't want to save the aliases that will be executed, only the actual
    // user input.
    if (aliasCountdown == MAX_RECURSION) {
      // Save the command history
      VimPlugin.getHistory().addEntry(HistoryGroup.COMMAND, cmd);
    }

    // If there is a command alias for the entered text, then process the alias and return that
    // instead of the original command.
    if (VimPlugin.getCommand().isAlias(cmd)) {
      if (aliasCountdown > 0) {
        String commandAlias = VimPlugin.getCommand().getAliasCommand(cmd, count);
        if (commandAlias.isEmpty()) {
          logger.warn("Command alias is empty");
          return;
        }
        processCommand(editor, context, commandAlias, count, aliasCountdown - 1);
      } else {
        VimPlugin.showMessage("Recursion detected, maximum alias depth reached.");
        VimPlugin.indicateError();
        logger.warn("Recursion detected, maximum alias depth reached. ");
      }
      return;
    }

    // Parse the command
    final ExCommand command = parse(cmd);
    final CommandHandler handler = getCommandHandler(command);

    if (handler == null) {
      final String message = MessageHelper.message(Msg.NOT_EX_CMD, command.getCommand());
      throw new InvalidCommandException(message, cmd);
    }

    if (handler.getArgFlags().getAccess() == CommandHandler.Access.WRITABLE && !editor.getDocument().isWritable()) {
      VimPlugin.indicateError();
      logger.info("Trying to modify readonly document");
      return;
    }

    // Run the command

    ThrowableComputable<Object, ExException> runCommand = () -> {
      boolean ok = handler.process(editor, context, command, count);
      if (ok && !handler.getArgFlags().getFlags().contains(CommandHandler.Flag.DONT_SAVE_LAST)) {
        VimPlugin.getRegister().storeTextInternal(editor, new TextRange(-1, -1), cmd,
                                                  SelectionType.CHARACTER_WISE, ':', false);
      }
      return null;
    };

    switch (handler.getArgFlags().getAccess()) {
      case WRITABLE:
        ApplicationManager.getApplication().runWriteAction(runCommand);
        break;
      case READ_ONLY:
        ApplicationManager.getApplication().runReadAction(runCommand);
        break;
      case SELF_SYNCHRONIZED:
        runCommand.compute();
    }
  }

  @Nullable
  public CommandHandler getCommandHandler(@NotNull ExCommand command) {
    final String cmd = command.getCommand();
    // If there is no command, just a range, use the 'goto line' handler
    if (cmd.length() == 0) {
      return new GotoLineHandler();
    }
    // See if the user entered a supported command by checking each character entered
    CommandNode node = root;
    for (int i = 0; i < cmd.length(); i++) {
      node = node.getChild(cmd.charAt(i));
      if (node == null) {
        return null;
      }
    }
    final ExBeanClass handlerHolder = node.getCommandHandler();
    return handlerHolder != null ? handlerHolder.getHandler() : null;
  }

  /**
   * Parse the text entered by the user. This does not include the leading colon.
   *
   * @param cmd The user entered text
   * @return The parse result
   * @throws ExException if the text is syntactically incorrect
   */
  @NotNull
  public ExCommand parse(@NotNull String cmd) throws ExException {
    // This is a complicated state machine that should probably be rewritten
    if (logger.isDebugEnabled()) {
      logger.debug("processing `" + cmd + "'");
    }
    State state = State.START;
    Ranges ranges = new Ranges(); // The list of ranges
    StringBuilder command = new StringBuilder(); // The command
    StringBuilder argument = new StringBuilder(); // The command's argument(s)
    StringBuffer location = null; // The current range text
    int offsetSign = 1; // Sign of current range offset
    int offsetNumber = 0; // The value of the current range offset
    int offsetTotal = 0; // The sum of all the current range offsets
    boolean move = false; // , vs. ; separated ranges (true=; false=,)
    char patternType = 0; // ? or /
    int backCount = 0; // Number of backslashes in a row in a pattern
    boolean inBrackets = false; // If inside [ ] range in a pattern
    String error = "";

    // Loop through each character. Treat the end of the string as a newline character
    for (int i = 0; i <= cmd.length(); i++) {
      boolean reprocess = true; // Should the current character be reprocessed after a state change?
      char ch = (i == cmd.length() ? '\n' : cmd.charAt(i));
      while (reprocess) {
        switch (state) {
          case START: // Very start of the entered text
            if (Character.isLetter(ch) || "~<>@=#*&!".indexOf(ch) >= 0) {
              state = State.COMMAND;
            }
            else if (ch == ' ') {
              state = State.START;
              reprocess = false;
            }
            else {
              state = State.RANGE;
            }
            break;
          case COMMAND: // Reading the actual command name
            // For commands that start with a non-letter, treat other non-letter characters as part of
            // the argument except for !, <, or >
            if (Character.isLetter(ch) ||
                (command.length() == 0 && "~<>@=#*&!".indexOf(ch) >= 0) ||
                (command.length() > 0 && ch == command.charAt(command.length() - 1) &&
                 "<>".indexOf(ch) >= 0)) {
              command.append(ch);
              reprocess = false;
              if (!Character.isLetter(ch) && "<>".indexOf(ch) < 0) {
                state = State.CMD_ARG;
              }
            }
            else {
              state = State.CMD_ARG;
            }
            break;
          case CMD_ARG: // Reading the command's argument
            argument.append(ch);
            reprocess = false;
            break;
          case RANGE: // Starting a new range
            location = new StringBuffer();
            offsetTotal = 0;
            offsetNumber = 0;
            move = false;
            if (ch >= '0' && ch <= '9') {
              state = State.RANGE_LINE;
            }
            else if (ch == '.') {
              state = State.RANGE_CURRENT;
            }
            else if (ch == '$') {
              state = State.RANGE_LAST;
            }
            else if (ch == '%') {
              state = State.RANGE_ALL;
            }
            else if (ch == '\'') {
              state = State.RANGE_MARK;
            }
            else if (ch == '+' || ch == '-') {
              location.append('.');
              state = State.RANGE_OFFSET;
            }
            else if (ch == '\\') {
              location.append(ch);
              state = State.RANGE_SHORT_PATTERN;
              reprocess = false;
            }
            else if (ch == '/' || ch == '?') {
              location.append(ch);
              patternType = ch;
              backCount = 0;
              inBrackets = false;
              state = State.RANGE_PATTERN;
              reprocess = false;
            }
            else {
              error = MessageHelper.message(Msg.e_badrange, Character.toString(ch));
              state = State.ERROR;
              reprocess = false;
            }
            break;
          case RANGE_SHORT_PATTERN: // Handle \/, \?, and \& patterns
            if (ch == '/' || ch == '?' || ch == '&') {
              location.append(ch);
              state = State.RANGE_PATTERN_MAYBE_DONE;
              reprocess = false;
            }
            else {
              error = MessageHelper.message(Msg.e_backslash);
              state = State.ERROR;
              reprocess = false;
            }
            break;
          case RANGE_PATTERN: // Reading a pattern range
            // No trailing / or ? required if there is no command so look for newline to tell us we are done
            if (ch == '\n') {
              location.append(patternType);
              state = State.RANGE_MAYBE_DONE;
            }
            else {
              // We need to skip over [ ] ranges. The ] is valid right after the [ or [^
              location.append(ch);
              if (ch == '[' && !inBrackets) {
                inBrackets = true;
              }
              else if (ch == ']' && inBrackets && !(location.charAt(location.length() - 2) == '[' ||
                                                    (location.length() >= 3 && location.substring(location.length() - 3).equals("[^]")))) {
                inBrackets = false;
              }
              // Keep count of the backslashes
              else if (ch == '\\') {
                backCount++;
              }
              // Does this mark the end of the current pattern? True if we found the matching / or ?
              // and it is not preceded by an even number of backslashes
              else if (ch == patternType && !inBrackets &&
                       (location.charAt(location.length() - 2) != '\\' || backCount % 2 == 0)) {
                state = State.RANGE_PATTERN_MAYBE_DONE;
              }

              // No more backslashes
              if (ch != '\\') {
                backCount = 0;
              }

              reprocess = false;
            }
            break;
          case RANGE_PATTERN_MAYBE_DONE: // Check to see if there is another immediate pattern
            if (ch == '/' || ch == '?') {
              // Use a special character to separate pattern for later, easier, parsing
              location.append('\u0000');
              location.append(ch);
              patternType = ch;
              backCount = 0;
              inBrackets = false;
              state = State.RANGE_PATTERN;
              reprocess = false;
            }
            else {
              state = State.RANGE_MAYBE_DONE;
            }
            break;
          case RANGE_LINE: // Explicit line number
            if (ch >= '0' && ch <= '9') {
              location.append(ch);
              state = State.RANGE_MAYBE_DONE;
              reprocess = false;
            }
            else {
              state = State.RANGE_MAYBE_DONE;
            }
            break;
          case RANGE_CURRENT: // Current line - .
            location.append(ch);
            state = State.RANGE_MAYBE_DONE;
            reprocess = false;
            break;
          case RANGE_LAST: // Last line - $
            location.append(ch);
            state = State.RANGE_MAYBE_DONE;
            reprocess = false;
            break;
          case RANGE_ALL: // All lines - %
            location.append(ch);
            state = State.RANGE_MAYBE_DONE;
            reprocess = false;
            break;
          case RANGE_MARK: // Mark line - 'x
            location.append(ch);
            state = State.RANGE_MARK_CHAR;
            reprocess = false;
            break;
          case RANGE_MARK_CHAR: // Actual mark
            location.append(ch);
            state = State.RANGE_MAYBE_DONE;
            reprocess = false;
            break;
          case RANGE_DONE: // We have hit the end of a range - process it
            Range[] range = AbstractRange.createRange(location.toString(), offsetTotal, move);
            if (range == null) {
              error = MessageHelper.message(Msg.e_badrange, Character.toString(ch));
              state = State.ERROR;
              reprocess = false;
              break;
            }
            ranges.addRange(range);
            // Could there be more ranges - nope - at end, start command
            if (ch == ':' || ch == '\n') {
              state = State.COMMAND;
              reprocess = false;
            }
            // Start of command
            else if (Character.isLetter(ch) || "~<>@=#*&!".indexOf(ch) >= 0 || ch == ' ') {
              state = State.START;
            }
            // We have another range
            else {
              state = State.RANGE;
            }
            break;
          case RANGE_MAYBE_DONE: // Are we done with the current range?
            // The range has an offset after it
            if (ch == '+' || ch == '-') {
              state = State.RANGE_OFFSET;
            }
            // End of the range - we found a separator
            else if (ch == ',' || ch == ';') {
              state = State.RANGE_SEPARATOR;
            }
            // Part of a line number
            else if (ch >= '0' && ch <= '9') {
              state = State.RANGE_LINE;
            }
            // No more range
            else {
              state = State.RANGE_DONE;
            }
            break;
          case RANGE_OFFSET: // Offset after a range
            // Figure out the sign of the offset and reset the offset value
            offsetNumber = 0;
            if (ch == '+') {
              offsetSign = 1;
            }
            else if (ch == '-') {
              offsetSign = -1;
            }
            state = State.RANGE_OFFSET_MAYBE_DONE;
            reprocess = false;
            break;
          case RANGE_OFFSET_MAYBE_DONE: // Are we done with the offset?
            // We found an offset value
            if (ch >= '0' && ch <= '9') {
              state = State.RANGE_OFFSET_NUM;
            }
            // Yes, offset done
            else {
              state = State.RANGE_OFFSET_DONE;
            }
            break;
          case RANGE_OFFSET_DONE: // At the end of a range offset
            // No number implies a one
            if (offsetNumber == 0) {
              offsetNumber = 1;
            }
            // Update offset total for this range
            offsetTotal += offsetNumber * offsetSign;

            // Another offset
            if (ch == '+' || ch == '-') {
              state = State.RANGE_OFFSET;
            }
            // No more offsets for this range
            else {
              state = State.RANGE_MAYBE_DONE;
            }
            break;
          case RANGE_OFFSET_NUM: // An offset number
            // Update the value of the current offset
            if (ch >= '0' && ch <= '9') {
              offsetNumber = offsetNumber * 10 + (ch - '0');
              state = State.RANGE_OFFSET_MAYBE_DONE;
              reprocess = false;
            }
            // Found the start of a new offset
            else if (ch == '+' || ch == '-') {
              state = State.RANGE_OFFSET_DONE;
            }
            else {
              state = State.RANGE_OFFSET_MAYBE_DONE;
            }
            break;
          case RANGE_SEPARATOR: // Found a range separator
            if (ch == ',') {
              move = false;
            }
            else if (ch == ';') {
              move = true;
            }
            state = State.RANGE_DONE;
            reprocess = false;
            break;
          case ERROR:
            break;
        }
      }

      // Oops - bad command string
      if (state == State.ERROR) {
        throw new InvalidCommandException(error, cmd);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("ranges = " + ranges);
      logger.debug("command = " + command);
      logger.debug("argument = " + argument);
    }

    String argumentString = argument.toString();
    final Matcher matcher = TRIM_WHITESPACE.matcher(argumentString);
    if (matcher.matches()) {
      argumentString = matcher.group(1);
    }
    return new ExCommand(ranges, command.toString(), argumentString);
  }

  /** Adds a command handler to the parser */
  public void addHandler(@NotNull ExBeanClass handlerHolder) {
    // Iterator through each command name alias
    CommandName[] names;
    if (handlerHolder.getNames() != null) {
      names = CommandDefinitionKt.commands(handlerHolder.getNames().split(","));
    }
    else if (handlerHolder.getHandler() instanceof ComplicatedNameExCommand) {
      names = ((ComplicatedNameExCommand)handlerHolder.getHandler()).getNames();
    }
    else {
      throw new RuntimeException("Cannot create an ex command: " + handlerHolder);
    }
    for (CommandName name : names) {
      CommandNode node = root;
      String text = name.getRequired();
      // Build a tree for each character in the required portion of the command name
      for (int i = 0; i < text.length() - 1; i++) {
        CommandNode cn = node.getChild(text.charAt(i));
        if (cn == null) {
          cn = node.addChild(text.charAt(i), null);
        }

        node = cn;
      }

      // For the last character we need to add the actual handler
      CommandNode cn = node.getChild(text.charAt(text.length() - 1));
      if (cn == null) {
        cn = node.addChild(text.charAt(text.length() - 1), handlerHolder);
      }
      else {
        cn.setCommandHandler(handlerHolder);
      }
      node = cn;

      // Now add the handler for each character in the optional portion of the command name
      text = name.getOptional();
      for (int i = 0; i < text.length(); i++) {
        cn = node.getChild(text.charAt(i));
        if (cn == null) {
          cn = node.addChild(text.charAt(i), handlerHolder);
        }
        else if (cn.getCommandHandler() == null) {
          cn.setCommandHandler(handlerHolder);
        }

        node = cn;
      }
    }
  }

  @NotNull private final CommandNode root = new CommandNode();
  private boolean registered = false;

  private enum State {
    START,
    COMMAND,
    CMD_ARG,
    RANGE,
    RANGE_LINE,
    RANGE_CURRENT,
    RANGE_LAST,
    RANGE_MARK,
    RANGE_MARK_CHAR,
    RANGE_ALL,
    RANGE_PATTERN,
    RANGE_SHORT_PATTERN,
    RANGE_PATTERN_MAYBE_DONE,
    RANGE_OFFSET,
    RANGE_OFFSET_NUM,
    RANGE_OFFSET_DONE,
    RANGE_OFFSET_MAYBE_DONE,
    RANGE_SEPARATOR,
    RANGE_MAYBE_DONE,
    RANGE_DONE,
    ERROR
  }

  private static final Logger logger = Logger.getInstance(CommandParser.class.getName());
}
