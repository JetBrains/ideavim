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

package com.maddyhome.idea.vim.option;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This is an option that accepts an arbitrary list of values
 */
public class ListOption extends TextOption {
  @NotNull public final static ListOption empty = new ListOption("", "", new String[0], "");

  /**
   * Gets the value of the option as a comma separated list of values
   *
   * @return The option's value
   */
  @Override
  @NotNull
  public String getValue() {
    StringBuilder res = new StringBuilder();
    int cnt = 0;
    for (String s : value) {
      if (cnt > 0) {
        res.append(",");
      }
      res.append(s);

      cnt++;
    }

    return res.toString();
  }

  /**
   * Gets the option's values as a list
   *
   * @return The option's values
   */
  @NotNull
  public List<String> values() {
    return value;
  }

  /**
   * Determines if this option has all the values listed on the supplied value
   *
   * @param val A comma separated list of values to look for
   * @return True if all the supplied values are set in this option, false if not
   */
  public boolean contains(String val) {
    final List<String> vals = parseVals(val);
    return vals != null && value.containsAll(vals);
  }

  /**
   * Sets this option to contain just the supplied values. If any of the supplied values are invalid then this
   * option is not updated at all.
   *
   * @param val A comma separated list of values
   * @return True if all the supplied values were correct, false if not
   */
  @Override
  public boolean set(String val) {
    return set(parseVals(val));
  }

  /**
   * Adds the supplied values to the current list of values. If any of the supplied values are invalid then this
   * option is not updated at all.
   *
   * @param val A comma separated list of values
   * @return True if all the supplied values were correct, false if not
   */
  @Override
  public boolean append(String val) {
    return append(parseVals(val));
  }

  /**
   * Adds the supplied values to the start of the current list of values. If any of the supplied values are invalid
   * then this option is not updated at all.
   *
   * @param val A comma separated list of values
   * @return True if all the supplied values were correct, false if not
   */
  @Override
  public boolean prepend(String val) {
    return prepend(parseVals(val));
  }

  /**
   * Removes the supplied values from the current list of values. If any of the supplied values are invalid then this
   * option is not updated at all.
   *
   * @param val A comma separated list of values
   * @return True if all the supplied values were correct, false if not
   */
  @Override
  public boolean remove(String val) {
    return remove(parseVals(val));
  }

  protected boolean set(@Nullable List<String> vals) {
    if (vals == null) {
      return false;
    }

    value = vals;
    fireOptionChangeEvent();

    return true;
  }

  protected boolean append(@Nullable List<String> vals) {
    if (vals == null) {
      return false;
    }

    value.addAll(vals);
    fireOptionChangeEvent();

    return true;
  }

  protected boolean prepend(@Nullable List<String> vals) {
    if (vals == null) {
      return false;
    }

    value.addAll(0, vals);
    fireOptionChangeEvent();

    return true;
  }

  protected boolean remove(@Nullable List<String> vals) {
    if (vals == null) {
      return false;
    }

    value.removeAll(vals);
    fireOptionChangeEvent();

    return true;
  }

  /**
   * Creates the option
   *
   * @param name    The name of the option
   * @param abbrev  The short name
   * @param dflt    The option's default values
   * @param pattern A regular expression that is used to validate new values. null if no check needed
   */
  ListOption(String name, String abbrev, String[] dflt, String pattern) {
    super(name, abbrev);

    this.dflt = new ArrayList<>(Arrays.asList(dflt));
    this.value = new ArrayList<>(this.dflt);
    this.pattern = pattern;
  }

  /**
   * Checks to see if the current value of the option matches the default value
   *
   * @return True if equal to default, false if not
   */
  @Override
  public boolean isDefault() {
    return dflt.equals(value);
  }

  @Nullable
  protected List<String> parseVals(String val) {
    List<String> res = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(val, ",");
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken().trim();
      if (pattern == null || token.matches(pattern)) {
        res.add(token);
      }
      else {
        return null;
      }
    }

    return res;
  }

  /**
   * Gets the string representation appropriate for output to :set all
   *
   * @return The option as a string {name}={value list}
   */
  @NotNull
  public String toString() {
    return "  " + getName() + "=" + getValue();
  }

  @NotNull protected final List<String> dflt;
  @NotNull protected List<String> value;
  protected final String pattern;

  /**
   * Resets the option to its default value
   */
  @Override
  public void resetDefault() {
    if (!dflt.equals(value)) {
      value = new ArrayList<>(dflt);
      fireOptionChangeEvent();
    }
  }
}
