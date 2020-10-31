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

package com.maddyhome.idea.vim.extension;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.key.MappingOwner;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * @author vlan
 */
public interface VimExtension {
  @NotNull ExtensionPointName<VimExtension> EP_NAME = ExtensionPointName.create("IdeaVIM.vimExtension");

  @NotNull String getName();

  /**
   * List of aliases for the extension. These aliases are used to support `Plug` and `Plugin` commands.
   * Technically, it would be enough to save here github link and short version of it ('author/plugin'),
   *   but it may contain more aliases just in case.
   */
  default Set<String> getAliases() {
    return Collections.emptySet();
  }

  default MappingOwner getOwner() {
    return MappingOwner.Plugin.Companion.get(getName());
  }

  void init();

  default void dispose() {
    VimPlugin.getKey().removeKeyMapping(getOwner());
  }
}
