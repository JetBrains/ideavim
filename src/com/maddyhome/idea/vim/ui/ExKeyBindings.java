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

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent.KeyBinding;

/**
 *
 */
public class ExKeyBindings
{
    public static KeyBinding[] getBindings()
    {
        return bindings;
    }

    // TODO - add the following keys:
    // Support abort when backspace from first column
    // Down, Ctrl-N - history
    // Shft-Down, PgDn - history
    // Up, Ctrl-P - history
    // Shft-Up, PgUp - history
    // Ctrl-R {register}
    // Ctrl-U - remove all chars to cursor
    // Ctrl-\ Ctrl-N - abort
    static final KeyBinding[] bindings = new KeyBinding[] {
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ExEditorKit.CompleteEdit),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK), ExEditorKit.CompleteEdit),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK), ExEditorKit.CompleteEdit),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ExEditorKit.AbortEdit),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK), ExEditorKit.AbortEdit),
        new KeyBinding(KeyStroke.getKeyStroke('[', KeyEvent.CTRL_MASK), ExEditorKit.AbortEdit),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), ExEditorKit.ToggleInsertReplace),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK), ExEditorKit.beginLineAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), ExEditorKit.beginLineAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK), ExEditorKit.endLineAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), ExEditorKit.endLineAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), ExEditorKit.backwardAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK), ExEditorKit.previousWordAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK), ExEditorKit.previousWordAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), ExEditorKit.forwardAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK), ExEditorKit.nextWordAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK), ExEditorKit.nextWordAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), ExEditorKit.deleteNextCharAction),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), ExEditorKit.DeletePreviousChar),
        new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK), ExEditorKit.DeletePreviousChar)
    };
}
