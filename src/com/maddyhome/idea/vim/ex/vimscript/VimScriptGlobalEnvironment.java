/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex.vimscript;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author vlan
 */
public class VimScriptGlobalEnvironment {
  private static final VimScriptGlobalEnvironment ourInstance = new VimScriptGlobalEnvironment();

  private final Map<String, Object> myVariables = new HashMap<>();

  private VimScriptGlobalEnvironment() {
  }

  public static @NotNull VimScriptGlobalEnvironment getInstance() {
    return ourInstance;
  }

  public @NotNull Map<String, Object> getVariables() {
    return myVariables;
  }
}
