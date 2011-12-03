/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maddyhome.idea.vim.option;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import com.maddyhome.idea.vim.ui.MorePanel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Maintains the set of support options
 */
public class Options {
  /**
   * Gets the singleton instance of the options
   *
   * @return The singleton
   */
  public synchronized static Options getInstance() {
    if (ourInstance == null) {
      ourInstance = new Options();
    }
    return ourInstance;
  }

  /**
   * Convenience method to check if a boolean option is set or not
   *
   * @param name The name of the option to check
   * @return True if set, false if not set or name is invalid or not a boolean option
   */
  public boolean isSet(String name) {
    Option opt = getOption(name);
    if (opt != null && opt instanceof ToggleOption) {
      return ((ToggleOption)opt).getValue();
    }

    return false;
  }

  /**
   * Gets an option by the supplied name or short name.
   *
   * @param name The option's name or short name
   * @return The option with the given name or short name. null if there is no such option
   */
  public Option getOption(String name) {
    Option res = options.get(name);
    if (res == null) {
      res = abbrevs.get(name);
    }

    return res;
  }

  /**
   * Gets all options
   *
   * @return All options
   */
  Collection<Option> allOptions() {
    return options.values();
  }

  /**
   * Gets only options that have values different from their default values
   *
   * @return The set of changed options
   */
  Collection<Option> changedOptions() {
    ArrayList<Option> res = new ArrayList<Option>();
    for (Option option : options.values()) {
      if (!option.isDefault()) {
        res.add(option);
      }
    }

    return res;
  }

