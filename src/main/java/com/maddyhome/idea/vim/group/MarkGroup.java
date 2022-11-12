/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.ide.bookmark.LineBookmark;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.mark.IntellijMark;
import com.maddyhome.idea.vim.mark.Jump;
import com.maddyhome.idea.vim.mark.Mark;
import com.maddyhome.idea.vim.mark.VimMarkGroup;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2.3")
public class MarkGroup implements VimMarkGroup {
  @Override
  public void saveJumpLocation(@NotNull VimEditor editor) {
    VimInjectorKt.injector.getJumpService().saveJumpLocation(editor);
  }

  @Override
  public void setChangeMarks(@NotNull VimEditor vimEditor, @NotNull TextRange range) {
    VimInjectorKt.injector.getMarkService().setChangeMarks(vimEditor.primaryCaret(), range);
  }

  @Override
  public void addJump(@NotNull VimEditor editor, boolean reset) {
    VimInjectorKt.injector.getJumpService().addJump(editor, reset);
  }

  @Nullable
  @Override
  public Mark getMark(@NotNull VimEditor editor, char ch) {
    return VimInjectorKt.injector.getMarkService().getMark(editor.primaryCaret(), ch);
  }

  @Nullable
  @Override
  public Jump getJump(int count) {
    return VimInjectorKt.injector.getJumpService().getJump(count);
  }

  @Nullable
  @Override
  public Mark createSystemMark(char ch, int line, int col, @NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    @Nullable LineBookmark systemMark = SystemMarks.createOrGetSystemMark(ch, line, ijEditor);
    if (systemMark == null) {
      return null;
    }
    return new IntellijMark(systemMark, col, ijEditor.getProject());
  }

  @Override
  public boolean setMark(@NotNull VimEditor editor, char ch, int offset) {
    return VimInjectorKt.injector.getMarkService().setMark(editor.primaryCaret(), ch, offset);
  }

  @Override
  public boolean setMark(@NotNull VimEditor editor, char ch) {
    return VimInjectorKt.injector.getMarkService().setMark(editor, ch);
  }

  @Override
  public void includeCurrentCommandAsNavigation(@NotNull VimEditor editor) {
    VimInjectorKt.injector.getJumpService().includeCurrentCommandAsNavigation(editor);
  }

  @Nullable
  @Override
  public Mark getFileMark(@NotNull VimEditor editor, char ch) {
    return VimInjectorKt.injector.getMarkService().getMark(editor.primaryCaret(), ch);
  }

  @Override
  public void setVisualSelectionMarks(@NotNull VimEditor editor, @NotNull TextRange range) {
    VimInjectorKt.injector.getMarkService().setVisualSelectionMarks(editor.primaryCaret(), range);
  }

  @Nullable
  @Override
  public TextRange getChangeMarks(@NotNull VimEditor editor) {
    return VimInjectorKt.injector.getMarkService().getChangeMarks(editor.primaryCaret());
  }

  @Nullable
  @Override
  public TextRange getVisualSelectionMarks(@NotNull VimEditor editor) {
    return VimInjectorKt.injector.getMarkService().getVisualSelectionMarks(editor.primaryCaret());
  }

  @Override
  public void resetAllMarks() {
    VimInjectorKt.injector.getMarkService().resetAllMarks();
  }

  @Override
  public void removeMark(char ch, @NotNull Mark mark) {
    VimInjectorKt.injector.getMarkService().removeMark(ch, mark);
  }

  @NotNull
  @Override
  public List<Mark> getMarks(@NotNull VimEditor editor) {
    Set<Mark> marks = VimInjectorKt.injector.getMarkService().getAllLocalMarks(editor.primaryCaret());
    marks.addAll(VimInjectorKt.injector.getMarkService().getGlobalMarks(editor));
    return new ArrayList<>(marks);
  }

  @NotNull
  @Override
  public List<Jump> getJumps() {
    return VimInjectorKt.injector.getJumpService().getJumps();
  }

  @Override
  public int getJumpSpot() {
    return VimInjectorKt.injector.getJumpService().getJumpSpot();
  }

  @Override
  public void updateMarkFromDelete(@Nullable VimEditor editor,
                                   @Nullable HashMap<Character, Mark> marks,
                                   int delStartOff,
                                   int delLength) {
    VimInjectorKt.injector.getMarkService().updateMarksFromDelete(editor, delStartOff, delLength);
  }

  @Override
  public void updateMarkFromInsert(@Nullable VimEditor editor,
                                   @Nullable HashMap<Character, Mark> marks,
                                   int insStartOff,
                                   int insLength) {
    VimInjectorKt.injector.getMarkService().updateMarksFromInsert(editor, insStartOff, insLength);
  }

  @Override
  public void dropLastJump() {
    VimInjectorKt.injector.getJumpService().dropLastJump();
  }
}
