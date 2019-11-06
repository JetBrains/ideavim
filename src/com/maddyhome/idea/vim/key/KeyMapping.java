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

package com.maddyhome.idea.vim.key;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * Container for key mappings for some mode
 * Iterable by "from" keys
 *
 * @author vlan
 */
public class KeyMapping implements Iterable<List<KeyStroke>> {
  /** Contains all key mapping for some mode. */
  @NotNull private final Map<List<KeyStroke>, MappingInfo> myKeys = new HashMap<>();
  /**
   * Set the contains all possible prefixes for mappings.
   * E.g. if there is mapping for "hello", this set will contain "h", "he", "hel", etc.
   * Multiset is used to correctly remove the mappings.
   */
  @NotNull private final Multiset<List<KeyStroke>> myPrefixes = HashMultiset.create();

  @NotNull
  @Override
  public Iterator<List<KeyStroke>> iterator() {
    return new ArrayList<>(myKeys.keySet()).iterator();
  }

  @Nullable
  public MappingInfo get(@NotNull List<KeyStroke> keys) {
    return myKeys.get(keys);
  }

  public void put(@NotNull Set<MappingMode> mappingModes,
                  @NotNull List<KeyStroke> fromKeys,
                  @Nullable List<KeyStroke> toKeys,
                  @Nullable VimExtensionHandler extensionHandler,
                  boolean recursive) {
    myKeys.put(new ArrayList<>(fromKeys),
               new MappingInfo(mappingModes, fromKeys, toKeys, extensionHandler, recursive));
    List<KeyStroke> prefix = new ArrayList<>();
    final int prefixLength = fromKeys.size() - 1;
    for (int i = 0; i < prefixLength; i++) {
      prefix.add(fromKeys.get(i));
      myPrefixes.add(new ArrayList<>(prefix));
    }
  }

  public void delete(@NotNull List<KeyStroke> keys) {
    myKeys.remove(keys);
    List<KeyStroke> prefix = new ArrayList<>();
    final int prefixLength = keys.size() - 1;
    for (int i = 0; i < prefixLength; i++) {
      prefix.add(keys.get(i));
      myPrefixes.remove(prefix);
    }
  }

  public boolean isPrefix(@NotNull List<KeyStroke> keys) {
    return myPrefixes.contains(keys);
  }
}
