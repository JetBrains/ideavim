/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimCaret;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.VimLockLabel;
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.options.OptionAccessScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;

public class WindowGroup extends WindowGroupBase {
  @Override
  public void closeCurrentWindow(@NotNull ExecutionContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager((DataContext)context.getContext());
    final EditorWindow window = fileEditorManager.getSplitters().getCurrentWindow();
    if (window != null) {
      window.closeAllExcept(null);
    }
  }

  @Override
  public void closeAllExceptCurrent(@NotNull ExecutionContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext)context.getContext()));
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    for (final EditorWindow window : fileEditorManager.getWindows()) {
      if (window != current) {
        window.closeAllExcept(null);
      }
    }
  }

  public void closeAll(@NotNull ExecutionContext context) {
    getFileEditorManager(((IjEditorExecutionContext)context).getContext()).closeAllFiles();
  }

  @Override
  public void selectNextWindow(@NotNull ExecutionContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext)context.getContext()));
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    if (current != null) {
      EditorWindow nextWindow = fileEditorManager.getNextWindow(current);
      if (nextWindow != null) {
        nextWindow.setAsCurrentWindow(true);
      }
    }
  }

  @Override
  public void selectPreviousWindow(@NotNull ExecutionContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext)context.getContext()));
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    if (current != null) {
      EditorWindow prevWindow = fileEditorManager.getPrevWindow(current);
      if (prevWindow != null) {
        prevWindow.setAsCurrentWindow(true);
      }
    }
  }

  @Override
  public void selectWindow(@NotNull ExecutionContext context, int index) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext)context.getContext()));
    final EditorWindow[] windows = fileEditorManager.getWindows();
    if (index - 1 < windows.length) {
      windows[index - 1].setAsCurrentWindow(true);
    }
  }

  @Override
  public void splitWindowHorizontal(@NotNull ExecutionContext context, @NotNull String filename) {
    splitWindow(SwingConstants.HORIZONTAL, (DataContext)context.getContext(), filename);
  }

  @Override
  public void splitWindowVertical(@NotNull ExecutionContext context, @NotNull String filename) {
    splitWindow(SwingConstants.VERTICAL, (DataContext)context.getContext(), filename);
  }

  private static @NotNull List<EditorWindow> findWindowsInRow(@NotNull Caret caret,
                                                              @NotNull List<EditorWindow> windows,
                                                              final boolean vertical) {
    var anchorPoint = getCaretPoint(caret);
    var result = new ArrayList<EditorWindow>();
    var coord = vertical ? anchorPoint.getX() : anchorPoint.getY();
    for (var window : windows) {
      var rect = getSplitRectangle(window);
      if (rect != null) {
        var min = vertical ? rect.getX() : rect.getY();
        var max = min + (vertical ? rect.getWidth() : rect.getHeight());
        if (coord >= min && coord <= max) {
          result.add(window);
        }
      }
    }
    result.sort((window1, window2) -> {
      var rect1 = getSplitRectangle(window1);
      var rect2 = getSplitRectangle(window2);
      if (rect1 != null && rect2 != null) {
        var diff = vertical ? (rect1.getY() - rect2.getY()) : (rect1.getX() - rect2.getX());
        return diff < 0 ? -1 : diff > 0 ? 1 : 0;
      }
      return 0;
    });
    return result;
  }

  private static boolean isVimEverywhereEnabled() {
    var option = injector.getOptionGroup().getOption("VimEverywhere");
    if (option == null) return false;
    var value = injector.getOptionGroup().getOptionValue(option, new OptionAccessScope.GLOBAL(null));
    return value.toVimNumber().getBooleanValue();
  }

  private static List<NavTarget> collectNavigableWindows(@NotNull Project project) {
    var editorTargets = collectEditorSplits(project);
    var toolWindowTargets = collectDockedToolWindows(project);

    var targets = new ArrayList<NavTarget>(editorTargets.size() + toolWindowTargets.size());
    targets.addAll(editorTargets);
    targets.addAll(toolWindowTargets);
    return targets;
  }

  private static List<NavTarget> collectEditorSplits(@NotNull Project project) {
    var fem = FileEditorManagerEx.getInstanceEx(project);
    var targets = new ArrayList<NavTarget>();
    for (var w : fem.getWindows()) {
      var rect = getSplitRectangle(w);
      if (rect != null) {
        targets.add(new NavTarget(rect, () -> w.setAsCurrentWindow(true)));
      }
    }
    return targets;
  }

  private static List<NavTarget> collectDockedToolWindows(@NotNull Project project) {
    var twm = ToolWindowManagerEx.getInstanceEx(project);
    var targets = new ArrayList<NavTarget>();
    for (var id : twm.getToolWindowIds()) {
      var tw = twm.getToolWindow(id);
      if (tw == null || !tw.isVisible()) continue;
      if (tw.getType() == ToolWindowType.FLOATING || tw.getType() == ToolWindowType.WINDOWED) continue;
      var comp = tw.getComponent();
      if (!comp.isShowing()) continue;
      try {
        var loc = comp.getLocationOnScreen();
        var size = comp.getSize();
        targets.add(new NavTarget(new Rectangle(loc, size), () -> tw.activate(null)));
      }
      catch (IllegalComponentStateException ignored) {
      }
    }
    return targets;
  }

  /**
   * Navigates to the nearest window in the given direction from the current position.
   * Works uniformly for editor splits and tool windows — both are treated as screen rectangles.
   *
   * @param referencePoint   screen point to measure from (e.g. caret position or tool window center)
   * @param currentBounds    screen bounds of the currently focused window
   * @param relativePosition positive = right/down, negative = left/up
   * @param vertical         true for up/down movement, false for left/right
   * @return true if navigation occurred
   */
  public static boolean navigateInDirection(@NotNull Project project,
                                            @NotNull Point referencePoint,
                                            @NotNull Rectangle currentBounds,
                                            int relativePosition,
                                            boolean vertical) {
    var targets = collectNavigableWindows(project);

    var row = getNavTargets(referencePoint, vertical, targets);

    var currentIdx = getCurrentIdx(currentBounds, row);
    if (currentIdx == null) return false;

    var targetIdx = Math.max(0, Math.min(currentIdx + relativePosition, row.size() - 1));
    if (targetIdx == currentIdx) return false;

    row.get(targetIdx).activate().run();
    return true;
  }

  private static @Nullable Integer getCurrentIdx(@NotNull Rectangle currentBounds, ArrayList<NavTarget> row) {
    for (int i = 0; i < row.size(); i++) {
      if (row.get(i).bounds().intersects(currentBounds)) {
        return i;
      }
    }
    return null;
  }

  private static @NotNull ArrayList<NavTarget> getNavTargets(@NotNull Point referencePoint,
                                                             boolean vertical,
                                                             List<NavTarget> targets) {
    var cord = vertical ? referencePoint.getX() : referencePoint.getY();
    var row = new ArrayList<NavTarget>();
    for (var target : targets) {
      var rect = target.bounds();
      var min = vertical ? rect.getX() : rect.getY();
      var max = min + (vertical ? rect.getWidth() : rect.getHeight());
      if (cord >= min && cord <= max) {
        row.add(target);
      }
    }

    // Sort by position in movement direction
    row.sort((a, b) -> {
      var posA = vertical ? a.bounds().getY() : a.bounds().getX();
      var posB = vertical ? b.bounds().getY() : b.bounds().getX();
      return Double.compare(posA, posB);
    });
    return row;
  }

  @Override
  @VimLockLabel.RequiresReadLock
  @RequiresReadLock
  public void selectWindowInRow(@NotNull VimCaret caret,
                                @NotNull ExecutionContext context,
                                int relativePosition,
                                boolean vertical) {
    var dataContext = (DataContext)context.getContext();
    var ijCaret = ((IjVimCaret)caret).getCaret();
    var fileEditorManager = getFileEditorManager(dataContext);
    var currentWindow = fileEditorManager.getCurrentWindow();
    if (currentWindow == null) return;

    if (isVimEverywhereEnabled()) {
      var project = PlatformDataKeys.PROJECT.getData(dataContext);
      if (project == null) return;
      var currentBounds = getSplitRectangle(currentWindow);
      if (currentBounds == null) return;
      var refPoint = getCaretPoint(ijCaret);
      navigateInDirection(project, refPoint, currentBounds, relativePosition, vertical);
      return;
    }

    var windows = fileEditorManager.getWindows();
    var row = findWindowsInRow(ijCaret, Arrays.asList(windows), vertical);
    selectWindow(currentWindow, row, relativePosition);
  }

  private void selectWindow(@NotNull EditorWindow currentWindow,
                            @NotNull List<EditorWindow> windows,
                            int relativePosition) {
    var pos = windows.indexOf(currentWindow);
    var selected = pos + relativePosition;
    var normalized = Math.max(0, Math.min(selected, windows.size() - 1));
    windows.get(normalized).setAsCurrentWindow(true);
  }

  private record NavTarget(Rectangle bounds, Runnable activate) {
  }

  private static @NotNull FileEditorManagerEx getFileEditorManager(@NotNull DataContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    return FileEditorManagerEx.getInstanceEx(Objects.requireNonNull(project));
  }

  private void splitWindow(int orientation, @NotNull DataContext context, @NotNull String filename) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    if (project == null) return;
    final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);

    VirtualFile virtualFile = null;
    if (!filename.isEmpty()) {
      virtualFile = VimPlugin.getFile().findFile(filename, project);
      if (virtualFile == null) {
        // Vim doesn't have this error message. It will create a split with a new file, if there's not one to load
        VimPlugin.showMessage(MessageHelper.message("error.split.window.could.not.find.file.0", filename));
        return;
      }
    }

    final EditorWindow editorWindow = fileEditorManager.getSplitters().getCurrentWindow();
    if (editorWindow != null) {
      editorWindow.split(orientation, true, virtualFile, true);
    }
  }

  private static @NotNull Point getCaretPoint(@NotNull Caret caret) {
    final Editor editor = caret.getEditor();
    final Point caretLocation = editor.logicalPositionToXY(caret.getLogicalPosition());
    Point caretScreenLocation = editor.getContentComponent().getLocationOnScreen();
    caretScreenLocation.translate(caretLocation.x, caretLocation.y);
    return caretScreenLocation;
  }

  private static @Nullable Rectangle getSplitRectangle(@NotNull EditorWindow window) {
    final EditorComposite editorComposite = window.getSelectedComposite();
    if (editorComposite != null) {
      final EditorTabbedContainer split = window.getTabbedPane();
      final Point point = split.getComponent().getLocationOnScreen();
      final Dimension dimension = split.getComponent().getSize();
      return new Rectangle(point, dimension);
    }
    return null;
  }
}
