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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoundedStringListOption extends StringListOption {
  protected final @NotNull List<String> allowedValues;

  public BoundedStringListOption(@NonNls String name,
                                 @NonNls String abbrev,
                                 @NonNls String[] defaultValues,
                                 @NonNls String[] allowedValues) {
    super(name, abbrev, defaultValues);

    this.allowedValues = new ArrayList<>(Arrays.asList(allowedValues));
  }

  @Override
  protected @Nullable String convertToken(@NotNull String token) {
    return allowedValues.contains(token) ? token : null;
  }
}
