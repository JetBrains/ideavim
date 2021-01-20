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

/**
 * Represents an option with a numeric value
 */
public class NumberOption extends TextOption {
  /**
   * Creates a number option that must contain a zero or positive value
   *
   * @param name   The name of the option
   * @param abbrev The short name
   * @param dflt   The default value
   */
  NumberOption(@NonNls String name, @NonNls String abbrev, int dflt) {
    this(name, abbrev, dflt, 0, Integer.MAX_VALUE);
  }

  /**
   * Creates a number option
   *
   * @param name   The name of the option
   * @param abbrev The short name
   * @param dflt   The default value
   * @param min    The option's minimum value
   * @param max    The option's maximum value
   */
  NumberOption(@VimNlsSafe String name, @VimNlsSafe String abbrev, int dflt, int min, int max) {
    super(name, abbrev);
    this.dflt = dflt;
    this.value = dflt;
    this.min = min;
    this.max = Integer.MAX_VALUE;
  }

  /**
   * Gets the option's value as a string
   *
   * @return The option's value
   */
  @Override
  public String getValue() {
    return Integer.toString(value);
  }

  /**
   * Gets the option's value as an int
   *
   * @return The option's value
   */
  public int value() {
    return value;
  }

  /**
   * Sets the option's value if the value is in the proper range
   *
   * @param val The new value
   * @return True if the set, false if new value is invalid
   */
  public boolean set(int val) {
    return set(Integer.toString(val));
  }

  /**
   * Sets the option's value after parsing the string into a number. The supplied value can be in decimal,
   * hex, or octal formats. Octal numbers must be preceded with a zero and all digits must be 0 - 7. Hex values
   * must start with 0x or 0X and all digits must be 0 - 9, a - F, or A - F. All others will be tried as a decimal
   * number.
   *
   * @param val The new value
   * @return True if the string can be converted to a number and it is in range. False if not.
   */
  @Override
  public boolean set(String val) {
    Integer num = asNumber(val);
    if (num == null) {
      return false;
    }

    if (inRange(num)) {

      String oldValue = getValue();
      this.value = num;
      fireOptionChangeEvent(oldValue, getValue());

      return true;
    }

    return false;
  }

  /**
   * Adds the value to the option's value after parsing the string into a number. The supplied value can be in decimal,
   * hex, or octal formats. Octal numbers must be preceded with a zero and all digits must be 0 - 7. Hex values
   * must start with 0x or 0X and all digits must be 0 - 9, a - F, or A - F. All others will be tried as a decimal
   * number.
   *
   * @param val The new value
   * @return True if the string can be converted to a number and the result is in range. False if not.
   */
  @Override
  public boolean append(String val) {
    Integer num = asNumber(val);
    if (num == null) {
      return false;
    }

    if (inRange(value + num)) {
      String oldValue = getValue();
      value += num;
      fireOptionChangeEvent(oldValue, getValue());

      return true;
    }

    return false;
  }

  /**
   * Multiplies the value by the option's value after parsing the string into a number. The supplied value can be in
   * decimal, hex, or octal formats. Octal numbers must be preceded with a zero and all digits must be 0 - 7. Hex
   * values must start with 0x or 0X and all digits must be 0 - 9, a - F, or A - F. All others will be tried as a
   * decimal number.
   *
   * @param val The new value
   * @return True if the string can be converted to a number and the result is in range. False if not.
   */
  @Override
  public boolean prepend(String val) {
    Integer num = asNumber(val);
    if (num == null) {
      return false;
    }

    if (inRange(value * num)) {
      String oldValue = getValue();
      value *= num;
      fireOptionChangeEvent(oldValue, getValue());

      return true;
    }

    return false;
  }

  /**
   * Substracts the value from the option's value after parsing the string into a number. The supplied value can be in
   * decimal, hex, or octal formats. Octal numbers must be preceded with a zero and all digits must be 0 - 7. Hex
   * values must start with 0x or 0X and all digits must be 0 - 9, a - F, or A - F. All others will be tried as a
   * decimal number.
   *
   * @param val The new value
   * @return True if the string can be converted to a number and the result is in range. False if not.
   */
  @Override
  public boolean remove(String val) {
    Integer num = asNumber(val);
    if (num == null) {
      return false;
    }

    if (inRange(value - num)) {
      String oldValue = getValue();
      value -= num;
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
    return value == dflt;
  }

  /**
   * Sets the option to its default value.
   */
  @Override
  public void resetDefault() {
    if (dflt != value) {
      String oldValue = getValue();
      value = dflt;
      fireOptionChangeEvent(oldValue, getValue());
    }
  }

  protected @Nullable Integer asNumber(String val) {
    try {
      return Integer.decode(val);
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  protected boolean inRange(int val) {
    return (val >= min && val <= max);
  }

  /**
   * {name}={value}
   *
   * @return The option as a string
   */
  public @NotNull String toString() {

    return "  " + getName() + "=" + value;
  }

  private final int dflt;
  private int value;
  private final int min;
  private final int max;
}
