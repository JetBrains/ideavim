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

package com.maddyhome.idea.vim.key;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.helper.StringHelper;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Container for key mappings for some mode
 * Iterable by "from" keys
 *
 * @author vlan
 */
public class KeyMapping implements Iterable<List<KeyStroke>> {
  /**
   * Contains all key mapping for some mode.
   */
  private final @NotNull Map<List<KeyStroke>, MappingInfo> myKeys = new HashMap<>();
  /**
   * Set the contains all possible prefixes for mappings.
   * E.g. if there is mapping for "hello", this set will contain "h", "he", "hel", etc.
   * Multiset is used to correctly remove the mappings.
   */
  private final @NotNull Multiset<List<KeyStroke>> myPrefixes = HashMultiset.create();

  @Override
  public @NotNull Iterator<List<KeyStroke>> iterator() {
    return new ArrayList<>(myKeys.keySet()).iterator();
  }

  public @Nullable MappingInfo get(@NotNull Iterable<KeyStroke> keys) {
    // Having a parameter of Iterable allows for a nicer API, because we know when a given list is immutable.
    // TODO: Should we change this to be a trie?
    assert (keys instanceof List) : "keys must be of type List<KeyStroke>";

    List<KeyStroke> keyStrokes = (List<KeyStroke>)keys;

    MappingInfo mappingInfo = myKeys.get(keys);
    if (mappingInfo != null) return mappingInfo;

    if (keyStrokes.size() > 3) {
      if (keyStrokes.get(0).getKeyCode() == StringHelper.VK_ACTION &&
          keyStrokes.get(1).getKeyChar() == '(' &&
          keyStrokes.get(keyStrokes.size() - 1).getKeyChar() == ')') {
        StringBuilder builder = new StringBuilder();
        for (int i = 2; i < keyStrokes.size() - 1; i++) {
          builder.append(keyStrokes.get(i).getKeyChar());
        }
        return new ToActionMappingInfo(builder.toString(), keyStrokes, false, MappingOwner.IdeaVim.INSTANCE);
      }
    }

    return null;
  }

  public void put(@NotNull List<KeyStroke> fromKeys,
                  @NotNull MappingOwner owner,
                  @NotNull VimExtensionHandler extensionHandler,
                  boolean recursive) {
    myKeys.put(new ArrayList<>(fromKeys), new ToHandlerMappingInfo(extensionHandler, fromKeys, recursive, owner));
    fillPrefixes(fromKeys);
  }

  public void put(@NotNull List<KeyStroke> fromKeys,
                  @NotNull List<KeyStroke> toKeys,
                  @NotNull MappingOwner owner,
                  boolean recursive) {
    myKeys.put(new ArrayList<>(fromKeys), new ToKeysMappingInfo(toKeys, fromKeys, recursive, owner));
    fillPrefixes(fromKeys);
  }

  private void fillPrefixes(@NotNull List<KeyStroke> fromKeys) {
    List<KeyStroke> prefix = new ArrayList<>();
    final int prefixLength = fromKeys.size() - 1;
    for (int i = 0; i < prefixLength; i++) {
      prefix.add(fromKeys.get(i));
      myPrefixes.add(new ArrayList<>(prefix));
    }
  }

  public void delete(@NotNull MappingOwner owner) {
    List<Map.Entry<List<KeyStroke>, MappingInfo>> toRemove =
      myKeys.entrySet().stream().filter(o -> o.getValue().getOwner().equals(owner)).collect(Collectors.toList());

    toRemove.forEach(o -> myKeys.remove(o.getKey(), o.getValue()));
    toRemove.stream().map(Map.Entry::getKey).forEach(this::removePrefixes);
  }

  public void delete(@NotNull List<KeyStroke> keys) {
    MappingInfo removed = myKeys.remove(keys);
    if (removed == null) return;

    removePrefixes(keys);
  }

  public void delete() {
    myKeys.clear();
    myPrefixes.clear();
  }

  private void removePrefixes(@NotNull List<KeyStroke> keys) {
    List<KeyStroke> prefix = new ArrayList<>();
    final int prefixLength = keys.size() - 1;
    for (int i = 0; i < prefixLength; i++) {
      prefix.add(keys.get(i));
      myPrefixes.remove(prefix);
    }
  }

  public List<Pair<List<KeyStroke>, MappingInfo>> getByOwner(@NotNull MappingOwner owner) {
    return myKeys.entrySet().stream().filter(o -> o.getValue().getOwner().equals(owner))
      .map(o -> new Pair<>(o.getKey(), o.getValue())).collect(Collectors.toList());
  }

  public boolean isPrefix(@NotNull Iterable<KeyStroke> keys) {
    // Having a parameter of Iterable allows for a nicer API, because we know when a given list is immutable.
    // Perhaps we should look at changing this to a trie or something?
    assert (keys instanceof List) : "keys must be of type List<KeyStroke>";

    List<KeyStroke> keyList = (List<KeyStroke>)keys;
    if (keyList.isEmpty()) return false;

    if (myPrefixes.contains(keys)) return true;

    int firstChar = keyList.get(0).getKeyCode();
    char lastChar = keyList.get(keyList.size() - 1).getKeyChar();
    return firstChar == StringHelper.VK_ACTION && lastChar != ')';
  }
}