  /**
   * This parses a set of :set commands. The following types of commands are supported:
   * <ul>
   * <li>:set - show all changed options</li>
   * <li>:set all - show all options</li>
   * <li>:set all& - reset all options to default values</li>
   * <li>:set {option} - set option of boolean, display others</li>
   * <li>:set {option}? - display option</li>
   * <li>:set no{option} - reset boolean option</li>
   * <li>:set inv{option} - toggle boolean option</li>
   * <li>:set {option}! - toggle boolean option</li>
   * <li>:set {option}& - set option to default</li>
   * <li>:set {option}={value} - set option to new value</li>
   * <li>:set {option}:{value} - set option to new value</li>
   * <li>:set {option}+={value} - append or add to option value</li>
   * <li>:set {option}-={value} - remove or subtract from option value</li>
   * <li>:set {option}^={value} - prepend or multiply option value</li>
   * </ul>
   *
   * @param editor    The editor the command was entered for, null if no editor - reading .vimrc
   * @param args      The :set command arguments
   * @param failOnBad True if processing should stop when a bad argument is found, false if a bad argument is simply
   *                  skipped and processing continues.
   * @return True if no errors were found, false if there were any errors
   */
  public boolean parseOptionLine(Editor editor, String args, boolean failOnBad) {
    // No arguments so we show changed values
    if (args.length() == 0) {
      showOptions(editor, changedOptions(), true);

      return true;
    }
    // Arg is all so show all options
    else if (args.equals("all")) {
      showOptions(editor, allOptions(), true);

      return true;
    }
    // Reset all options to default
    else if (args.equals("all&")) {
      resetAllOptions();

      return true;
    }

    // We now have 1 or more option operators separator by spaces
    String error = null;
    String token = null;
    StringTokenizer tokenizer = new StringTokenizer(args);
    ArrayList<Option> toShow = new ArrayList<Option>();
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      // See if a space has been backslashed, if no get the rest of the text
      while (token.endsWith("\\")) {
        token = token.substring(0, token.length() - 1) + ' ';
        if (tokenizer.hasMoreTokens()) {
          token += tokenizer.nextToken();
        }
      }

      // Print the value of an option
      if (token.endsWith("?")) {
        String option = token.substring(0, token.length() - 1);
        Option opt = getOption(option);
        if (opt != null) {
          toShow.add(opt);
        }
        else {
          error = Msg.unkopt;
        }
      }
      // Reset a boolean option
      else if (token.startsWith("no")) {
        String option = token.substring(2);
        Option opt = getOption(option);
        if (opt != null) {
          if (opt instanceof ToggleOption) {
            ((ToggleOption)opt).reset();
          }
          else {
            error = Msg.e_invarg;
          }
        }
        else {
          error = Msg.unkopt;
        }
      }
      // Toggle a boolean option
      else if (token.startsWith("inv")) {
        String option = token.substring(3);
        Option opt = getOption(option);
        if (opt != null) {
          if (opt instanceof ToggleOption) {
            ((ToggleOption)opt).toggle();
          }
          else {
            error = Msg.e_invarg;
          }
        }
        else {
          error = Msg.unkopt;
        }
      }
      // Toggle a boolean option
      else if (token.endsWith("!")) {
        String option = token.substring(0, token.length() - 1);
        Option opt = getOption(option);
        if (opt != null) {
          if (opt instanceof ToggleOption) {
            ((ToggleOption)opt).toggle();
          }
          else {
            error = Msg.e_invarg;
          }
        }
        else {
          error = Msg.unkopt;
        }
      }
      // Reset option to default
      else if (token.endsWith("&")) {
        String option = token.substring(0, token.length() - 1);
        Option opt = getOption(option);
        if (opt != null) {
          opt.resetDefault();
        }
        else {
          error = Msg.unkopt;
        }
      }
      // This must be one of =, :, +=, -=, or ^=
      else {
        // Look for the = or : first
        int eq = token.indexOf('=');
        if (eq == -1) {
          eq = token.indexOf(':');
        }
        // No operator so only the option name was given
        if (eq == -1) {
          Option opt = getOption(token);
          if (opt != null) {
            // Valid option so set booleans or display others
            if (opt instanceof ToggleOption) {
              ((ToggleOption)opt).set();
            }
            else {
              toShow.add(opt);
            }
          }
          else {
            error = Msg.unkopt;
          }
        }
        // We have an operator
        else {
          // Make sure there is an option name
          if (eq > 0) {
            // See if an operator before the equal sign
            char op = token.charAt(eq - 1);
            int end = eq;
            if ("+-^".indexOf(op) != -1) {
              end--;
            }
            // Get option name and value after operator
            String option = token.substring(0, end);
            String value = token.substring(eq + 1);
            Option opt = getOption(option);
            if (opt != null) {
              // If not a boolean
              if (opt instanceof TextOption) {
                TextOption to = (TextOption)opt;
                boolean res;
                switch (op) {
                  case '+':
                    res = to.append(value);
                    break;
                  case '-':
                    res = to.remove(value);
                    break;
                  case '^':
                    res = to.prepend(value);
                    break;
                  default:
                    res = to.set(value);
                }
                if (!res) {
                  error = Msg.e_invarg;
                }
              }
              // boolean option - no good
              else {
                error = Msg.e_invarg;
              }
            }
            else {
              error = Msg.unkopt;
            }
          }
          else {
            error = Msg.unkopt;
          }
        }
      }

      if (failOnBad && error != null) {
        break;
      }
    }

    // Now show all options that were individually requested
    if (toShow.size() > 0) {
      showOptions(editor, toShow, false);
    }

    if (editor != null && error != null) {
      VimPlugin.showMessage(MessageHelper.message(error, token));
      VimPlugin.indicateError();
    }

