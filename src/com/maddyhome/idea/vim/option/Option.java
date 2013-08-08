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

package com.maddyhome.idea.vim.option;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents an VIM options that can be set with the :set command. Listeners can be set that are interested in knowing
 * when the value of the option changes.
 */
public abstract class Option {
  /**
   * Create the option
   *
   * @param name   The name of the option
   * @param abbrev The short name
   */
  protected Option(String name, String abbrev) {
    this.name = name;
    this.abbrev = abbrev;
  }

  /**
   * Registers an option change listener. The listener will receive an OptionChangeEvent whenever the value of this
   * option changes.
   *
   * @param listener The listener
   */
  public void addOptionChangeListener(OptionChangeListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes the listener from the list.
   *
   * @param listener The listener
   */
  public void removeOptionChangeListener(OptionChangeListener listener) {
    listeners.remove(listener);
  }

  /**
   * The name of the option
   *
   * @return The option's name
   */
  public String getName() {
    return name;
  }

  /**
   * The short name of the option
   *
   * @return The option's short name
   */
  public String getAbbreviation() {
    return abbrev;
  }

  /**
   * Checks to see if the option's current value equals the default value
   *
   * @return True if equal to default, false if not.
   */
  public abstract boolean isDefault();

  /**
   * Sets the option to its default value.
   */
  public abstract void resetDefault();

  /**
   * Lets all listeners know that the value has changed. Subclasses are responsible for calling this when their
   * value changes.
   */
  protected void fireOptionChangeEvent() {
    OptionChangeEvent event = new OptionChangeEvent(this);
    for (OptionChangeListener listener : listeners) {
      listener.valueChange(event);
    }
  }

  /**
   * Helper method used to sort lists of options by their name
   */
  static class NameSorter<V> implements Comparator<V> {
    public int compare(@NotNull V o1, @NotNull V o2) {
      return ((Option)o1).name.compareTo(((Option)o2).name);
    }
  }

  protected String name;
  protected String abbrev;
  @NotNull protected List<OptionChangeListener> listeners = new ArrayList<OptionChangeListener>();
}
