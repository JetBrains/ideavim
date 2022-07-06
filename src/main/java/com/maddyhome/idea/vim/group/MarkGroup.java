/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.group;

import com.intellij.ide.bookmark.BookmarkGroup;
import com.intellij.ide.bookmark.BookmarkType;
import com.intellij.ide.bookmark.BookmarksManager;
import com.intellij.ide.bookmark.LineBookmark;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.HelperKt;
import com.maddyhome.idea.vim.mark.*;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.OptionScope;
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.maddyhome.idea.vim.mark.VimMarkConstants.GLOBAL_MARKS;
import static com.maddyhome.idea.vim.mark.VimMarkConstants.SAVE_FILE_MARKS;

/**
 * This class contains all the mark related functionality
 */
@State(name = "VimMarksSettings", storages = {
  @Storage(value = "$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)})
public class MarkGroup extends VimMarkGroupBase implements PersistentStateComponent<Element> {
  public void editorReleased(@NotNull EditorFactoryEvent event) {
    // Save off the last caret position of the file before it is closed
    Editor editor = event.getEditor();
    setMark(new IjVimEditor(editor), '"', editor.getCaretModel().getOffset());
  }

  @Override
  public void includeCurrentCommandAsNavigation(@NotNull VimEditor editor) {
    Project project = ((IjVimEditor)editor).getEditor().getProject();
    if (project != null) {
      IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
    }
  }

  @Override
  public @Nullable Mark createSystemMark(char ch, int line, int col, @NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    @Nullable LineBookmark systemMark = SystemMarks.createOrGetSystemMark(ch, line, ijEditor);
    if (systemMark == null) {
      return null;
    }
    return new IntellijMark(systemMark, col, ijEditor.getProject());
  }

  /**
   * Gets the map of marks for the specified file
   *
   * @param doc The editor to get the marks for
   * @return The map of marks. The keys are <code>Character</code>s of the mark names, the values are
   * <code>Mark</code>s.
   */
  private @Nullable FileMarks<Character, Mark> getFileMarks(final @NotNull Document doc) {
    VirtualFile vf = FileDocumentManager.getInstance().getFile(doc);
    if (vf == null) {
      return null;
    }

    return getFileMarks(vf.getPath());
  }

  private @Nullable HashMap<Character, Mark> getAllFileMarks(final @NotNull Document doc) {
    VirtualFile vf = FileDocumentManager.getInstance().getFile(doc);
    if (vf == null) {
      return null;
    }

    HashMap<Character, Mark> res = new HashMap<>();
    FileMarks<Character, Mark> fileMarks = getFileMarks(doc);
    if (fileMarks != null) {
      res.putAll(fileMarks);
    }

    for (Character ch : globalMarks.keySet()) {
      Mark mark = globalMarks.get(ch);
      if (vf.getPath().equals(mark.getFilename())) {
        res.put(ch, mark);
      }
    }

    return res;
  }

  public void saveData(@NotNull Element element) {
    Element marksElem = new Element("globalmarks");
    if (!VimPlugin.getOptionService()
      .isSet(OptionScope.GLOBAL.INSTANCE, IjVimOptionService.ideamarksName, IjVimOptionService.ideamarksName)) {
      for (Mark mark : globalMarks.values()) {
        if (!mark.isClear()) {
          Element markElem = new Element("mark");
          markElem.setAttribute("key", Character.toString(mark.getKey()));
          markElem.setAttribute("line", Integer.toString(mark.getLogicalLine()));
          markElem.setAttribute("column", Integer.toString(mark.getCol()));
          markElem.setAttribute("filename", StringUtil.notNullize(mark.getFilename()));
          markElem.setAttribute("protocol", StringUtil.notNullize(mark.getProtocol(), "file"));
          marksElem.addContent(markElem);
          if (logger.isDebugEnabled()) {
            logger.debug("saved mark = " + mark);
          }
        }
      }
    }
    element.addContent(marksElem);

    Element fileMarksElem = new Element("filemarks");

    List<FileMarks<Character, Mark>> files = new ArrayList<>(fileMarks.values());
    files.sort(Comparator.comparing(FileMarks<Character, Mark>::getMyTimestamp));

    if (files.size() > SAVE_MARK_COUNT) {
      files = files.subList(files.size() - SAVE_MARK_COUNT, files.size());
    }

    for (String file : fileMarks.keySet()) {
      FileMarks<Character, Mark> marks = fileMarks.get(file);
      if (!files.contains(marks)) {
        continue;
      }

      if (marks.size() > 0) {
        Element fileMarkElem = new Element("file");
        fileMarkElem.setAttribute("name", file);
        fileMarkElem.setAttribute("timestamp", Long.toString(marks.getMyTimestamp().getTime()));
        for (Mark mark : marks.values()) {
          if (!mark.isClear() && !Character.isUpperCase(mark.getKey()) && SAVE_FILE_MARKS.indexOf(mark.getKey()) >= 0) {
            Element markElem = new Element("mark");
            markElem.setAttribute("key", Character.toString(mark.getKey()));
            markElem.setAttribute("line", Integer.toString(mark.getLogicalLine()));
            markElem.setAttribute("column", Integer.toString(mark.getCol()));
            fileMarkElem.addContent(markElem);
          }
        }
        fileMarksElem.addContent(fileMarkElem);
      }
    }
    element.addContent(fileMarksElem);

    Element jumpsElem = new Element("jumps");
    for (Jump jump : jumps) {
      Element jumpElem = new Element("jump");
      jumpElem.setAttribute("line", Integer.toString(jump.getLogicalLine()));
      jumpElem.setAttribute("column", Integer.toString(jump.getCol()));
      jumpElem.setAttribute("filename", StringUtil.notNullize(jump.getFilepath()));
      jumpsElem.addContent(jumpElem);
      if (logger.isDebugEnabled()) {
        logger.debug("saved jump = " + jump);
      }
    }
    element.addContent(jumpsElem);
  }

  public void readData(@NotNull Element element) {
    // We need to keep the filename for now and create the virtual file later. Any attempt to call
    // LocalFileSystem.getInstance().findFileByPath() results in the following error:
    // Read access is allowed from event dispatch thread or inside read-action only
    // (see com.intellij.openapi.application.Application.runReadAction())

    Element marksElem = element.getChild("globalmarks");
    if (marksElem != null &&
        !VimPlugin.getOptionService()
          .isSet(OptionScope.GLOBAL.INSTANCE, IjVimOptionService.ideamarksName, IjVimOptionService.ideamarksName)) {
      List<Element> markList = marksElem.getChildren("mark");
      for (Element aMarkList : markList) {
        Mark mark = VimMark.create(aMarkList.getAttributeValue("key").charAt(0),
                                   Integer.parseInt(aMarkList.getAttributeValue("line")),
                                   Integer.parseInt(aMarkList.getAttributeValue("column")),
                                   aMarkList.getAttributeValue("filename"), aMarkList.getAttributeValue("protocol"));

        if (mark != null) {
          globalMarks.put(mark.getKey(), mark);
          HashMap<Character, Mark> fmarks = getFileMarks(mark.getFilename());
          fmarks.put(mark.getKey(), mark);
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("globalMarks=" + globalMarks);
    }

    Element fileMarksElem = element.getChild("filemarks");
    if (fileMarksElem != null) {
      List<Element> fileList = fileMarksElem.getChildren("file");
      for (Element aFileList : fileList) {
        String filename = aFileList.getAttributeValue("name");
        Date timestamp = new Date();
        try {
          long date = Long.parseLong(aFileList.getAttributeValue("timestamp"));
          timestamp.setTime(date);
        }
        catch (NumberFormatException e) {
          // ignore
        }
        FileMarks<Character, Mark> fmarks = getFileMarks(filename);
        List<Element> markList = aFileList.getChildren("mark");
        for (Element aMarkList : markList) {
          Mark mark = VimMark.create(aMarkList.getAttributeValue("key").charAt(0),
                                     Integer.parseInt(aMarkList.getAttributeValue("line")),
                                     Integer.parseInt(aMarkList.getAttributeValue("column")), filename,
                                     aMarkList.getAttributeValue("protocol"));

          if (mark != null) fmarks.put(mark.getKey(), mark);
        }
        fmarks.setTimestamp(timestamp);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("fileMarks=" + fileMarks);
    }

    jumps.clear();
    Element jumpsElem = element.getChild("jumps");
    if (jumpsElem != null) {
      List<Element> jumpList = jumpsElem.getChildren("jump");
      for (Element aJumpList : jumpList) {
        Jump jump = new Jump(Integer.parseInt(aJumpList.getAttributeValue("line")),
                             Integer.parseInt(aJumpList.getAttributeValue("column")),
                             aJumpList.getAttributeValue("filename"));

        jumps.add(jump);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("jumps=" + jumps);
    }
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("marks");
    saveData(element);
    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    readData(state);
  }

  /**
   * This class is used to listen to editor document changes
   */
  public static class MarkUpdater implements DocumentListener {

    public static MarkUpdater INSTANCE = new MarkUpdater();

    /**
     * Creates the listener for the supplied editor
     */
    private MarkUpdater() {
    }

    /**
     * This event indicates that a document is about to be changed. We use this event to update all the
     * editor's marks if text is about to be deleted.
     *
     * @param event The change event
     */
    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
      if (!VimPlugin.isEnabled()) return;

      if (logger.isDebugEnabled()) logger.debug("MarkUpdater before, event = " + event);
      if (event.getOldLength() == 0) return;

      Document doc = event.getDocument();
      Editor anEditor = getAnEditor(doc);
      VimInjectorKt.getInjector().getMarkGroup()
        .updateMarkFromDelete(anEditor == null ? null : new IjVimEditor(anEditor),
                              VimPlugin.getMark().getAllFileMarks(doc), event.getOffset(), event.getOldLength());
      // TODO - update jumps
    }

    /**
     * This event indicates that a document was just changed. We use this event to update all the editor's
     * marks if text was just added.
     *
     * @param event The change event
     */
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
      if (!VimPlugin.isEnabled()) return;

      if (logger.isDebugEnabled()) logger.debug("MarkUpdater after, event = " + event);
      if (event.getNewLength() == 0 || (event.getNewLength() == 1 && event.getNewFragment().charAt(0) != '\n')) return;

      Document doc = event.getDocument();
      Editor anEditor = getAnEditor(doc);
      VimInjectorKt.getInjector().getMarkGroup()
        .updateMarkFromInsert(anEditor == null ? null : new IjVimEditor(anEditor),
                              VimPlugin.getMark().getAllFileMarks(doc), event.getOffset(), event.getNewLength());
      // TODO - update jumps
    }

    private @Nullable Editor getAnEditor(@NotNull Document doc) {
      List<Editor> editors = HelperKt.localEditors(doc);

      if (editors.size() > 0) {
        return editors.get(0);
      }
      else {
        return null;
      }
    }
  }

  public static class VimBookmarksListener implements com.intellij.ide.bookmark.BookmarksListener {
    private final Project myProject;

    public VimBookmarksListener(Project project) {
      myProject = project;
    }

    @Override
    public void bookmarkAdded(@NotNull BookmarkGroup group, com.intellij.ide.bookmark.@NotNull Bookmark bookmark) {
      if (!VimPlugin.isEnabled()) return;
      if (!VimPlugin.getOptionService()
        .isSet(OptionScope.GLOBAL.INSTANCE, IjVimOptionService.ideamarksName, IjVimOptionService.ideamarksName)) {
        return;
      }

      if (!(bookmark instanceof LineBookmark)) return;
      BookmarksManager bookmarksManager = BookmarksManager.getInstance(myProject);
      if (bookmarksManager == null) return;
      BookmarkType type = bookmarksManager.getType(bookmark);
      if (type == null) return;

      char mnemonic = type.getMnemonic();
      if (GLOBAL_MARKS.indexOf(mnemonic) == -1) return;

      createVimMark((LineBookmark)bookmark, mnemonic);
    }

    @Override
    public void bookmarkRemoved(@NotNull BookmarkGroup group, com.intellij.ide.bookmark.@NotNull Bookmark bookmark) {
      if (!VimPlugin.isEnabled()) return;
      if (!VimPlugin.getOptionService()
        .isSet(OptionScope.GLOBAL.INSTANCE, IjVimOptionService.ideamarksName, IjVimOptionService.ideamarksName)) {
        return;
      }

      if (!(bookmark instanceof LineBookmark)) return;
      BookmarksManager bookmarksManager = BookmarksManager.getInstance(myProject);
      if (bookmarksManager == null) return;
      BookmarkType type = bookmarksManager.getType(bookmark);
      if (type == null) return;
      char ch = type.getMnemonic();
      if (GLOBAL_MARKS.indexOf(ch) != -1) {
        FileMarks<Character, Mark> fmarks =
          VimPlugin.getMark().getFileMarks(((LineBookmark)bookmark).getFile().getPath());
        fmarks.remove(ch);
        VimPlugin.getMark().globalMarks.remove(ch);
      }
    }

    private void createVimMark(@NotNull LineBookmark b, char mnemonic) {
      int col = 0;
      Editor editor = EditorHelper.getEditor(b.getFile());
      if (editor != null) col = editor.getCaretModel().getCurrentCaret().getLogicalPosition().column;
      IntellijMark mark = new IntellijMark(b, col, myProject);
      FileMarks<Character, Mark> fmarks = VimPlugin.getMark().getFileMarks(b.getFile().getPath());
      fmarks.put(mnemonic, mark);
      VimPlugin.getMark().globalMarks.put(mnemonic, mark);
    }
  }

  /**
   * COMPATIBILITY-LAYER: Method added
   * Please see: <a href="https://jb.gg/zo8n0r">doc</a>
   *
   * @deprecated Please use method with VimEditor
   */
  @Deprecated
  public void saveJumpLocation(Editor editor) {
    this.saveJumpLocation(new IjVimEditor(editor));
  }

  private static final int SAVE_MARK_COUNT = 20;

  private static final Logger logger = Logger.getInstance(MarkGroup.class.getName());
}
