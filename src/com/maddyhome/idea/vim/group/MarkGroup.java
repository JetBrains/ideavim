package com.maddyhome.idea.vim.group;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.common.Mark;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.SearchHelper;
import com.maddyhome.idea.vim.VimPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.jdom.Element;

/**
 * This class contains all the mark related functionality
 */
public class MarkGroup extends AbstractActionGroup
{
    /**
     * Creates the class
     */
    public MarkGroup()
    {
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
            public void editorReleased(EditorFactoryEvent event)
            {
                // Save off the last caret position of the file before it is closed
                Editor editor = event.getEditor();
                setMark(editor, null, '"', editor.getCaretModel().getOffset());
            }
        });
    }

    /**
     * Saves the caret location prior to doing a jump
     * @param editor The editor the jump will occur in
     * @param context The data context
     */
    public void saveJumpLocation(Editor editor, DataContext context)
    {
        setMark(editor, context, '\'');
    }

    /**
     * Gets the requested mark for the editor
     * @param editor The editor to get the mark for
     * @param ch The desired mark
     * @return The requested mark if set, null if not set
     */
    public Mark getMark(Editor editor, char ch)
    {
        Mark mark = null;
        if (ch == '`') ch = '\'';

        // Make sure this is a valid mark
        if (VALID_GET_MARKS.indexOf(ch) < 0) return null;

        if ("{}".indexOf(ch) >= 0)
        {
            int offset = SearchHelper.findNextParagraph(editor, ch == '{' ? -1 : 1);
            offset = EditorHelper.normalizeOffset(editor, offset, false);
            LogicalPosition lp = editor.offsetToLogicalPosition(offset);
            mark = new Mark(ch, lp.line, lp.column, EditorData.getVirtualFile(editor));
        }
        // If this is a file mark, get the mark from this file
        else if (FILE_MARKS.indexOf(ch) >= 0)
        {
            HashMap fmarks = getFileMarks(editor.getDocument());
            mark = (Mark)fmarks.get(new Character(ch));
            if (mark != null && mark.isClear())
            {
                fmarks.remove(new Character(ch));
                mark = null;
            }
        }
        // This is a mark from another file
        else if (GLOBAL_MARKS.indexOf(ch) >= 0)
        {
            mark = (Mark)globalMarks.get(new Character(ch));
            if (mark != null && mark.isClear())
            {
                globalMarks.remove(new Character(ch));
                mark = null;
            }
        }

        return mark;
    }

    /**
     * Get's a mark from the file
     * @param editor The editor to get the mark from
     * @param ch The mark to get
     * @return The mark in the current file, if set, null if no such mark
     */
    public Mark getFileMark(Editor editor, char ch)
    {
        Mark mark = null;
        if (ch == '`') ch = '\'';
        HashMap fmarks = getFileMarks(editor.getDocument());
        mark = (Mark)fmarks.get(new Character(ch));
        if (mark != null && mark.isClear())
        {
            fmarks.remove(new Character(ch));
            mark = null;
        }

        return mark;
    }

    /**
     * Sets the specified mark to the caret position of the editor
     * @param editor The editor to get the current position from
     * @param context The data context
     * @param ch The mark set set
     * @return True if a valid, writable mark, false if not
     */
    public boolean setMark(Editor editor, DataContext context, char ch)
    {
        if (VALID_SET_MARKS.indexOf(ch) >= 0)
        {
            return setMark(editor, context, ch, editor.getCaretModel().getOffset());
        }
        else
        {
            return false;
        }
    }

    /**
     * Sets the specified mark to the specified location.
     * @param editor The editor the mark is associated with
     * @param context The data context
     * @param ch The mark to set
     * @param offset The offset to set the mark to
     * @return true if able to set the mark, false if not
     */
    public boolean setMark(Editor editor, DataContext context, char ch, int offset)
    {
        if (ch == '`') ch = '\'';
        LogicalPosition lp = editor.offsetToLogicalPosition(offset);

        VirtualFile vf = null;
        if (context != null)
        {
            vf = (VirtualFile)context.getData(DataConstants.VIRTUAL_FILE);
        }
        else
        {
            vf = EditorData.getVirtualFile(editor);
        }

        if (vf == null)
        {
            return false;
        }

        Mark mark = new Mark(ch, lp.line, lp.column, vf);
        // File specific marks get added to the file
        if (FILE_MARKS.indexOf(ch) >= 0)
        {
            HashMap fmarks = getFileMarks(editor.getDocument());
            fmarks.put(new Character(ch), mark);
        }
        // Global marks get set to both the file and the global list of marks
        else if (GLOBAL_MARKS.indexOf(ch) >= 0)
        {
            HashMap fmarks = getFileMarks(editor.getDocument());
            fmarks.put(new Character(ch), mark);
            Mark oldMark = (Mark)globalMarks.put(new Character(ch), mark);
            if (oldMark != null)
            {
                oldMark.clear();
            }
        }

        return true;
    }

    public List getMarks(Editor editor)
    {
        ArrayList res = new ArrayList();

        res.addAll(getFileMarks(editor.getDocument()).values());
        res.addAll(globalMarks.values());

        Collections.sort(res, new Mark.KeySorter());

        return res;
    }

    /**
     * Gets the map of marks for the specified file
     * @param doc The editor to get the marks for
     * @return The map of marks. The keys are <code>Character</code>s of the mark names, the values are
     * <code>Mark</code>s.
     */
    private FileMarks getFileMarks(Document doc)
    {
        VirtualFile vf = FileDocumentManager.getInstance().getFile(doc);
        if (vf == null)
        {
            return null;
        }

        FileMarks marks = getFileMarks(vf.getPath());

        return marks;
    }

    /**
     * Gets the map of marks for the specified file
     * @param filename The file to get the marks for
     * @return The map of marks. The keys are <code>Character</code>s of the mark names, the values are
     * <code>Mark</code>s.
     */
    private FileMarks getFileMarks(String filename)
    {
        FileMarks marks = (FileMarks)fileMarks.get(filename);
        if (marks == null)
        {
            marks = new FileMarks();
            fileMarks.put(filename, marks);
        }

        return marks;
    }

    /**
     * Allows the group to save its state and any configuration.
     * @param element The plugin's root XML element that this group can add a child to
     */
    public void saveData(Element element)
    {
        Element marksElem = new Element("globalmarks");
        for (Iterator iterator = globalMarks.values().iterator(); iterator.hasNext();)
        {
            Mark mark = (Mark)iterator.next();
            if (!mark.isClear())
            {
                Element markElem = new Element("mark");
                markElem.setAttribute("key", Character.toString(mark.getKey()));
                markElem.setAttribute("line", Integer.toString(mark.getLogicalLine()));
                markElem.setAttribute("column", Integer.toString(mark.getCol()));
                markElem.setAttribute("filename", mark.getFile().getPath());
                marksElem.addContent(markElem);
                logger.debug("saved mark = " + mark);
            }
        }
        element.addContent(marksElem);

        Element fileMarksElem = new Element("filemarks");

        List files = new ArrayList(fileMarks.values());
        Collections.sort(files, new Comparator() {
            public int compare(Object o1, Object o2)
            {
                return ((FileMarks)o1).timestamp.compareTo(((FileMarks)o2).timestamp);
            }
        });

        if (files.size() > SAVE_MARK_COUNT)
        {
            files = files.subList(files.size() - SAVE_MARK_COUNT, files.size());
        }

        for (Iterator iterator = fileMarks.keySet().iterator(); iterator.hasNext();)
        {
            String file = (String)iterator.next();
            FileMarks marks = (FileMarks)fileMarks.get(file);
            if (!files.contains(marks))
            {
                continue;
            }

            if (marks.size() > 0)
            {
                Element fileMarkElem = new Element("file");
                fileMarkElem.setAttribute("name", file);
                fileMarkElem.setAttribute("timestamp", Long.toString(marks.timestamp.getTime()));
                for (Iterator miter = marks.values().iterator(); miter.hasNext();)
                {
                    Mark mark = (Mark)miter.next();
                    if (!mark.isClear() && !Character.isUpperCase(mark.getKey()) && SAVE_FILE_MARKS.indexOf(mark.getKey()) >= 0)
                    {
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
    }

    /**
     * Allows the group to restore its state and any configuration.
     * @param element The plugin's root XML element that this group can add a child to
     */
    public void readData(Element element)
    {
        // We need to keep the filename for now and create the virtual file later. Any attempt to call
        // LocalFileSystem.getInstance().findFileByPath() results in the following error:
        // Read access is allowed from event dispatch thread or inside read-action only
        // (see com.intellij.openapi.application.Application.runReadAction())

        Element marksElem = element.getChild("globalmarks");
        if (marksElem != null)
        {
            List markList = marksElem.getChildren("mark");
            for (int i = 0; i < markList.size(); i++)
            {
                Element markElem = (Element)markList.get(i);
                Mark mark = new Mark(markElem.getAttributeValue("key").charAt(0),
                    Integer.parseInt(markElem.getAttributeValue("line")),
                    Integer.parseInt(markElem.getAttributeValue("column")),
                    markElem.getAttributeValue("filename"));

                globalMarks.put(new Character(mark.getKey()), mark);
                HashMap fmarks = getFileMarks(mark.getFilename());
                fmarks.put(new Character(mark.getKey()), mark);
            }
        }

        logger.debug("globalMarks=" + globalMarks);

        Element fileMarksElem = element.getChild("filemarks");
        if (fileMarksElem != null)
        {
            List fileList = fileMarksElem.getChildren("file");
            for (int i = 0; i < fileList.size(); i++)
            {
                Element fileElem = (Element)fileList.get(i);
                String filename = fileElem.getAttributeValue("name");
                Date timestamp = new Date();
                try
                {
                    long date = Long.parseLong(fileElem.getAttributeValue("timestamp"));
                    timestamp.setTime(date);
                }
                catch (NumberFormatException e)
                {
                }
                FileMarks fmarks = getFileMarks(filename);
                List markList = fileElem.getChildren("mark");
                for (int j = 0; j < markList.size(); j++)
                {
                    Element markElem = (Element)markList.get(j);
                    Mark mark = new Mark(markElem.getAttributeValue("key").charAt(0),
                        Integer.parseInt(markElem.getAttributeValue("line")),
                        Integer.parseInt(markElem.getAttributeValue("column")),
                        filename);

                    fmarks.put(new Character(mark.getKey()), mark);
                }
                fmarks.setTimestamp(timestamp);
            }
        }

        logger.debug("fileMarks=" + fileMarks);
    }

    /**
     * This updates all the marks for a file whenever text is deleted from the file. If the line that contains a mark
     * is completely deleted then the mark is deleted too. If the deleted text is before the marked line, the mark is
     * moved up by the number of deleted lines.
     * @param editor The modified editor
     * @param marks The editor's marks to update
     * @param delStartOff The offset within the editor where the deletion occurred
     * @param delLength The length of the deleted text
     */
    public static void updateMarkFromDelete(Editor editor, HashMap marks, int delStartOff, int delLength)
    {
        // Skip all this work if there are no marks
        if (marks != null && marks.size() > 0 && editor != null)
        {
            // Calculate the logical position of the start and end of the deleted text
            int delEndOff = delStartOff + delLength;
            LogicalPosition delStart = editor.offsetToLogicalPosition(delStartOff);
            LogicalPosition delEnd = editor.offsetToLogicalPosition(delEndOff);
            logger.debug("mark delete. delStart = " + delStart + ", delEnd = " + delEnd);

            // Now analyze each mark to determine if it needs to be updated or removed
            for (Iterator iterator = marks.values().iterator(); iterator.hasNext();)
            {
                Mark mark = (Mark)iterator.next();
                logger.debug("mark = " + mark);
                // If the end of the deleted text is prior to the marked line, simply shift the mark up by the
                // proper number of lines.
                if (delEnd.line < mark.getLogicalLine())
                {
                    int lines = delEnd.line - delStart.line;
                    logger.debug("Shifting mark by " + lines + " lines");
                    mark.setLogicalLine(mark.getLogicalLine() - lines);
                }
                // If the deleted text begins before the mark and ends after the mark then it may be shifted or deleted
                else if (delStart.line <= mark.getLogicalLine() && delEnd.line >= mark.getLogicalLine())
                {
                    int markLineStartOff = EditorHelper.getLineStartOffset(editor, mark.getLogicalLine());
                    int markLineEndOff = EditorHelper.getLineEndOffset(editor, mark.getLogicalLine(), true);
                    // If the marked line is completely within the deleted text, remove the mark
                    if (delStartOff <= markLineStartOff && delEndOff >= markLineEndOff)
                    {
                        mark.clear();
                        iterator.remove();
                        logger.debug("Removed mark");
                    }
                    // The deletion only covers part of the marked line so shift the mark only if the deletion begins
                    // on a line prior to the marked line (which means the deletion must end on the marked line).
                    else if (delStart.line < mark.getLogicalLine())
                    {
                        // shift mark
                        mark.setLogicalLine(delStart.line);
                        logger.debug("Shifting mark to line " + delStart.line);
                    }
                }
            }
        }
    }

    /**
     * This updates all the marks for a file whenever text is inserted into the file. If the line that contains a mark
     * that is after the start of the insertion point, shift the mark by the number of new lines added.
     * @param editor The editor that was updated
     * @param marks The editor's marks
     * @param insStartOff The insertion point
     * @param insLength The length of the insertion
     */
    public static void updateMarkFromInsert(Editor editor, HashMap marks, int insStartOff, int insLength)
    {
        if (marks != null && marks.size() > 0 && editor != null)
        {
            int insEndOff = insStartOff + insLength;
            LogicalPosition insStart = editor.offsetToLogicalPosition(insStartOff);
            LogicalPosition insEnd = editor.offsetToLogicalPosition(insEndOff);
            logger.debug("mark insert. insStart = " + insStart + ", insEnd = " + insEnd);
            int lines = insEnd.line - insStart.line;
            if (lines == 0) return;

            for (Iterator iterator = marks.values().iterator(); iterator.hasNext();)
            {
                Mark mark = (Mark)iterator.next();
                logger.debug("mark = " + mark);
                // Shift the mark if the insertion began on a line prior to the marked line.
                if (insStart.line < mark.getLogicalLine())
                {
                    mark.setLogicalLine(mark.getLogicalLine() + lines);
                    logger.debug("Shifting mark by " + lines + " lines");
                }
            }
        }
    }

    private static class FileMarks extends HashMap
    {
        public Date getTimestamp()
        {
            return timestamp;
        }

        public void setTimestamp(Date timestamp)
        {
            this.timestamp = timestamp;
        }

        public Object put(Object key, Object value)
        {
            timestamp = new Date();
            return super.put(key, value);
        }

        private Date timestamp = new Date();
    }

    /**
     * This class is used to listen to editor document changes
     */
    public static class MarkUpdater extends DocumentAdapter
    {
        /**
         * Creates the listener for the supplied editor
         */
        public MarkUpdater()
        {
        }

        /**
         * This event indicates that a document is about to be changed. We use this event to update all the
         * editor's marks if text is about to be deleted.
         * @param event The change event
         */
        public void beforeDocumentChange(DocumentEvent event)
        {
            if (!VimPlugin.isEnabled()) return;

            logger.debug("MarkUpdater before, event = " + event);
            if (event.getOldLength() == 0) return;

            Document doc = event.getDocument();
            updateMarkFromDelete(getAnEditor(doc), CommandGroups.getInstance().getMark().getFileMarks(doc), event.getOffset(), event.getOldLength());
        }

        /**
         * This event indicates that a document was just changed. We use this event to update all the editor's
         * marks if text was just added.
         * @param event The change event
         */
        public void documentChanged(DocumentEvent event)
        {
            if (!VimPlugin.isEnabled()) return;

            logger.debug("MarkUpdater after, event = " + event);
            if (event.getNewLength() == 0 || (event.getNewLength() == 1 && !event.getNewFragment().equals("\n"))) return;

            Document doc = event.getDocument();
            updateMarkFromInsert(getAnEditor(doc), CommandGroups.getInstance().getMark().getFileMarks(doc), event.getOffset(), event.getNewLength());
        }

        private Editor getAnEditor(Document doc)
        {
            Editor[] editors = EditorFactory.getInstance().getEditors(doc);

            if (editors.length > 0)
            {
                return editors[0];
            }
            else
            {
                return null;
            }
        }
    }

    private HashMap fileMarks = new HashMap();
    private HashMap globalMarks = new HashMap();

    private int SAVE_MARK_COUNT = 20;

    private static final String WR_GLOBAL_MARKS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String WR_FILE_MARKS = "abcdefghijklmnopqrstuvwxyz'";
    private static final String RO_GLOBAL_MARKS = "0123456789";
    private static final String RO_FILE_MARKS = "[]<>^{}";
    private static final String SAVE_FILE_MARKS = WR_FILE_MARKS + ".^[]\"";

    private static final String GLOBAL_MARKS = WR_GLOBAL_MARKS + RO_GLOBAL_MARKS;
    private static final String FILE_MARKS = WR_FILE_MARKS + RO_FILE_MARKS;

    private static final String WRITE_MARKS = WR_GLOBAL_MARKS + WR_FILE_MARKS;
    private static final String READONLY_MARKS = RO_GLOBAL_MARKS + RO_FILE_MARKS;

    private static final String VALID_SET_MARKS = WRITE_MARKS;
    private static final String VALID_GET_MARKS = WRITE_MARKS + READONLY_MARKS;

    private static Logger logger = Logger.getInstance(MarkGroup.class.getName());
}
