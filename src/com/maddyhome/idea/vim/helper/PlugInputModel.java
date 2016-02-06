package com.maddyhome.idea.vim.helper;

import com.google.common.collect.Lists;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dhleong
 */
public class PlugInputModel {
  @NotNull private final List<KeyStroke> myKeyStrokes = Lists.newArrayList();

  private PlugInputModel() {}

  public static PlugInputModel getInstance(@NotNull Editor editor) {
    PlugInputModel model = EditorData.getPlugInputModel(editor);
    if (model == null) {
      model = new PlugInputModel();
      EditorData.setPlugInputModel(editor, model);
    }
    return model;
  }

  /**
   * Clear pending strokes state, removing
   *  any that were not consumed
   */
  public List<KeyStroke> removePendingKeyStrokes() {
    List<KeyStroke> unused = new ArrayList<KeyStroke>(myKeyStrokes);
    myKeyStrokes.clear();
    return unused;
  }

  public boolean hasPendingKeyStroke() {
    return !myKeyStrokes.isEmpty();
  }

  public void setPendingKeyStrokes(@NotNull List<KeyStroke> keyStrokes) {
    myKeyStrokes.clear();
    myKeyStrokes.addAll(keyStrokes);
  }

  @Nullable
  public KeyStroke nextKeyStroke() {
    if (!myKeyStrokes.isEmpty()) {
      return myKeyStrokes.remove(0);
    }
    return null;
  }

}
