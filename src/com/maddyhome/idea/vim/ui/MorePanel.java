/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2004 Rick Maddy
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
package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.option.Options;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * This panel displays text in a <code>more</code> like window.
 */
public class MorePanel extends JPanel
{
    public static MorePanel getInstance(Editor editor)
    {
        if (instance == null)
        {
            instance = new MorePanel();
        }

        instance.setEditor(editor);
        
        return instance;
    }

    /**
     * @param editor The editor that this more panel will be displayed over
     */
    public void setEditor(Editor editor)
    {
        this.editor = editor;
        this.parent = editor.getContentComponent();
    }

    /**
     * Creates the panel
     */
    private MorePanel()
    {
        // Create a text editor for the text and a label for the prompt
        BorderLayout layout = new BorderLayout(0, 0);
        setLayout(layout);
        add(scrollPane, BorderLayout.CENTER);
        add(label, BorderLayout.SOUTH);

        Font font = new Font("Monospaced", Font.PLAIN, 12);
        text.setFont(font);
        label.setFont(font);

        text.setBorder(null);
        scrollPane.setBorder(null);

        label.setForeground(text.getForeground());
        label.setBackground(text.getBackground());
        setForeground(text.getForeground());
        setBackground(text.getBackground());

        text.setEditable(false);

        setBorder(BorderFactory.createEtchedBorder());

        // Setup some listeners to handle keystrokes
        moreKeyListener = new MoreKeyListener(this);
        addKeyListener(moreKeyListener);
        text.addKeyListener(moreKeyListener);

        // Setup a listener to handle focus changes
        focusListener = new ParentFocusListener(this);

        resizeListener = new MoreResizeListener(this);
    }

    /**
     * Gets the number of characters that will fit across the 'more' window. This is useful if the text to be
     * presented in the 'more' window needs to be formatted based on the display width.
     * @return The column count
     */
    public int getDisplayWidth()
    {
        Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, parent);
        int width = scroll.getSize().width;
        
        //int width = text.getSize().width;
        logger.debug("width=" + width);
        int charWidth = text.getFontMetrics(text.getFont()).charWidth('M');

