/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.state.mode.Mode;
import com.maddyhome.idea.vim.state.VimStateMachine;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.EditorHelperRt;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.SearchHelper;
import com.maddyhome.idea.vim.newapi.ExecuteExtensionKt;
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.newapi.IjVimInjectorKt.globalIjOptions;

public class FileGroup extends VimFileBase {
  public boolean openFile(@NotNull String filename, @NotNull ExecutionContext context) {
    if (logger.isDebugEnabled()) {
      logger.debug("openFile(" + filename + ")");
    }
    final Project project = PlatformDataKeys.PROJECT.getData(((IjEditorExecutionContext) context).getContext()); // API change - don't merge
    if (project == null) return false;

    VirtualFile found = findFile(filename, project);

    if (found != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("found file: " + found);
      }
      // Can't open a file unless it has a known file type. The next call will return the known type.
      // If unknown, IDEA will prompt the user to pick a type.
      FileType type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(found, project);

      //noinspection IfStatementWithIdenticalBranches
      if (type != null) {
        FileEditorManager fem = FileEditorManager.getInstance(project);
        fem.openFile(found, true);

        return true;
      } else {
        // There was no type and user didn't pick one. Don't open the file
        // Return true here because we found the file but the user canceled by not picking a type.
        return true;
      }
    } else {
      VimPlugin.showMessage(MessageHelper.message("unable.to.find.0", filename));

      return false;
    }
  }

  @Nullable
  VirtualFile findFile(@NotNull String filename, @NotNull Project project) {
    VirtualFile found;
    if (filename.length() > 2 && filename.charAt(0) == '~' && filename.charAt(1) == File.separatorChar) {
      String homefile = filename.substring(2);
      String dir = System.getProperty("user.home");
      if (logger.isDebugEnabled()) {
        logger.debug("home dir file");
        logger.debug("looking for " + homefile + " in " + dir);
      }
      found = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(dir, homefile));
    } else {
      found = LocalFileSystem.getInstance().findFileByIoFile(new File(filename));

      if (found == null) {
        found = findByNameInContentRoots(filename, project);
        if (found == null) {
          found = findByNameInProject(filename, project);
        }
      }
    }

    return found;
  }

  @Nullable
  private VirtualFile findByNameInContentRoots(@NotNull String filename, @NotNull Project project) {
    VirtualFile found = null;
    ProjectRootManager prm = ProjectRootManager.getInstance(project);
    VirtualFile[] roots = prm.getContentRoots();
    for (int i = 0; i < roots.length; i++) {
      if (logger.isDebugEnabled()) {
        logger.debug("root[" + i + "] = " + roots[i].getPath());
      }
      found = roots[i].findFileByRelativePath(filename);
      if (found != null) {
        break;
      }
    }
    return found;
  }

  @Nullable
  private static VirtualFile findByNameInProject(@NotNull String filename, @NotNull Project project) {
    GlobalSearchScope projectScope = ProjectScope.getProjectScope(project);
    Collection<VirtualFile> names = FilenameIndex.getVirtualFilesByName(filename, projectScope);
    if (!names.isEmpty()) {
      return names.stream().findFirst().get();
    }
    return null;
  }

  /**
   * Closes the current editor.
   */
  @Override
  public void closeFile(@NotNull VimEditor editor, @NotNull ExecutionContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(((DataContext) context.getContext()));
    if (project != null) {
      final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
      final EditorWindow window = fileEditorManager.getCurrentWindow();
      final VirtualFile virtualFile = fileEditorManager.getCurrentFile();

      if (virtualFile != null && window != null) {
        // During the work on VIM-2912 I've changed the close function to this one.
        //   However, the function with manager seems to work weirdly and it causes VIM-2953
        //window.getManager().closeFile(virtualFile, true, false);
        window.closeFile(virtualFile);

        // Get focus after closing tab
        window.requestFocus(true);
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
          // This thing doesn't have an implementation in test mode
          EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project);
        }
      }
    }
  }

  /**
   * Closes editor.
   */
  @Override
  public void closeFile(int number, @NotNull ExecutionContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(((IjEditorExecutionContext) context).getContext());
    if (project == null) return;
    final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
    final EditorWindow window = fileEditorManager.getCurrentWindow();
    VirtualFile[] editors = fileEditorManager.getOpenFiles();
    if (window != null) {
      if (number >= 0 && number < editors.length) {
        fileEditorManager.closeFile(editors[number], window);
      }
    }
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      // This thing doesn't have an implementation in test mode
      EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project);
    }
  }

  /**
   * Saves specific file in the project.
   */
  @Override
  public void saveFile(@NotNull ExecutionContext context) {
    NativeAction action;
    if (globalIjOptions(injector).getIdeawrite().contains(IjOptionConstants.ideawrite_all)) {
      action = injector.getNativeActionManager().getSaveAll();
    } else {
      action = injector.getNativeActionManager().getSaveCurrent();
    }
    ExecuteExtensionKt.execute(action, context);
  }

  /**
   * Saves all files in the project.
   */
  public void saveFiles(@NotNull ExecutionContext context) {
    ExecuteExtensionKt.execute(VimInjectorKt.getInjector().getNativeActionManager().getSaveAll(), context);
  }

  /**
   * Selects then next or previous editor.
   */
  @Override
  public boolean selectFile(int count, @NotNull ExecutionContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(((IjEditorExecutionContext) context).getContext());
    if (project == null) return false;
    FileEditorManager fem = FileEditorManager.getInstance(project); // API change - don't merge
    VirtualFile[] editors = fem.getOpenFiles();
    if (count == 99) {
      count = editors.length - 1;
    }
    if (count < 0 || count >= editors.length) {
      return false;
    }

    fem.openFile(editors[count], true);

    return true;
  }

  /**
   * Selects then next or previous editor.
   */
  public void selectNextFile(int count, @NotNull ExecutionContext context) {
    Project project = PlatformDataKeys.PROJECT.getData(((IjEditorExecutionContext) context).getContext());
    if (project == null) return;
    FileEditorManager fem = FileEditorManager.getInstance(project); // API change - don't merge
    VirtualFile[] editors = fem.getOpenFiles();
    VirtualFile current = fem.getSelectedFiles()[0];
    for (int i = 0; i < editors.length; i++) {
      if (editors[i].equals(current)) {
        int pos = (i + (count % editors.length) + editors.length) % editors.length;

        fem.openFile(editors[pos], true);
      }
    }
  }

  /**
   * Selects previous editor tab.
   */
  @Override
  public void selectPreviousTab(@NotNull ExecutionContext context) {
    Project project = PlatformDataKeys.PROJECT.getData(((DataContext) context.getContext()));
    if (project == null) return;
    VirtualFile vf = LastTabService.getInstance(project).getLastTab();
    if (vf != null && vf.isValid()) {
      FileEditorManager.getInstance(project).openFile(vf, true);
    } else {
      VimPlugin.indicateError();
    }
  }

  /**
   * Returns the previous tab.
   */
  public @Nullable
  VirtualFile getPreviousTab(@NotNull DataContext context) {
    Project project = PlatformDataKeys.PROJECT.getData(context);
    if (project == null) return null;
    VirtualFile vf = LastTabService.getInstance(project).getLastTab();
    if (vf != null && vf.isValid()) {
      return vf;
    }
    return null;
  }

  @Nullable
  Editor selectEditor(Project project, @NotNull VirtualFile file) {
    FileEditorManager fMgr = FileEditorManager.getInstance(project);
    FileEditor[] feditors = fMgr.openFile(file, true);
    if (feditors.length > 0) {
      if (feditors[0] instanceof TextEditor) {
        Editor editor = ((TextEditor) feditors[0]).getEditor();
        if (!editor.isDisposed()) {
          return editor;
        }
      }
    }

    return null;
  }

  @Override
  public void displayLocationInfo(@NotNull VimEditor vimEditor) {
    Editor editor = ((IjVimEditor) vimEditor).getEditor();
    StringBuilder msg = new StringBuilder();
    Document doc = editor.getDocument();

    if (!(VimStateMachine.Companion.getInstance(new IjVimEditor(editor)).getMode() instanceof Mode.VISUAL)) {
      LogicalPosition lp = editor.getCaretModel().getLogicalPosition();
      int col = editor.getCaretModel().getOffset() - doc.getLineStartOffset(lp.line);
      int endoff = doc.getLineEndOffset(lp.line);
      if (endoff < EditorHelperRt.getFileSize(editor) && doc.getCharsSequence().charAt(endoff) == '\n') {
        endoff--;
      }
      int ecol = endoff - doc.getLineStartOffset(lp.line);
      LogicalPosition elp = editor.offsetToLogicalPosition(endoff);

      msg.append("Col ").append(col + 1);
      if (col != lp.column) {
        msg.append("-").append(lp.column + 1);
      }

      msg.append(" of ").append(ecol + 1);
      if (ecol != elp.column) {
        msg.append("-").append(elp.column + 1);
      }

      int lline = editor.getCaretModel().getLogicalPosition().line;
      int total = new IjVimEditor(editor).lineCount();

      msg.append("; Line ").append(lline + 1).append(" of ").append(total);

      SearchHelper.CountPosition cp = SearchHelper.countWords(editor);

      msg.append("; Word ").append(cp.getPosition()).append(" of ").append(cp.getCount());

      int offset = editor.getCaretModel().getOffset();
      int size = EditorHelperRt.getFileSize(editor);

      msg.append("; Character ").append(offset + 1).append(" of ").append(size);
    } else {
      msg.append("Selected ");

      TextRange vr = new TextRange(editor.getSelectionModel().getBlockSelectionStarts(),
        editor.getSelectionModel().getBlockSelectionEnds());
      vr.normalize();

      int lines;
      SearchHelper.CountPosition cp = SearchHelper.countWords(editor);
      int words = cp.getCount();
      int word = 0;
      if (vr.isMultiple()) {
        lines = vr.size();
        int cols = vr.getMaxLength();

        msg.append(cols).append(" Cols; ");

        for (int i = 0; i < vr.size(); i++) {
          cp = SearchHelper.countWords(editor, vr.getStartOffsets()[i], vr.getEndOffsets()[i] - 1);
          word += cp.getCount();
        }
      } else {
        LogicalPosition slp = editor.offsetToLogicalPosition(vr.getStartOffset());
        LogicalPosition elp = editor.offsetToLogicalPosition(vr.getEndOffset());

        lines = elp.line - slp.line + 1;

        cp = SearchHelper.countWords(editor, vr.getStartOffset(), vr.getEndOffset() - 1);
        word = cp.getCount();
      }

      int total = new IjVimEditor(editor).lineCount();

      msg.append(lines).append(" of ").append(total).append(" Lines");

      msg.append("; ").append(word).append(" of ").append(words).append(" Words");

      int chars = vr.getSelectionCount();
      int size = EditorHelperRt.getFileSize(editor);

      msg.append("; ").append(chars).append(" of ").append(size).append(" Characters");
    }

    VimPlugin.showMessage(msg.toString());
  }

  @Override
  public void displayFileInfo(@NotNull VimEditor vimEditor, boolean fullPath) {
    Editor editor = ((IjVimEditor) vimEditor).getEditor();
    StringBuilder msg = new StringBuilder();
    VirtualFile vf = EditorHelper.getVirtualFile(editor);
    if (vf != null) {
      msg.append('"');
      if (fullPath) {
        msg.append(vf.getPath());
      } else {
        Project project = editor.getProject();
        if (project != null) {
          VirtualFile root = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(vf);
          if (root != null) {
            msg.append(vf.getPath().substring(root.getPath().length() + 1));
          } else {
            msg.append(vf.getPath());
          }
        }
      }
      msg.append("\" ");
    } else {
      msg.append("\"[No File]\" ");
    }

    Document doc = editor.getDocument();
    if (!doc.isWritable()) {
      msg.append("[RO] ");
    } else if (FileDocumentManager.getInstance().isDocumentUnsaved(doc)) {
      msg.append("[+] ");
    }

    int lline = editor.getCaretModel().getLogicalPosition().line;
    int total = new IjVimEditor(editor).lineCount();
    int pct = (int) ((float) lline / (float) total * 100f + 0.5);

    msg.append("line ").append(lline + 1).append(" of ").append(total);
    msg.append(" --").append(pct).append("%-- ");

    LogicalPosition lp = editor.getCaretModel().getLogicalPosition();
    int col = editor.getCaretModel().getOffset() - doc.getLineStartOffset(lline);

    msg.append("col ").append(col + 1);
    if (col != lp.column) {
      msg.append("-").append(lp.column + 1);
    }

    VimPlugin.showMessage(msg.toString());
  }

  private static final @NotNull
  Logger logger = Logger.getInstance(FileGroup.class.getName());

  /**
   * This method listens for editor tab changes so any insert/replace modes that need to be reset can be.
   */
  public static void fileEditorManagerSelectionChangedCallback(@NotNull FileEditorManagerEvent event) {
    // The user has changed the editor they are working with - exit insert/replace mode, and complete any
    // appropriate repeat
    if (event.getOldFile() != null) {
      LastTabService.getInstance(event.getManager().getProject()).setLastTab(event.getOldFile());
    }
  }
}

