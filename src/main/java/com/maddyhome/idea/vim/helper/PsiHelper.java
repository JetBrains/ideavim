/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PsiHelper {
  public static int findMethodStart(@NotNull Editor editor, int offset, int count) {
    return findMethodOrClass(editor, offset, count, true);
  }

  public static int findMethodEnd(@NotNull Editor editor, int offset, int count) {
    return findMethodOrClass(editor, offset, count, false);
  }

  private static int findMethodOrClass(@NotNull Editor editor, int offset, int count, boolean isStart) {
    PsiFile file = getFile(editor);

    if (file == null) {
      return -1;
    }
    StructureViewBuilder structureViewBuilder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file);
    if (!(structureViewBuilder instanceof TreeBasedStructureViewBuilder builder)) return -1;
    StructureViewModel model = builder.createStructureViewModel(editor);

    List<Integer> navigationOffsets = new ArrayList<>();
    addNavigationElements(model.getRoot(), navigationOffsets, isStart);

    if (navigationOffsets.isEmpty()) {
      return -1;
    }

    ContainerUtil.sort(navigationOffsets);

    int index = navigationOffsets.size();
    for (int i = 0; i < navigationOffsets.size(); i++) {
      if (navigationOffsets.get(i) > offset) {
        index = i;
        if (count > 0) count--;
        break;
      } else if (navigationOffsets.get(i) == offset) {
        index = i;
        break;
      }
    }
    int resultIndex = index + count;
    if (resultIndex < 0) {
      resultIndex = 0;
    } else if (resultIndex >= navigationOffsets.size()) {
      resultIndex = navigationOffsets.size() - 1;
    }

    return navigationOffsets.get(resultIndex);
  }

  private static void addNavigationElements(@NotNull TreeElement root,
                                            @NotNull List<Integer> navigationOffsets,
                                            boolean start) {
    if (root instanceof PsiTreeElementBase) {
      PsiElement element = ((PsiTreeElementBase<?>) root).getValue();
      int offset;
      if (start) {
        offset = element.getTextRange().getStartOffset();
        if (element.getLanguage().getID().equals("JAVA")) {
          // HACK: for Java classes and methods, we want to jump to the opening brace
          int textOffset = element.getTextOffset();
          // TODO: Try to get rid of `getText()` because it takes a lot of time to calculate the string
          int braceIndex = element.getText().indexOf('{', textOffset - offset);
          if (braceIndex >= 0) {
            offset += braceIndex;
          }
        }
      } else {
        offset = element.getTextRange().getEndOffset() - 1;
      }
      if (!navigationOffsets.contains(offset)) {
        navigationOffsets.add(offset);
      }
    }
    for (TreeElement child : root.getChildren()) {
      addNavigationElements(child, navigationOffsets, start);
    }
  }

  public static @Nullable
  PsiFile getFile(@NotNull Editor editor) {
    VirtualFile vf = EditorHelper.getVirtualFile(editor);
    if (vf != null) {
      Project proj = editor.getProject();
      if (proj != null) {
        PsiManager mgr = PsiManager.getInstance(proj);
        return mgr.findFile(vf);
      }
    }
    return null;
  }
}
