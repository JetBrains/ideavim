package com.maddyhome.idea.vim.ui;

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

import com.intellij.openapi.diagnostic.Logger;
import java.awt.Font;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

/**
 * Provides a custom keymap for the text field. The keymap is the VIM Ex command keymapping
 */
public class ExTextField extends JTextField
{
    /**
     */
    public ExTextField()
    {
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        setFont(font);

        setInputMap(WHEN_FOCUSED, new InputMap());
        Keymap map = addKeymap("ex", null);
        loadKeymap(map, ExKeyBindings.getBindings(), getActions());
        map.setDefaultAction(new ExEditorKit.DefaultKeyTypedAction());
        setKeymap(map);
    }

    public Action[] getActions()
    {
        return TextAction.augmentList(super.getActions(), ExEditorKit.getInstance().getActions());
    }

    /**
     * Creates the default implementation of the model
     * to be used at construction if one isn't explicitly
     * given.  An instance of <code>PlainDocument</code> is returned.
     *
     * @return the default model implementation
     */
    protected Document createDefaultModel()
    {
        return new ExDocument();
    }

    // TODO - support block cursor for overwrite mode

    private static Logger logger = Logger.getInstance(ExTextField.class.getName());
}
