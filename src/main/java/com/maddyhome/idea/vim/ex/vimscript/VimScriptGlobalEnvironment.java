/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ex.vimscript;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author vlan
 */

@Deprecated() // please use VariableService instead
@ApiStatus.ScheduledForRemoval(inVersion = "1.12")
public class VimScriptGlobalEnvironment {
  private static final VimScriptGlobalEnvironment ourInstance = new VimScriptGlobalEnvironment();

  private final Map<String, Object> myVariables = new HashMap<>();

  private VimScriptGlobalEnvironment() {
  }

  public static @NotNull
  VimScriptGlobalEnvironment getInstance() {
    return ourInstance;
  }

  public @NotNull
  Map<String, Object> getVariables() {
    return myVariables;
  }
}
