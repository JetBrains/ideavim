/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
package com.maddyhome.idea.vim.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.handler.*;
import com.maddyhome.idea.vim.ex.range.AbstractRange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.HistoryGroup;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import org.jetbrains.annotations.NotNull;

/**
 * Maintains a tree of Ex commands based on the required and optional parts of the command names. Parses and
 * executes Ex commands entered by the user.
 */
public class CommandParser {
  public static final int RES_EMPTY = 1;
  public static final int RES_ERROR = 1;
  public static final int RES_READONLY = 1;
  public static final int RES_MORE_PANEL = 2;
  public static final int RES_DONT_REOPEN = 4;

  /**
   * There is only one parser.
   *
   * @return The singleton instance
   */
  public synchronized static CommandParser getInstance() {
    if (ourInstance == null) {
      ourInstance = new CommandParser();
    }
    return ourInstance;
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

    new AsciiHandler();
    new CmdFilterHandler();
    new CopyTextHandler();
    new DeleteLinesHandler();
    new DigraphHandler();
    new DumpLineHandler();
    new EditFileHandler();
    new ExitHandler();
    new FindClassHandler();
    new FindFileHandler();
    new FindSymbolHandler();
    new GotoCharacterHandler();
    //new GotoLineHandler(); - not needed here
    new HelpHandler();
    new HistoryHandler();
    new JoinLinesHandler();
    new JumpsHandler();
    new MarkHandler();
    new MarksHandler();
    new MoveTextHandler();
    new NextFileHandler();
    new NoHLSearchHandler();
    new OnlyHandler();
    new PreviousFileHandler();
    new PromptFindHandler();
    new PromptReplaceHandler();
    new PutLinesHandler();
    new QuitHandler();
    new RedoHandler();
    new RegistersHandler();
    new RepeatHandler();
    new SelectFileHandler();
    new SelectFirstFileHandler();
    new SelectLastFileHandler();
    new SetHandler();
    new ShiftLeftHandler();
    new ShiftRightHandler();
    new SubstituteHandler();
    new UndoHandler();
    new WriteAllHandler();
    new WriteHandler();
    new WriteNextFileHandler();
    new WritePreviousFileHandler();
    new WriteQuitHandler();
    new YankLinesHandler();

    registered = true;
    //logger.debug("root=" + root);
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
  public boolean processLastCommand(@NotNull Editor editor, DataContext context, int count) throws ExException {
    final Register reg = CommandGroups.getInstance().getRegister().getRegister(':');
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
   * @return A bitwise collection of flags, if any, from the result of running the command.
   * @throws ExException if any part of the command is invalid or unknown
   */
  public int processCommand(@NotNull Editor editor, DataContext context, @NotNull String cmd, int count) throws ExException {
    // Nothing entered
    int result = 0;
    if (cmd.length() == 0) {
      return result | RES_EMPTY;
    }

    // Save the command history
    CommandGroups.getInstance().getHistory().addEntry(HistoryGroup.COMMAND, cmd);

    // Parse the command
    ParseResult res = parse(cmd);
    String command = res.getCommand();

    // If there is no command, just a range, use the 'goto line' handler
    CommandHandler handler;
    if (command.length() == 0) {
      handler = new GotoLineHandler();
    }
    else {
      // See if the user entered a supported command by checking each character entered
      CommandNode node = root;
      for (int i = 0; i < command.length(); i++) {
        node = node.getChild(command.charAt(i));
        if (node == null) {
          VimPlugin.showMessage(MessageHelper.message(Msg.NOT_EX_CMD, command));
          // No such command
          throw new InvalidCommandException(cmd);
        }
      }

      // We found a valid command
      handler = node.getCommandHandler();
    }

    if (handler == null) {
      VimPlugin.showMessage(MessageHelper.message(Msg.NOT_EX_CMD, command));
      throw new InvalidCommandException(cmd);
    }

    if ((handler.getArgFlags() & CommandHandler.WRITABLE) > 0 && !editor.getDocument().isWritable()) {
      VimPlugin.indicateError();
      return result | RES_READONLY;
    }

    // Run the command
    boolean ok = handler.process(editor, context, new ExCommand(res.getRanges(), command, res.getArgument()), count);
    if (ok && (handler.getArgFlags() & CommandHandler.DONT_SAVE_LAST) == 0) {
      CommandGroups.getInstance().getRegister().storeTextInternal(editor, new TextRange(-1, -1), cmd,
                                                                  SelectionType.CHARACTER_WISE, ':', false);
    }

    if (ok && (handler.getArgFlags() & CommandHandler.KEEP_FOCUS) != 0) {
      result |= RES_MORE_PANEL;
    }

    if ((handler.getArgFlags() & CommandHandler.DONT_REOPEN) != 0) {
      result |= RES_DONT_REOPEN;
    }

    return result;
  }

  /**
   * Parse the text entered by the user. This does not include the leading colon.
   *
   * @param cmd The user entered text
   * @return The parse result
   * @throws ExException if the text is syntactically incorrect
   */
  @NotNull
  public ParseResult parse(@NotNull String cmd) throws ExException {
    // This is a complicated state machine that should probably be rewritten
    if (logger.isDebugEnabled()) {
      logger.debug("processing `" + cmd + "'");
    }
    int state = STATE_START;
    Ranges ranges = new Ranges(); // The list of ranges
    StringBuffer command = new StringBuffer(); // The command
    StringBuffer argument = new StringBuffer(); // The command's argument(s)
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
          case STATE_START: // Very start of the entered text
            if (Character.isLetter(ch) || "~<>@=#*&!".indexOf(ch) >= 0) {
              state = STATE_COMMAND;
            }
            else if (ch == ' ') {
              state = STATE_START;
              reprocess = false;
            }
            else {
              state = STATE_RANGE;
            }
            break;
          case STATE_COMMAND: // Reading the actual command name
            // For commands that start with a non-letter, treat other non-letter characters as part of
            // the argument except for !, <, or >
            if (Character.isLetter(ch) ||
                (command.length() == 0 && "~<>@=#*&!".indexOf(ch) >= 0) ||
                (command.length() > 0 && ch == command.charAt(command.length() - 1) &&
                 "<>".indexOf(ch) >= 0)) {
              command.append(ch);
              reprocess = false;
              if (!Character.isLetter(ch) && "<>".indexOf(ch) < 0) {
                state = STATE_CMD_ARG;
              }
            }
            else {
              state = STATE_CMD_ARG;
            }
            break;
          case STATE_CMD_ARG: // Reading the command's argument
            argument.append(ch);
            reprocess = false;
            break;
          case STATE_RANGE: // Starting a new range
            location = new StringBuffer();
            offsetTotal = 0;
            offsetNumber = 0;
            move = false;
            if (ch >= '0' && ch <= '9') {
              state = STATE_RANGE_LINE;
            }
            else if (ch == '.') {
              state = STATE_RANGE_CURRENT;
            }
            else if (ch == '$') {
              state = STATE_RANGE_LAST;
            }
            else if (ch == '%') {
              state = STATE_RANGE_ALL;
            }
            else if (ch == '\'') {
              state = STATE_RANGE_MARK;
            }
            else if (ch == '+' || ch == '-') {
              location.append('0');
              state = STATE_RANGE_OFFSET;
            }
            else if (ch == '\\') {
              location.append(ch);
              state = STATE_RANGE_SHORT_PATTERN;
              reprocess = false;
            }
            else if (ch == '/' || ch == '?') {
              location.append(ch);
              patternType = ch;
              backCount = 0;
              inBrackets = false;
              state = STATE_RANGE_PATTERN;
              reprocess = false;
            }
            else {
              error = MessageHelper.message(Msg.e_badrange, Character.toString(ch));
              state = STATE_ERROR;
              reprocess = false;
            }
            break;
          case STATE_RANGE_SHORT_PATTERN: // Handle \/, \?, and \& patterns
            if (ch == '/' || ch == '?' || ch == '&') {
              location.append(ch);
              state = STATE_RANGE_PATTERN_MAYBE_DONE;
              reprocess = false;
            }
            else {
              error = MessageHelper.message(Msg.e_backslash);
              state = STATE_ERROR;
              reprocess = false;
            }
            break;
          case STATE_RANGE_PATTERN: // Reading a pattern range
            // No trailing / or ? required if there is no command so look for newline to tell us we are done
            if (ch == '\n') {
              location.append(patternType);
              state = STATE_RANGE_MAYBE_DONE;
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
                state = STATE_RANGE_PATTERN_MAYBE_DONE;
              }

              // No more backslashes
              if (ch != '\\') {
                backCount = 0;
              }

              reprocess = false;
            }
            break;
          case STATE_RANGE_PATTERN_MAYBE_DONE: // Check to see if there is another immediate pattern
            if (ch == '/' || ch == '?') {
              // Use a special character to separate pattern for later, easier, parsing
              location.append('\u0000');
              location.append(ch);
              patternType = ch;
              backCount = 0;
              inBrackets = false;
              state = STATE_RANGE_PATTERN;
              reprocess = false;
            }
            else {
              state = STATE_RANGE_MAYBE_DONE;
            }
            break;
          case STATE_RANGE_LINE: // Explicit line number
            if (ch >= '0' && ch <= '9') {
              location.append(ch);
              state = STATE_RANGE_MAYBE_DONE;
              reprocess = false;
            }
            else {
              state = STATE_RANGE_MAYBE_DONE;
            }
            break;
          case STATE_RANGE_CURRENT: // Current line - .
            location.append(ch);
            state = STATE_RANGE_MAYBE_DONE;
            reprocess = false;
            break;
          case STATE_RANGE_LAST: // Last line - $
            location.append(ch);
            state = STATE_RANGE_MAYBE_DONE;
            reprocess = false;
            break;
          case STATE_RANGE_ALL: // All lines - %
            location.append(ch);
            state = STATE_RANGE_MAYBE_DONE;
            reprocess = false;
            break;
          case STATE_RANGE_MARK: // Mark line - 'x
            location.append(ch);
            state = STATE_RANGE_MARK_CHAR;
            reprocess = false;
            break;
          case STATE_RANGE_MARK_CHAR: // Actual mark
            location.append(ch);
            state = STATE_RANGE_MAYBE_DONE;
            reprocess = false;
            break;
          case STATE_RANGE_DONE: // We have hit the end of a range - process it
            Range[] range = AbstractRange.createRange(location.toString(), offsetTotal, move);
            if (range == null) {
              error = MessageHelper.message(Msg.e_badrange, Character.toString(ch));
              state = STATE_ERROR;
              reprocess = false;
              break;
            }
            ranges.addRange(range);
            // Could there be more ranges - nope - at end, start command
            if (ch == ':' || ch == '\n') {
              state = STATE_COMMAND;
              reprocess = false;
            }
            // Start of command
            else if (Character.isLetter(ch) || "~<>@=#*&!".indexOf(ch) >= 0 || ch == ' ') {
              state = STATE_START;
            }
            // We have another range
            else {
              state = STATE_RANGE;
            }
            break;
          case STATE_RANGE_MAYBE_DONE: // Are we done with the current range?
            // The range has an offset after it
            if (ch == '+' || ch == '-') {
              state = STATE_RANGE_OFFSET;
            }
            // End of the range - we found a separator
            else if (ch == ',' || ch == ';') {
              state = STATE_RANGE_SEPARATOR;
            }
            // Part of a line number
            else if (ch >= '0' && ch <= '9') {
              state = STATE_RANGE_LINE;
            }
            // No more range
            else {
              state = STATE_RANGE_DONE;
            }
            break;
          case STATE_RANGE_OFFSET: // Offset after a range
            // Figure out the sign of the offset and reset the offset value
            offsetNumber = 0;
            if (ch == '+') {
              offsetSign = 1;
            }
            else if (ch == '-') {
              offsetSign = -1;
            }
            state = STATE_RANGE_OFFSET_MAYBE_DONE;
            reprocess = false;
            break;
          case STATE_RANGE_OFFSET_MAYBE_DONE: // Are we done with the offset?
            // We found an offset value
            if (ch >= '0' && ch <= '9') {
              state = STATE_RANGE_OFFSET_NUM;
            }
            // Yes, offset done
            else {
              state = STATE_RANGE_OFFSET_DONE;
            }
            break;
          case STATE_RANGE_OFFSET_DONE: // At the end of a range offset
            // No number implies a one
            if (offsetNumber == 0) {
              offsetNumber = 1;
            }
            // Update offset total for this range
            offsetTotal += offsetNumber * offsetSign;

            // Another offset
            if (ch == '+' || ch == '-') {
              state = STATE_RANGE_OFFSET;
            }
            // No more offsets for this range
            else {
              state = STATE_RANGE_MAYBE_DONE;
            }
            break;
          case STATE_RANGE_OFFSET_NUM: // An offset number
            // Update the value of the current offset
            if (ch >= '0' && ch <= '9') {
              offsetNumber = offsetNumber * 10 + (ch - '0');
              state = STATE_RANGE_OFFSET_MAYBE_DONE;
              reprocess = false;
            }
            // Found the start of a new offset
            else if (ch == '+' || ch == '-') {
              state = STATE_RANGE_OFFSET_DONE;
            }
            else {
              state = STATE_RANGE_OFFSET_MAYBE_DONE;
            }
            break;
          case STATE_RANGE_SEPARATOR: // Found a range separator
            if (ch == ',') {
              move = false;
            }
            else if (ch == ';') {
              move = true;
            }
            state = STATE_RANGE_DONE;
            reprocess = false;
            break;
        }
      }

