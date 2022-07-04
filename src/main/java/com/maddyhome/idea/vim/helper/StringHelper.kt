/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.helper;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;

/**
 * COMPATIBILITY-LAYER: Created a helper class
 */
public class StringHelper {
  public static List<KeyStroke> parseKeys(String string) {
    return injector.getParser().parseKeys(string);
  }

  public static List<KeyStroke> parseKeys(String... string) {
    return Arrays.stream(string).flatMap(o -> injector.getParser().parseKeys(o).stream()).collect(Collectors.toList());
  }

  public static boolean isCloseKeyStroke(KeyStroke stroke) {
    return StringAndKeysKt.isCloseKeyStroke(stroke);
  }
}
