package com.maddyhome.idea.vim.helper;

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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * This is a set of helper methods for working with editors. All line and column values are zero based.
 */
public class EditorHelper
{
    /**
     * Gets the visual line number the cursor is on
     * @param editor The editor
     * @return The cursor's visual line number
     */
    public static int getCurrentVisualLine(Editor editor)
    {
        return editor.getCaretModel().getVisualPosition().line;
    }

    /**
     * Gets the visual column number the cursor is on
     * @param editor The editor
     * @return The cursor's visual column number
     */
    public static int getCurrentVisualColumn(Editor editor)
    {
        return editor.getCaretModel().getVisualPosition().column;
    }

    /**
     * Gets the logical line number the cursor is on
     * @param editor The editor
     * @return The cursor's logical line number
     */
    public static int getCurrentLogicalLine(Editor editor)
    {
        return editor.getCaretModel().getLogicalPosition().line;
    }

    /**
     * Gets the logical column number the cursor is on
     * @param editor The editor
     * @return The cursor's logical column number
     */
    public static int getCurrentLogicalColumn(Editor editor)
    {
        return editor.getCaretModel().getLogicalPosition().column;
    }

    /**
     * Gets the number of characters on the current line. This will be different than the number of visual
     * characters if there are "real" tabs in the line.
     * @param editor The editor
     * @return The number of characters in the current line
     */
    public static int getLineLength(Editor editor)
    {
        int lline = getCurrentLogicalLine(editor);

        return getLineLength(editor, lline);
    }

    /**
     * Gets the number of characters on the specified logical line. This will be different than the number of visual
     * characters if there are "real" tabs in the line.
     * @param editor The editor
     * @param lline The logical line within the file
     * @return The number of characters in the specified line
     */
    public static int getLineLength(Editor editor, int lline)
    {
        return Math.max(0, editor.offsetToLogicalPosition(editor.getDocument().getLineEndOffset(lline)).column);
    }

    /**
     * Gets the number of characters on the specified visual line. This will be different than the number of visual
     * characters if there are "real" tabs in the line.
     * @param editor The editor
     * @param vline The visual line within the file
     * @return The number of characters in the specified line
     */
    public static int getVisualLineLength(Editor editor, int vline)
    {
        int lline = visualLineToLogicalLine(editor, vline);
        return getLineLength(editor, lline);
    }

    /**
     * Gets the number of visible lines in the editor. This will less then the actual number of lines in the file
     * if there are any collapsed folds.
     * @param editor The editor
     * @return The number of visible lines in the file
     */
    public static int getVisualLineCount(Editor editor)
    {
        int count = getLineCount(editor);
        return count == 0 ? 0 : logicalLineToVisualLine(editor, count - 1) + 1;
    }

    /**
     * Gets the number of actual lines in the file
     * @param editor The editor
     * @return The file line count
     */
    public static int getLineCount(Editor editor)
    {
        int len = editor.getDocument().getLineCount();
        if (editor.getDocument().getTextLength() > 0 && editor.getDocument().getChars()[editor.getDocument().getTextLength() - 1] == '\n')
        {
            len--;
        }

        return len;
    }

    /**
     * Gets the actual number of characters in the file
     * @param editor The editor
     * @return The file's character count
     */
    public static int getFileSize(Editor editor)
    {
        int len = editor.getDocument().getTextLength();
        if (len >= 1 && editor.getDocument().getChars()[len - 1] == '\n')
        {
            len--;
        }

        return len;
    }

    /**
     * Gets the number of lines than can be displayed on the screen at one time. This is rounded down to the
     * nearest whole line if there is a parial line visible at the bottom of the screen.
     * @param editor The editor
     * @return The number of screen lines
     */
    public static int getScreenHeight(Editor editor)
    {
        return editor.getScrollingModel().getVisibleArea().height / editor.getLineHeight();
    }

    /**
     * Gets the number of characters that are visible on a screen line
     * @param editor The editor
     * @return The number of screen columns
     */
    public static int getScreenWidth(Editor editor)
    {
        Rectangle rect = editor.getScrollingModel().getVisibleArea();
        Point pt = new Point(rect.width, 0);
        VisualPosition vp = editor.xyToVisualPosition(pt);

        return vp.column;
    }

    /**
     * Converts a visual line number to a logical line number.
     * @param editor The editor
     * @param vline The visual line number to convert
     * @return The logical line number
     */
    public static int visualLineToLogicalLine(Editor editor, int vline)
    {
        return editor.visualToLogicalPosition(new VisualPosition(vline, 0)).line;
    }

    /**
     * Converts a logical line number to a visual line number. Several logical lines can map to the same
     * visual line when there are collapsed fold regions.
     * @param editor The editor
     * @param lline The logical line number to convert
     * @return The visual line number
     */
    public static int logicalLineToVisualLine(Editor editor, int lline)
    {
        return editor.logicalToVisualPosition(new LogicalPosition(lline, 0)).line;
    }

    /**
     * Returns the offset of the start of the requested line.
     * @param editor The editor
     * @param lline The logical line to get the start offset for.
     * @return 0 if line is &lt 0, file size of line is bigger than file, else the start offset for the line
     */
    public static int getLineStartOffset(Editor editor, int lline)
    {
        if (lline < 0)
        {
            return 0;
        }
        else if (lline >= getLineCount(editor))
        {
            return getFileSize(editor);
        }
        else
        {
            return editor.getDocument().getLineStartOffset(lline);
        }
    }

