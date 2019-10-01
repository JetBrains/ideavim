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

/**
 * Represents a boolean option
 */
public class ToggleOption extends Option {
  /**
   * Creates the option
   *
   * @param name   The option's name
   * @param abbrev The short name
   * @param dflt   The default value
   */
  public ToggleOption(String name, String abbrev, boolean dflt) {
    super(name, abbrev);

    this.dflt = dflt;
    this.value = dflt;
  }

  /**
   * The option's value
   *
   * @return The value
   */
  public boolean getValue() {
    return value;
  }

  /**
   * Sets the on (true)
   */
  public void set() {
    update(true);
  }

  /**
   * Resets the option (false)
   */
  public void reset() {
    update(false);
  }

  /**
   * Toggles the option's value (false to true, true to false)
   */
  public void toggle() {
    update(!value);
  }

  public boolean isSet() {
    return value;
  }

  /**
   * Helper to set the value only it is changing and notify listeners
   *
   * @param val The new value
   */
  private void update(boolean val) {
    boolean old = value;
    value = val;
    if (val != old) {
      fireOptionChangeEvent();
    }
  }

  /**
   * The display value of the option [no]{name}
   *
   * @return The option's display value
   */
  @NotNull
  public String toString() {
    StringBuilder res = new StringBuilder();
    if (!value) {
      res.append("no");
    }
    else {
      res.append("  ");
    }

    res.append(getName());

    return res.toString();
  }

  /**
   * Checks to see if the option's current value equals the default value
   *
   * @return True if equal to default, false if not.
   */
  @Override
  public boolean isDefault() {
    return value == dflt;
  }

  /**
   * Sets the option to its default value.
   */
  @Override
  public void resetDefault() {
    value = dflt;
  }

  protected final boolean dflt;
  protected boolean value;
}