        return width / charWidth;
    }

    /**
     * Sets the text of the 'more' window
     * @param data The text to display
     */
    public void setText(String data)
    {
        if (data.length() > 0 && data.charAt(data.length() - 1) == '\n')
        {
            data = data.substring(0, data.length() - 1);
        }

        text.setText(data);
        text.setCaretPosition(0);
    }

    private static int countLines(String text)
    {
        if (text.length() == 0)
        {
            return 0;
        }

        int count = 0;
        int pos = -1;
        while ((pos = text.indexOf('\n', pos + 1)) != -1)
        {
            count++;
        }

        if (text.charAt(text.length() - 1) != '\n')
        {
            count++;
        }

        return count;
    }

    /**
     * Makes the component visible or invisible.
     * Overrides <code>Component.setVisible</code>.
     *
     * @param aFlag  true to make the component visible; false to
     *		make it invisible
     */
    public void setVisible(boolean aFlag)
    {
        if (aFlag)
        {
            JPanel glass = (JPanel)SwingUtilities.getRootPane(parent).getGlassPane();
            glass.setLayout(null);
            glass.add(this);
            glass.addComponentListener(resizeListener);
            glass.setVisible(true);
            positionPanel();
        }

        super.setVisible(aFlag);

        if (aFlag)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    requestFocus();
                    text.requestFocus();

                    focusListener.reset();
                    parent.addFocusListener(focusListener);
                }
            });

            currentPanel = this;
        }
        else
        {
            currentPanel = null;
            JPanel glass = (JPanel)SwingUtilities.getRootPane(parent).getGlassPane();
            glass.setVisible(false);
            glass.remove(this);
            glass.removeComponentListener(resizeListener);
            parent.requestFocus();
        }
    }

    private void positionPanel()
    {
        if (parent == null) return;

        Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, parent);
        setSize(scroll.getSize());

        lineHeight = text.getFontMetrics(text.getFont()).getHeight();
        int count = countLines(text.getText());
        int visLines = getSize().height / lineHeight - 1;
        logger.debug("size.height=" + getSize().height);
        logger.debug("lineHeight=" + lineHeight);
        logger.debug("count=" + count);
        logger.debug("visLines=" + visLines);
        int lines = Math.min(count, visLines);
        setSize(getSize().width, lines * lineHeight + label.getPreferredSize().height +
            getBorder().getBorderInsets(this).top * 2);

        scrollPane.getVerticalScrollBar().setValues(0, visLines, 0, count - 1);

        int height = getSize().height;
        Rectangle bounds = scroll.getBounds();
        bounds.translate(0, scroll.getHeight() - height);
        bounds.height = height;
        Point pos = SwingUtilities.convertPoint(scroll.getParent(), bounds.getLocation(),
            SwingUtilities.getRootPane(parent).getGlassPane());
        bounds.setLocation(pos);
        setBounds(bounds);

        scrollPane.getVerticalScrollBar().setValue(0);
        if (!Options.getInstance().isSet("more"))
        {
            // FIX
            scrollOffset(100000);
        }
        else
        {
            scrollOffset(0);
        }
    }

    private void scrollLine()
    {
        scrollOffset(lineHeight);
    }

    private void scrollPage()
    {
        scrollOffset(scrollPane.getVerticalScrollBar().getVisibleAmount());
    }

    private void scrollHalfPage()
    {
        double sa = scrollPane.getVerticalScrollBar().getVisibleAmount() / 2.0;
        double offset = Math.ceil(sa / lineHeight) * lineHeight;
        scrollOffset((int)offset);
    }

    private void handleEnter()
    {
        if (atEnd)
        {
            close();
        }
        else
        {
            scrollLine();
        }
    }

    private void badKey()
    {
        label.setText("-- MORE -- (RET: line, SPACE: page, d: half page, q: quit)");
    }

    private void scrollOffset(int more)
    {
        int val = scrollPane.getVerticalScrollBar().getValue();
        logger.debug("val=" + val);
        logger.debug("more=" + more);
        scrollPane.getVerticalScrollBar().setValue(val + more);
        scrollPane.getHorizontalScrollBar().setValue(0);
        logger.debug("scrollPane.getVerticalScrollBar().getMaximum()=" + scrollPane.getVerticalScrollBar().getMaximum());
        logger.debug("scrollPane.getVerticalScrollBar().getVisibleAmount()=" + scrollPane.getVerticalScrollBar().getVisibleAmount());
        if (val + more >= scrollPane.getVerticalScrollBar().getMaximum() - scrollPane.getVerticalScrollBar().getVisibleAmount())
        {
            atEnd = true;
            label.setText("Hit ENTER or type command to continue");
        }
        else
        {
            label.setText("-- MORE --");
        }
    }

    private void close()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                setVisible(false);
                //removeKeyListener(moreKeyListener);
                parent.removeFocusListener(focusListener);

                FileEditorManager.getInstance(EditorData.getProject(editor)).openFile(
                    EditorData.getVirtualFile(editor), true);
            }
        });
    }

    static class MoreKeyListener extends KeyAdapter
    {
        public MoreKeyListener(MorePanel parent)
        {
            this.parent = parent;
        }

        /**
         * Invoked when a key has been pressed.
         */
        public void keyPressed(KeyEvent e)
        {
            if (parent.atEnd)
            {
                parent.close();
            }
            else
            {
                switch (e.getKeyChar())
                {
                    case ' ':
                        parent.scrollPage();
                        break;
                    case 'd':
                        parent.scrollHalfPage();
                        break;
                    case 'q':
                        parent.close();
                        break;
                    case '\n':
                        parent.handleEnter();
                        break;
                    case '\u001b':
                        parent.close();
                        break;
                    case KeyEvent.CHAR_UNDEFINED:
                        {
                            switch (e.getKeyCode())
                            {
                                case KeyEvent.VK_ENTER:
                                    parent.handleEnter();
                                    break;
                                case KeyEvent.VK_ESCAPE:
                                    parent.close();
                                    break;
                                default:
                                    logger.debug("e.getKeyCode()=" + e.getKeyCode());
                                    parent.badKey();
                            }
                        }
                    default:
                        logger.debug("e.getKeyChar()=" + (int)e.getKeyChar());
                        parent.badKey();
                }
            }
        }

        private MorePanel parent;
    }

    static class ParentFocusListener extends FocusAdapter
    {
        public ParentFocusListener(MorePanel parent)
        {
            this.parent = parent;
        }

        public void reset()
        {
            cnt = 0;
        }

        public void focusGained(FocusEvent e)
        {
            cnt++;
            if (cnt > 1)
            {
                parent.close();
                logger.debug("cnt="+cnt);
            }
            else
            {
                // This is a kludge to solve a focus problem I was unable to solve an other way.
                parent.requestFocus();
            }
        }

        private MorePanel parent;
        private int cnt;
    }

    public static class MoreEditorChangeListener extends FileEditorManagerAdapter
    {
        public void selectionChanged(FileEditorManagerEvent event)
        {
            if (currentPanel != null)
            {
                currentPanel.close();
            }
        }
    }

    public static class MoreResizeListener extends ComponentAdapter
    {
        public MoreResizeListener(MorePanel parent)
        {
            this.parent = parent;
        }

        /**
         * Invoked when the component's size changes.
         */
        public void componentResized(ComponentEvent e)
        {
            logger.debug("resized");
            parent.positionPanel();
        }

        private MorePanel parent;
    }

    private Editor editor;
    private Component parent;
    private JLabel label = new JLabel("more");
    private JTextArea text = new JTextArea();
    private JScrollPane scrollPane = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private MoreKeyListener moreKeyListener;
    private ParentFocusListener focusListener;
    private MoreResizeListener resizeListener;
    private boolean atEnd;
    private int lineHeight;

    private static MorePanel currentPanel;

    private static Logger logger = Logger.getInstance(MorePanel.class.getName());
    private static MorePanel instance;
}
