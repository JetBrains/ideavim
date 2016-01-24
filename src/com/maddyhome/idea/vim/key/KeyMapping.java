/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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

package com.maddyhome.idea.vim.key;

import com.google.common.collect.ImmutableList;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author vlan
 */
public class KeyMapping implements Iterable<List<KeyStroke>> {
  @NotNull private final Map<ImmutableList<KeyStroke>, MappingInfo> myKeys = new HashMap<ImmutableList<KeyStroke>, MappingInfo>();
  @NotNull private final Map<ImmutableList<KeyStroke>, Integer> myPrefixes = new HashMap<ImmutableList<KeyStroke>, Integer>();

  @NotNull
  @Override
  public Iterator<List<KeyStroke>> iterator() {
    return new ArrayList<List<KeyStroke>>(myKeys.keySet()).iterator();
  }

  @Nullable
  public MappingInfo get(@NotNull List<KeyStroke> keys) {
    return myKeys.get(ImmutableList.copyOf(keys));
  }

  public void put(@NotNull Set<MappingMode> mappingModes, @NotNull List<KeyStroke> fromKeys,
                  @Nullable List<KeyStroke> toKeys, @Nullable VimExtensionHandler extensionHandler, boolean recursive) {
    myKeys.put(ImmutableList.copyOf(fromKeys),
               new MappingInfo(mappingModes, fromKeys, toKeys, extensionHandler, recursive));
    List<KeyStroke> prefix = new ArrayList<KeyStroke>();
    final int prefixLength = fromKeys.size() - 1;
    for (int i = 0; i < prefixLength; i++) {
      prefix.add(fromKeys.get(i));
      increment(ImmutableList.copyOf(prefix));
    }
  }

  public void delete(@NotNull List<KeyStroke> keys) {
    myKeys.remove(ImmutableList.copyOf(keys));
    List<KeyStroke> prefix = new ArrayList<KeyStroke>();
    final int prefixLength = keys.size() - 1;
    for (int i = 0; i < prefixLength; i++) {
      prefix.add(keys.get(i));
      decrement(ImmutableList.copyOf(prefix));
    }
  }

  public boolean isPrefix(@NotNull List<KeyStroke> keys) {
    return myPrefixes.get(ImmutableList.copyOf(keys)) != null;
  }

  private void increment(@NotNull ImmutableList<KeyStroke> prefix) {
    Integer count = myPrefixes.get(prefix);
    if (count == null) {
      count = 0;
    }
    myPrefixes.put(prefix, count + 1);
  }

  private void decrement(@NotNull ImmutableList<KeyStroke> prefix) {
    final Integer count = myPrefixes.get(prefix);
    if (count != null) {
      if (count <= 1) {
        myPrefixes.remove(prefix);
      }
      else {
        myPrefixes.put(prefix, count - 1);
      }
    }
  }
}
