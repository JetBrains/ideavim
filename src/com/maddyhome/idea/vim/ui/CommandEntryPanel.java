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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * TODO - support complete set of command line editing keys
 */
public class CommandEntryPanel extends JPanel
{
    public static CommandEntryPanel getInstance()
    {
        if (instance == null)
        {
            instance = new CommandEntryPanel();
        }

        return instance;
    }

    private CommandEntryPanel()
    {
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        this.label = new JLabel(" ");
        this.label.setFont(font);
        this.entry = new ExTextField();
        this.entry.setFont(font);

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

        entry.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e)
            {
                if (isActive())
                {
                    deactivate();
                }
            }
        });

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                //deactivate();
            }
        });

        // TODO - move these keys to the ExEditorKit
        entry.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
                {
                    if (entry.getDocument().getLength() == 0)
                    {
                        e.consume();
                        entry.setText("");
                        entry.postActionEvent();
                    }
                }
                /*
                else if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    e.consume();
                    entry.postActionEvent();
                }
                */
            }
        });
    }

    public void activate(JComponent comp, String label, String initText)
    {
        this.label.setText(label);
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

    public void deactivate()
    {
        logger.info("deactivate");
        active = false;
        newGlass.setVisible(false);
        root.setGlassPane(oldGlass);
        parent.requestFocus();
        parent = null;
    }

    public void addActionListener(ActionListener listener)
    {
        entry.addActionListener(listener);
    }

    public void removeActionListener(ActionListener listener)
    {
        entry.removeActionListener(listener);
    }

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

    private boolean active;

    private static CommandEntryPanel instance;

    private static Logger logger = Logger.getInstance(CommandEntryPanel.class.getName());
}
