package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
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

public class EditorDataContext implements DataContext
{
    public EditorDataContext(Editor editor)
    {
        this.editor = editor;
    }

    /**
     * Returns the object corresponding to the specified data identifier. Some of the supported data identifiers are
     * defined in the {@link PlatformDataKeys} class.
     *
     * @param dataId the data identifier for which the value is requested.
     * @return the value, or null if no value is available in the current context for this identifier.
     */
    public Object getData(String dataId)
    {
        if (PlatformDataKeys.EDITOR.getName().equals(dataId))
        {
            return editor;
        }
        else if (PlatformDataKeys.PROJECT.getName().equals(dataId))
        {
            return EditorData.getProject(editor);
        }
        else if (PlatformDataKeys.VIRTUAL_FILE.getName().equals(dataId))
        {
            return EditorData.getVirtualFile(editor);
        }

        return null;
    }

    private Editor editor;
}
