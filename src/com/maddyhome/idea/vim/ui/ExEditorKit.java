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
import com.maddyhome.idea.vim.helper.SearchHelper;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

/**
 *
 */
public class ExEditorKit extends DefaultEditorKit
{
    public static ExEditorKit getInstance()
    {
        if (instance == null)
        {
            instance = new ExEditorKit();
        }

        return instance;
    }

    /**
     * Gets the MIME type of the data that this
     * kit represents support for.
     *
     * @return the type
     */
    public String getContentType()
    {
        return "text/plain";
    }

    /**
     * Fetches the set of commands that can be used
     * on a text component that is using a model and
     * view produced by this kit.
     *
     * @return the set of actions
     */
    public Action[] getActions()
    {
        return TextAction.augmentList(super.getActions(), this.exActions);
    }

    /**
     * Creates an uninitialized text storage model
     * that is appropriate for this type of editor.
     *
     * @return the model
     */
    public Document createDefaultDocument()
    {
        return new ExDocument();
    }

    public static final String CompleteEdit = "complete-edit";
    public static final String AbortEdit = "abort-edit";
    public static final String DeletePreviousChar = "delete-prev-char";
    public static final String DeletePreviousWord = "delete-prev-word";
    public static final String DeleteToCursor = "delete-to-cursor";
    public static final String ToggleInsertReplace = "toggle-insert";
    public static final String InsertRegister = "insert-register";
    public static final String InsertWord = "insert-word";
    public static final String InsertWORD = "insert-WORD";
    public static final String HistoryRecent = "history-recent";
    public static final String HistoryOld = "history-old";
    public static final String HistoryRecentFilter = "history-recent-filter";
    public static final String HistoryOldFilter = "history-old-filter";

    //TODO - add rest of actions
    protected Action[] exActions = new Action[] {
        new CompleteEditAction(),
        new AbortEditAction(),
        new DeletePreviousCharAction(),
        new DeletePreviousWordAction(),
        new DeleteToCursorAction(),
        new ToggleInsertReplaceAction()
    };

    public static class CompleteEditAction extends TextAction
    {
        public CompleteEditAction()
        {
            super(CompleteEdit);
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e)
        {
            logger.info("actionPeformed");
            JTextField target = (JTextField)getTextComponent(e);
            target.postActionEvent();
        }
    }

    public static class AbortEditAction extends TextAction
    {
        public AbortEditAction()
        {
            super(AbortEdit);
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e)
        {
            logger.info("actionPeformed");
            JTextField target = (JTextField)getTextComponent(e);
            target.setText("");
            target.postActionEvent();
        }
    }

    // TODO - how do I get the argument (register name)?
    public static class InsertRegisterAction extends TextAction
    {
        public InsertRegisterAction()
        {
            super(InsertRegister);
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent target = getTextComponent(e);
            if (target != null && target.isEditable())
            {
            }
        }

    }

    public static class DeletePreviousCharAction extends TextAction
    {
        public DeletePreviousCharAction()
        {
            super(DeletePreviousChar);
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e)
        {
            JTextField target = (JTextField)getTextComponent(e);
            if ((target != null) && (target.isEditable()))
            {
                try
                {
                    Document doc = target.getDocument();
                    Caret caret = target.getCaret();
                    int dot = caret.getDot();
                    int mark = caret.getMark();
                    if (dot != mark)
                    {
                        doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
                    }
                    else if (dot > 0)
                    {
                        int delChars = 1;

                        if (dot > 1)
                        {
                            String dotChars = doc.getText(dot - 2, 2);
                            char c0 = dotChars.charAt(0);
                            char c1 = dotChars.charAt(1);

                            if (c0 >= '\uD800' && c0 <= '\uDBFF' &&
                                c1 >= '\uDC00' && c1 <= '\uDFFF')
                            {
                                delChars = 2;
                            }
                        }

                        doc.remove(dot - delChars, delChars);
                    }
                    else
                    {
                        if (doc.getLength() == 0)
                        {
                            target.postActionEvent();
                        }
                    }
                }
                catch (BadLocationException bl)
                {
                }
            }
        }
    }

    public static class DeletePreviousWordAction extends TextAction
    {
        public DeletePreviousWordAction()
        {
            super(DeletePreviousWord);
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent target = getTextComponent(e);
            if (target != null && target.isEditable())
            {
                Document doc = target.getDocument();
                Caret caret = target.getCaret();
                int offset = SearchHelper.findNextWord(target.getText().toCharArray(), caret.getDot(), doc.getLength(),
                    -1, false);
                try
                {
                    doc.remove(offset, caret.getDot());
                }
                catch (BadLocationException ex)
                {
                }
            }
        }
    }

    public static class DeleteToCursorAction extends TextAction
    {
        public DeleteToCursorAction()
        {
            super(DeleteToCursor);
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent target = getTextComponent(e);
            if (target != null && target.isEditable())
            {
                Document doc = target.getDocument();
                Caret caret = target.getCaret();
                try
                {
                    doc.remove(0, caret.getDot());
                }
                catch (BadLocationException ex)
                {
                }
            }
        }
    }

    public static class ToggleInsertReplaceAction extends TextAction
    {
        public ToggleInsertReplaceAction()
        {
            super(ToggleInsertReplace);
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e)
        {
            JTextComponent target = getTextComponent(e);
            ExDocument doc = (ExDocument)target.getDocument();
            doc.toggleInsertReplace();
        }
    }

    private static ExEditorKit instance;

    private static Logger logger = Logger.getInstance(ExEditorKit.class.getName());
}
