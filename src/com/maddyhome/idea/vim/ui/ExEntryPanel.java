package com.maddyhome.idea.vim.ui;

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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * This is used to enter ex commands such as searches and "colon" commands
 * TODO - support complete set of command line editing keys
 * TODO - redo focus change support to work like MorePanel
 */
public class ExEntryPanel extends JPanel
{
    public static ExEntryPanel getInstance()
    {
        if (instance == null)
        {
            instance = new ExEntryPanel();
        }

        return instance;
    }

    private ExEntryPanel()
    {
        setBorder(BorderFactory.createEtchedBorder());
        
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        label = new JLabel(" ");
        label.setFont(font);
        entry = new ExTextField();
        entry.setFont(font);
        entry.setBorder(null);

        setForeground(entry.getForeground());
        setBackground(entry.getBackground());

        label.setForeground(entry.getForeground());
        label.setBackground(entry.getBackground());

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();

        setLayout(layout);
        gbc.gridx = 0;
        layout.setConstraints(this.label, gbc);
        add(this.label);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(entry, gbc);
        add(entry);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        newGlass = new CommandEntryGlass();
        newGlass.add(this);
        newGlass.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e)
            {
                positionPanel();
            }
        });
    }

    /**
     * Turns on the ex entry field for the given editor
     * @param editor The editor to use for dislay
     * @param context The data context
     * @param label The label for the ex entry (i.e. :, /, or ?)
     * @param initText The initial text for the entry
     * @param count A holder for the ex entry count
     */
    public void activate(Editor editor, DataContext context, String label, String initText, int count)
    {
        //last = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        entry.setEditor(editor, context);
        JComponent comp = editor.getContentComponent();
        this.label.setText(label);
        this.count = count;
        entry.setDocument(entry.createDefaultModel());
        entry.setText(initText);
        parent = comp;
        root = SwingUtilities.getRootPane(parent);
        oldGlass = root.getGlassPane();
        root.setGlassPane(newGlass);

        positionPanel();

        newGlass.setVisible(true);
        entry.requestFocus();
        active = true;
    }

    /**
     * Gets the label for the ex entry. This should be one of ":", "/", or "?"
     * @return The ex entry label
     */
    public String getLabel()
    {
        return label.getText();
    }

    /**
     * Gets the count given during activation
     * @return The count
     */
    public int getCount()
    {
        return count;
    }

    /**
     * Pass the keystroke on to the text edit for handling
     * @param stroke The keystroke
     */
    public void handleKey(KeyStroke stroke)
    {
        entry.handleKey(stroke);
    }

    private void positionPanel()
    {
        if (parent == null) return;

        Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, parent);
        int height = (int)getPreferredSize().getHeight();
        Rectangle bounds = scroll.getBounds();
        bounds.translate(0, scroll.getHeight() - height);
        bounds.height = height;
        Point pos = SwingUtilities.convertPoint(scroll.getParent(), bounds.getLocation(), newGlass);
        bounds.setLocation(pos);
        setBounds(bounds);
        repaint();
    }

    /**
     * Gets the text entered by the user. This includes any initial text but does not include the label
     * @return The user entered text
     */
    public String getText()
    {
        return entry.getText();
    }

    public ExTextField getEntry()
    {
        return entry;
    }
    
    /**
     * Turns off the ex entry field and puts the focus back to the original component
     * @param changeFocus true if focus should be put back, false if not
     */
    public void deactivate(boolean changeFocus)
    {
        logger.info("deactivate");
        if (!active) return;
        active = false;
        newGlass.setVisible(false);
        root.setGlassPane(oldGlass);
        /*
        if (changeFocus)
        {
            logger.debug("parent.requestFocus()");
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    //parent.requestFocus();
                    last.requestFocus();
                }
            });
        }
        */
        parent = null;
    }

    /**
     * Checks if the ex entry panel is currently active
     * @return true if active, false if not
     */
    public boolean isActive()
    {
        return active;
    }

    class CommandEntryGlass extends JPanel
    {
        CommandEntryGlass()
        {
            setLayout(null);
            setOpaque(false);
        }
    }

    private JComponent parent;
    private JLabel label;
    private ExTextField entry;
    private JPanel newGlass;
    private Component oldGlass;
    private JRootPane root;
    private int count;
    //private Component last;

    private boolean active;

    private static ExEntryPanel instance;

    private static Logger logger = Logger.getInstance(ExEntryPanel.class.getName());
}
