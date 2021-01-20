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

import org.jetbrains.annotations.NotNull;

/**
 * An option that has an arbitrary string value
 */
public class StringOption extends TextOption {
  /**
   * Creates the string option
   *
   * @param name   The name of the option
   * @param abbrev The short name
   * @param dflt   The default value
   */
  StringOption(String name, String abbrev, String dflt) {
    super(name, abbrev);
    this.dflt = dflt;
    this.value = dflt;
  }

  /**
   * The option's value
   *
   * @return The option's value
   */
  @Override
  public String getValue() {
    return value;
  }

  /**
   * Sets the option to the new value
   *
   * @param val The new value
   * @return True
   */
  @Override
  public boolean set(String val) {
    String oldValue = getValue();
    value = val;
    fireOptionChangeEvent(oldValue, getValue());

    return true;
  }

  /**
   * Appends the value to the option's current value
   *
   * @param val The string to append
   * @return True
   */
  @Override
  public boolean append(String val) {
    String oldValue = getValue();
    value += val;
    fireOptionChangeEvent(oldValue, getValue());

    return true;
  }

  /**
   * Prepends the value to the start of the option's current value
   *
   * @param val The string to prepend
   * @return True
   */
  @Override
  public boolean prepend(String val) {
    String oldValue = getValue();
    value = val + value;
    fireOptionChangeEvent(oldValue, getValue());

    return true;
  }

  /**
   * Removes the value if the value is a substring of the option's current value.
   *
   * @param val The substring to remove from the current value
   * @return True if the substring was removed, false if the value is not a substring of the current value
   */
  @Override
  public boolean remove(@NotNull String val) {
    int pos = value.indexOf(val);
    if (pos != -1) {
      String oldValue = getValue();
      value = value.substring(0, pos) + value.substring(pos + val.length());
      fireOptionChangeEvent(oldValue, getValue());

      return true;
    }

    return false;
  }

  /**
   * Checks to see if the option's current value equals the default value
   *
   * @return True if equal to default, false if not.
   */
  @Override
  public boolean isDefault() {
    return dflt.equals(value);
  }

  /**
   * Sets the option to its default value.
   */
  @Override
  public void resetDefault() {
    if (!dflt.equals(value)) {
      String oldValue = getValue();
      value = dflt;
      fireOptionChangeEvent(oldValue, getValue());
    }
  }

  /**
   * The option as {name}={value}
   *
   * @return The option as a string for display
   */
  public @NotNull String toString() {

    return "  " + getName() + "=" + value;
  }

  protected final String dflt;
  protected String value;
}
