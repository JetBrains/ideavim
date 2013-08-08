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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class BoundListOption extends ListOption {
  BoundListOption(String name, String abbrev, String[] dflt, String[] values) {
    super(name, abbrev, dflt, null);

    this.values = new ArrayList<String>(Arrays.asList(values));
  }

  public boolean set(String val) {
    List<String> vals = parseVals(val);
    if (values.containsAll(vals)) {
      set(vals);
    }

    return true;
  }

  public boolean append(String val) {
    List<String> vals = parseVals(val);
    if (values.containsAll(vals)) {
      append(vals);
    }

    return true;
  }

  public boolean prepend(String val) {
    List<String> vals = parseVals(val);
    if (values.containsAll(vals)) {
      prepend(vals);
    }

    return true;
  }

  public boolean remove(String val) {
    List<String> vals = parseVals(val);
    if (values.containsAll(vals)) {
      remove(vals);
    }

    return true;
  }

  protected List<String> values;
}
