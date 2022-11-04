/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper;

import com.google.common.collect.Lists;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author vlan
 */
public class TestInputModel {
  private final @NotNull List<KeyStroke> myKeyStrokes = Lists.newArrayList();

  private TestInputModel() {
  }

  public static TestInputModel getInstance(@NotNull Editor editor) {
    TestInputModel model = UserDataManager.getVimTestInputModel(editor);
    if (model == null) {
      model = new TestInputModel();
      UserDataManager.setVimTestInputModel(editor, model);
    }
    return model;
  }

  public void setKeyStrokes(@NotNull List<KeyStroke> keyStrokes) {
    myKeyStrokes.clear();
    myKeyStrokes.addAll(keyStrokes);
  }

  public @Nullable KeyStroke nextKeyStroke() {

    // Return key from the unfinished mapping
    /*
    MappingStack mappingStack = KeyHandler.getInstance().getMappingStack();
    if (mappingStack.hasStroke()) {
      return mappingStack.feedStroke();
    }
    */

    if (!myKeyStrokes.isEmpty()) {
      return myKeyStrokes.remove(0);
    }
    return null;
  }
}
