/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.ide.bookmark.LineBookmark;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.mark.IntellijMark;
import com.maddyhome.idea.vim.mark.Jump;
import com.maddyhome.idea.vim.mark.Mark;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2.3")
public class MarkGroup {
  public List<Jump> jumps = VimInjectorKt.injector.getJumpService().getJumps();

  public void saveJumpLocation(@NotNull Editor editor) {
    VimInjectorKt.injector.getJumpService().saveJumpLocation(new IjVimEditor(editor));
  }

  public void saveJumpLocation(@NotNull VimEditor editor) {
    VimInjectorKt.injector.getJumpService().saveJumpLocation(editor);
  }

  public void setChangeMarks(@NotNull VimEditor vimEditor, @NotNull TextRange range) {
    VimMarkService markService = VimInjectorKt.injector.getMarkService();
    VimMarkServiceKt.setChangeMarks(markService, vimEditor.primaryCaret(), range);
  }

  public void addJump(@NotNull VimEditor editor, boolean reset) {
    VimJumpServiceKt.addJump(VimInjectorKt.injector.getJumpService(), editor, reset);
  }

  @Nullable
  public Mark getMark(@NotNull VimEditor editor, char ch) {
    return VimInjectorKt.injector.getMarkService().getMark(editor.primaryCaret(), ch);
  }

  @Nullable
  public Jump getJump(int count) {
    return VimInjectorKt.injector.getJumpService().getJump(count);
  }

  @Nullable
  public Mark createSystemMark(char ch, int line, int col, @NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor) editor).getEditor();
    @Nullable LineBookmark systemMark = SystemMarks.createOrGetSystemMark(ch, line, ijEditor);
    if (systemMark == null) {
      return null;
    }
    return new IntellijMark(systemMark, col, ijEditor.getProject());
  }

  public boolean setMark(@NotNull VimEditor editor, char ch, int offset) {
    return VimInjectorKt.injector.getMarkService().setMark(editor.primaryCaret(), ch, offset);
  }

  public boolean setMark(@NotNull VimEditor editor, char ch) {
    return VimInjectorKt.injector.getMarkService().setMark(editor, ch);
  }

  public void includeCurrentCommandAsNavigation(@NotNull VimEditor editor) {
    VimInjectorKt.injector.getJumpService().includeCurrentCommandAsNavigation(editor);
  }

  @Nullable
  public Mark getFileMark(@NotNull VimEditor editor, char ch) {
    return VimInjectorKt.injector.getMarkService().getMark(editor.primaryCaret(), ch);
  }

  public void setVisualSelectionMarks(@NotNull VimEditor editor, @NotNull TextRange range) {
    VimMarkService markService = VimInjectorKt.injector.getMarkService();
    VimMarkServiceKt.setVisualSelectionMarks(markService, editor.primaryCaret(), range);
  }

  @Nullable
  public TextRange getChangeMarks(@NotNull VimEditor editor) {
    return VimInjectorKt.injector.getMarkService().getChangeMarks(editor.primaryCaret());
  }

  @Nullable
  public TextRange getVisualSelectionMarks(@NotNull VimEditor editor) {
    return VimInjectorKt.injector.getMarkService().getVisualSelectionMarks(editor.primaryCaret());
  }

  public void resetAllMarks() {
    VimInjectorKt.injector.getMarkService().resetAllMarks();
  }

  public void removeMark(char ch, @NotNull Mark mark) {
    VimInjectorKt.injector.getMarkService().removeMark(ch, mark);
  }

  @NotNull
  public List<Mark> getMarks(@NotNull VimEditor editor) {
    Set<Mark> marks = VimInjectorKt.injector.getMarkService().getAllLocalMarks(editor.primaryCaret());
    marks.addAll(VimInjectorKt.injector.getMarkService().getGlobalMarks(editor));
    return new ArrayList<>(marks);
  }

  public int getJumpSpot() {
    return VimInjectorKt.injector.getJumpService().getJumpSpot();
  }

  public void updateMarkFromDelete(@Nullable VimEditor editor,
                                   @Nullable HashMap<Character, Mark> marks,
                                   int delStartOff,
                                   int delLength) {
    VimInjectorKt.injector.getMarkService().updateMarksFromDelete(editor, delStartOff, delLength);
  }

  public void updateMarkFromInsert(@Nullable VimEditor editor,
                                   @Nullable HashMap<Character, Mark> marks,
                                   int insStartOff,
                                   int insLength) {
    VimInjectorKt.injector.getMarkService().updateMarksFromInsert(editor, insStartOff, insLength);
  }

  public void dropLastJump() {
    VimInjectorKt.injector.getJumpService().dropLastJump();
  }
}
