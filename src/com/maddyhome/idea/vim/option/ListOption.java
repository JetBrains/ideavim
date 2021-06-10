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

package com.maddyhome.idea.vim.option;

import com.maddyhome.idea.vim.helper.VimNlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This is an option that accepts an arbitrary list of values
 */
public abstract class ListOption<T> extends TextOption {
  protected final @NotNull List<T> defaultValues;
  protected @NotNull List<T> value;

  /**
   * Creates the option
   *
   * @param name    The name of the option
   * @param abbrev  The short name
   * @param defaultValues    The option's default values
   */
  public ListOption(String name, String abbrev, @VimNlsSafe T[] defaultValues) {
    super(name, abbrev);

    this.defaultValues = new ArrayList<>(Arrays.asList(defaultValues));
    this.value = new ArrayList<>(this.defaultValues);
  }

  public ListOption(String name, String abbrev, String defaultValue) {
    super(name, abbrev);

    final List<T> defaultValues = parseVals(defaultValue);
    this.defaultValues = defaultValues != null ? new ArrayList<>(defaultValues) : new ArrayList<>();
    this.value = new ArrayList<>(this.defaultValues);
  }

  /**
   * Gets the value of the option as a comma separated list of values
   *
   * @return The option's value
   */
  @Override
  public @NotNull String getValue() {
    StringBuilder res = new StringBuilder();
    int cnt = 0;
    for (T s : value) {
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
  public @NotNull List<T> values() {
    return value;
  }

  /**
   * Determines if this option has all the values listed on the supplied value
   *
   * @param val A comma separated list of values to look for
   * @return True if all the supplied values are set in this option, false if not
   */
  public boolean contains(@NonNls String val) {
    final List<T> vals = parseVals(val);
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

  protected boolean set(@Nullable List<T> vals) {
    if (vals == null) {
      return false;
    }

    String oldValue = getValue();
    this.value = vals;
    fireOptionChangeEvent(oldValue, getValue());

    return true;
  }

  protected boolean append(@Nullable List<T> vals) {
    if (vals == null) {
      return false;
    }

    String oldValue = getValue();
    value.removeAll(vals);
    value.addAll(vals);
    fireOptionChangeEvent(oldValue, getValue());

    return true;
  }

  protected boolean prepend(@Nullable List<T> vals) {
    if (vals == null) {
      return false;
    }

    String oldValue = getValue();
    value.removeAll(vals);
    value.addAll(0, vals);
    fireOptionChangeEvent(oldValue, getValue());

    return true;
  }

  protected boolean remove(@Nullable List<T> vals) {
    if (vals == null) {
      return false;
    }

    String oldValue = getValue();
    value.removeAll(vals);
    fireOptionChangeEvent(oldValue, getValue());

    return true;
  }

  /**
   * Checks to see if the current value of the option matches the default value
   *
   * @return True if equal to default, false if not
   */
  @Override
  public boolean isDefault() {
    return defaultValues.equals(value);
  }

  protected @Nullable List<T> parseVals(String val) {
    List<T> res = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(val, ",");
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken().trim();
      T item = convertToken(token);
      if (item != null) {
        res.add(item);
      }
      else {
        return null;
      }
    }

    return res;
  }

  protected abstract @Nullable T convertToken(@NotNull String token);

  /**
   * Gets the string representation appropriate for output to :set all
   *
   * @return The option as a string {name}={value list}
   */
  public @NotNull String toString() {
    return "  " + getName() + "=" + getValue();
  }

  /**
   * Resets the option to its default value
   */
  @Override
  public void resetDefault() {
    if (!defaultValues.equals(value)) {
      String oldValue = getValue();
      value = new ArrayList<>(defaultValues);
      fireOptionChangeEvent(oldValue, getValue());
    }
  }
}