    return error == null;
  }

  /**
   * Resets all options to their default value
   */
  private void resetAllOptions() {
    Collection<Option> opts = allOptions();
    for (Option option : opts) {
      option.resetDefault();
    }
  }

  /**
   * Shows the set of options
   *
   * @param editor    The editor to show them in - if null, this is aborted
   * @param opts      The list of options to display
   * @param showIntro True if intro is displayed, false if not
   */
  private void showOptions(Editor editor, Collection<Option> opts, boolean showIntro) {
    if (editor == null) {
      return;
    }

    ArrayList<Option> cols = new ArrayList<Option>();
    ArrayList<Option> extra = new ArrayList<Option>();
    for (Option option : opts) {
      if (option.toString().length() > 19) {
        extra.add(option);
      }
      else {
        cols.add(option);
      }
    }

    Collections.sort(cols, new Option.NameSorter<Option>());
    Collections.sort(extra, new Option.NameSorter<Option>());

    String pad = "                    ";
    MorePanel panel = MorePanel.getInstance(editor);
    int width = panel.getDisplayWidth();
    if (width < 20) {
      width = 80;
    }
    int colCount = width / 20;
    int height = (int)Math.ceil((double)cols.size() / (double)colCount);
    int empty = cols.size() % colCount;
    empty = empty == 0 ? colCount : empty;

    if (logger.isDebugEnabled()) {
      logger.debug("width=" + width);
      logger.debug("colCount=" + colCount);
      logger.debug("height=" + height);
    }

    StringBuffer res = new StringBuffer();
    if (showIntro) {
      res.append("--- Options ---\n");
    }
    for (int h = 0; h < height; h++) {
      for (int c = 0; c < colCount; c++) {
        if (h == height - 1 && c >= empty) {
          break;
        }

        int pos = c * height + h;
        if (c > empty) {
          pos -= c - empty;
        }

        Option opt = cols.get(pos);
        String val = opt.toString();
        res.append(val);
        res.append(pad.substring(0, 20 - val.length()));
      }
      res.append("\n");
    }

    for (Option opt : extra) {
      String val = opt.toString();
      int seg = (val.length() - 1) / width;
      for (int j = 0; j <= seg; j++) {
        res.append(val.substring(j * width, Math.min(j * width + width, val.length())));
        res.append("\n");
      }
    }

    panel.setText(res.toString());
  }

  /**
   * Create all the options
   */
  private Options() {
    createDefaultOptions();
    loadVimrc();
  }

  /**
   * Attempts to load all :set commands from the user's .vimrc file if found
   */
  private void loadVimrc() {
    // Look in the JVM's idea of the user's home directory for .vimrc or _vimrc
    String home = System.getProperty("user.home");
    if (home != null) {
      File rc = new File(home, ".vimrc");
      if (!rc.exists()) {
        rc = new File(home, "_vimrc");
        if (!rc.exists()) {
          return;
        }
      }

      if (logger.isDebugEnabled()) logger.debug("found vimrc at " + rc);

      try {
        BufferedReader br = new BufferedReader(new FileReader(rc));
        String line;
        while ((line = br.readLine()) != null) {
          if (line.startsWith(":set") || line.startsWith("set")) {
            int pos = line.indexOf(' ');
            parseOptionLine(null, line.substring(pos).trim(), false);
          }
        }
      }
      catch (Exception e) {
        // no-op
      }
    }
  }

  /**
   * Creates all the supported options
   */
  private void createDefaultOptions() {
    addOption(new ToggleOption("digraph", "dg", false));
    addOption(new ToggleOption("gdefault", "gd", false));
    addOption(new NumberOption("history", "hi", 20, 1, Integer.MAX_VALUE));
    addOption(new ToggleOption("hlsearch", "hls", false));
    addOption(new ToggleOption("ignorecase", "ic", false));
    addOption(new ListOption("matchpairs", "mps", new String[]{"(:)", "{:}", "[:]"}, ".:."));
    addOption(new ToggleOption("more", "more", true));
    addOption(new BoundListOption("nrformats", "nf", new String[]{"octal", "hex"}, new String[]{"octal", "hex", "alpha"}));
    addOption(new NumberOption("scroll", "scr", 0));
    addOption(new NumberOption("scrolljump", "sj", 1));
    addOption(new NumberOption("scrolloff", "so", 0));
    addOption(new BoundStringOption("selection", "sel", "inclusive", new String[]{"old", "inclusive", "exclusive"}));
    addOption(new ToggleOption("showmode", "smd", false));
    addOption(new NumberOption("sidescroll", "ss", 0));
    addOption(new NumberOption("sidescrolloff", "siso", 0));
    addOption(new ToggleOption("smartcase", "scs", false));
    addOption(new NumberOption("undolevels", "ul", 1000, -1, Integer.MAX_VALUE));
    addOption(new ToggleOption("visualbell", "vb", false));
    addOption(new ToggleOption("wrapscan", "ws", true));
  }

  private void addOption(Option option) {
    options.put(option.getName(), option);
    abbrevs.put(option.getAbbreviation(), option);
  }

  private HashMap<String, Option> options = new HashMap<String, Option>();
  private HashMap<String, Option> abbrevs = new HashMap<String, Option>();

  private static Options ourInstance;

  private static Logger logger = Logger.getInstance(Options.class.getName());
}