    /**
     * Returns the offset of the end of the requested line.
     * @param editor The editor
     * @param lline The logical line to get the end offset for.
     * @return 0 if line is &lt 0, file size of line is bigger than file, else the end offset for the line
     */
    public static int getLineEndOffset(Editor editor, int lline)
    {
        if (lline < 0)
        {
            return 0;
        }
        else if (lline >= getLineCount(editor))
        {
            return getFileSize(editor);
        }
        else
        {
            return editor.getDocument().getLineEndOffset(lline);
        }
    }

    /**
     * Ensures that the supplied visual line is within the range 0 (incl) and the number of visual lines in the file
     * (excl).
     * @param editor The editor
     * @param vline The visual line number to normalize
     * @return The normalized visual line number
     */
    public static int normalizeVisualLine(Editor editor, int vline)
    {
        vline = Math.min(Math.max(0, vline), getVisualLineCount(editor) - 1);

        return vline;
    }

    /**
     * Ensures that the supplied logical line is within the range 0 (incl) and the number of logical lines in the file
     * (excl).
     * @param editor The editor
     * @param lline The logical line number to normalize
     * @return The normalized logical line number
     */
    public static int normalizeLine(Editor editor, int lline)
    {
        lline = Math.max(0, Math.min(lline, getLineCount(editor) - 1));

        return lline;
    }

    /**
     * Ensures that the supplied column number for the given visual line is within the range 0 (incl) and the
     * number of columns in the line (excl).
     * @param editor The editor
     * @param vline The visual line number
     * @param col The column number to normalize
     * @return The normalized column number
     */
    public static int normalizeVisualColumn(Editor editor, int vline, int col)
    {
        col = Math.min(Math.max(0, col), getVisualLineLength(editor, vline) - 1);

        return col;
    }

    /**
     * Ensures that the supplied column number for the given logical line is within the range 0 (incl) and the
     * number of columns in the line (excl).
     * @param editor The editor
     * @param lline The logical line number
     * @param col The column number to normalize
     * @return The normalized column number
     */
    public static int normalizeColumn(Editor editor, int lline, int col)
    {
        col = Math.min(Math.max(0, col), getLineLength(editor, lline) - 1);

        return col;
    }

    /**
     * Ensures that the supplied offset for the given logical line is within the range for the line. If allowEnd
     * is true, the range will allow for the offset to be one past the last character on the line.
     * @param editor The editor
     * @param lline The logical line number
     * @param offset The offset to normalize
     * @param allowEnd true if the offset can be one past the last character on the line, false if not
     * @return The normalized column number
     */
    public static int normalizeOffset(Editor editor, int lline, int offset, boolean allowEnd)
    {
        if (getFileSize(editor) == 0)
        {
            return 0;
        }

        int min = getLineStartOffset(editor, lline);
        int max = getLineEndOffset(editor, lline) - (allowEnd ? 0 : 1);
        offset = Math.max(Math.min(offset, max), min);

        return offset;
    }

    public static int normalizeOffset(Editor editor, int offset, boolean allowEnd)
    {
        int lline = editor.offsetToLogicalPosition(offset).line;
        
        return normalizeOffset(editor, lline, offset, allowEnd);
    }

    /**
     * Gets the editor for the virtual file within the editor mananger.
     * @param manager The file editor manager
     * @param file The virtual file get the editor for
     * @return The matching editor or null if no match was found
     */
    public static Editor getEditor(FileEditorManager manager, VirtualFile file)
    {
        Editor[] editors = manager.getAllEditors();
        for (int i = 0; i < editors.length; i++)
        {
            if (manager.fileToDocument(file).equals(editors[i].getDocument()))
            {
                return editors[i];
            }
        }

        return null;
    }

    /**
     * Converts a visual position to a file offset
     * @param editor The editor
     * @param pos The visual position to convert
     * @return The file offset of the visual position
     */
    public static int visualPostionToOffset(Editor editor, VisualPosition pos)
    {
        return editor.logicalPositionToOffset(editor.visualToLogicalPosition(pos));
    }

    /**
     * Gets a string representation of the file for the supplied offset range
     * @param editor The editor
     * @param start The starting offset (inclusive)
     * @param end The ending offset (exclusive)
     * @return The string, never null but empty if start == end
     */
    public static String getText(Editor editor, int start, int end)
    {
        return new String(editor.getDocument().getChars(), start, end - start);
    }

    /**
     * Gets the offset of the start of the line containing the supplied offset
     * @param editor The editor
     * @param offset The offset within the line
     * @return The offset of the line start
     */
    public static int getLineStartForOffset(Editor editor, int offset)
    {
        LogicalPosition pos = editor.offsetToLogicalPosition(offset);
        return editor.getDocument().getLineStartOffset(pos.line);
    }

    /**
     * Gets the offset of the end of the line containing the supplied offset
     * @param editor The editor
     * @param offset The offset within the line
     * @return The offset of the line end
     */
    public static int getLineEndForOffset(Editor editor, int offset)
    {
        LogicalPosition pos = editor.offsetToLogicalPosition(offset);
        return editor.getDocument().getLineEndOffset(pos.line);
    }

    /**
     * Returns the text of the requested logical line
     * @param editor The editor
     * @param lline The logical line to get the text for
     * @return The requested line
     */
    public static String getLineText(Editor editor, int lline)
    {
        return getText(editor, getLineStartOffset(editor, lline), getLineEndOffset(editor, lline));
    }
}