      // Oops - bad command string
      if (state == STATE_ERROR) {
        VimPlugin.showMessage(error);
        throw new InvalidCommandException(cmd);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("ranges = " + ranges);
      logger.debug("command = " + command);
      logger.debug("argument = " + argument);
    }

    return new ParseResult(ranges, command.toString(), argument.toString().trim());
  }

  /**
   * Adds a command handler to the parser
   *
   * @param handler The new handler to add
   */
  public void addHandler(@NotNull CommandHandler handler) {
    // Iterator through each command name alias
    CommandName[] names = handler.getNames();
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
        cn = node.addChild(text.charAt(text.length() - 1), handler);
      }
      else {
        cn.setCommandHandler(handler);
      }
      node = cn;

      // Now add the handler for each character in the optional portion of the command name
      text = name.getOptional();
      for (int i = 0; i < text.length(); i++) {
        cn = node.getChild(text.charAt(i));
        if (cn == null) {
          cn = node.addChild(text.charAt(i), handler);
        }
        else if (cn.getCommandHandler() == null) {
          cn.setCommandHandler(handler);
        }

        node = cn;
      }
    }
  }

  @NotNull private CommandNode root = new CommandNode();
  private boolean registered = false;

  private static CommandParser ourInstance;

  private static final int STATE_START = 1;
  private static final int STATE_COMMAND = 10;
  private static final int STATE_CMD_ARG = 11;
  private static final int STATE_RANGE = 20;
  private static final int STATE_RANGE_LINE = 21;
  private static final int STATE_RANGE_CURRENT = 22;
  private static final int STATE_RANGE_LAST = 23;
  private static final int STATE_RANGE_MARK = 24;
  private static final int STATE_RANGE_MARK_CHAR = 25;
  private static final int STATE_RANGE_ALL = 26;
  private static final int STATE_RANGE_PATTERN = 27;
  private static final int STATE_RANGE_SHORT_PATTERN = 28;
  private static final int STATE_RANGE_PATTERN_MAYBE_DONE = 29;
  private static final int STATE_RANGE_OFFSET = 30;
  private static final int STATE_RANGE_OFFSET_NUM = 31;
  private static final int STATE_RANGE_OFFSET_DONE = 32;
  private static final int STATE_RANGE_OFFSET_MAYBE_DONE = 33;
  private static final int STATE_RANGE_SEPARATOR = 40;
  private static final int STATE_RANGE_MAYBE_DONE = 50;
  private static final int STATE_RANGE_DONE = 51;
  private static final int STATE_ERROR = 99;

  private static Logger logger = Logger.getInstance(CommandParser.class.getName());
}
