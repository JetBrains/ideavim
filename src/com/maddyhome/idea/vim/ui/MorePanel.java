package com.maddyhome.idea.vim.ui;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2008 Rick Maddy
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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.option.Options;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * This panel displays text in a <code>more</code> like window.
 */
public class MorePanel extends JPanel {
  public static MorePanel getInstance() {
    if (instance == null) {
      instance = new MorePanel();
    }

    return instance;
  }

  public static MorePanel getInstance(Editor editor) {
    if (instance == null) {
      instance = new MorePanel();
    }

    instance.setEditor(editor);
    return instance;
  }

  /**
   * @param editor The editor that this more panel will be displayed over
   */
  public void setEditor(Editor editor) {
    this.editor = editor;
    this.parent = editor.getContentComponent();
  }

  private MorePanel() {
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

    adapter = new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        positionPanel();
      }
    };

    // Setup some listeners to handle keystrokes
    MoreKeyListener moreKeyListener = new MoreKeyListener(this);
    addKeyListener(moreKeyListener);
    text.addKeyListener(moreKeyListener);
  }

  /**
   * Gets the number of characters that will fit across the 'more' window. This is useful if the text to be
   * presented in the 'more' window needs to be formatted based on the display width.
   *
   * @return The column count
   */
  public int getDisplayWidth() {
    Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, parent);
    int width = scroll.getSize().width;

    if (logger.isDebugEnabled()) logger.debug("width=" + width);
    int charWidth = text.getFontMetrics(text.getFont()).charWidth('M');

    return width / charWidth;
  }

  public boolean hasText() {
    return text.getText().length() > 0;
  }

  public String getText() {
    return text.getText();
  }

  public void setText(String data) {
    if (data.length() > 0 && data.charAt(data.length() - 1) == '\n') {
      data = data.substring(0, data.length() - 1);
    }

    text.setText(data);
    text.setCaretPosition(0);
  }

  /**
   * Turns on the more window for the given editor
   */
  public void activate() {
    JRootPane root = SwingUtilities.getRootPane(parent);
    oldGlass = (JComponent)root.getGlassPane();
    oldLayout = oldGlass.getLayout();
    wasOpaque = oldGlass.isOpaque();
    oldGlass.setLayout(null);
    oldGlass.setOpaque(false);
    oldGlass.add(this);
    oldGlass.addComponentListener(adapter);

    positionPanel();

    oldGlass.setVisible(true);
    active = true;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            text.requestFocus();
          }
        });
      }
    });
  }

  /**
   * Turns off the ex entry field and puts the focus back to the original component
   */
  public void deactivate(boolean changeFocus) {
    logger.info("deactivate");
    if (!active) return;
    active = false;
    text.setText("");
    oldGlass.removeComponentListener(adapter);
    oldGlass.setVisible(false);
    oldGlass.remove(this);
    oldGlass.setOpaque(wasOpaque);
    oldGlass.setLayout(oldLayout);
    parent.requestFocus();
    parent = null;
  }

  /**
   * Checks if the ex entry panel is currently active
   *
   * @return true if active, false if not
   */
  public boolean isActive() {
    return active;
  }

  private static int countLines(String text) {
    if (text.length() == 0) {
      return 0;
    }

    int count = 0;
    int pos = -1;
    while ((pos = text.indexOf('\n', pos + 1)) != -1) {
      count++;
    }

    if (text.charAt(text.length() - 1) != '\n') {
      count++;
    }

    return count;
  }

  private void scrollLine() {
    scrollOffset(lineHeight);
  }

  private void scrollPage() {
    scrollOffset(scrollPane.getVerticalScrollBar().getVisibleAmount());
  }

  private void scrollHalfPage() {
    double sa = scrollPane.getVerticalScrollBar().getVisibleAmount() / 2.0;
    double offset = Math.ceil(sa / lineHeight) * lineHeight;
    scrollOffset((int)offset);
  }

  private void handleEnter() {
    if (atEnd) {
      close();
    }
    else {
      scrollLine();
    }
  }

  private void badKey() {
    label.setText("-- MORE -- (RET: line, SPACE: page, d: half page, q: quit)");
  }

  private void scrollOffset(int more) {
    atEnd = false;
    int val = scrollPane.getVerticalScrollBar().getValue();
    scrollPane.getVerticalScrollBar().setValue(val + more);
    scrollPane.getHorizontalScrollBar().setValue(0);
    if (logger.isDebugEnabled()) {
      logger.debug("val=" + val);
      logger.debug("more=" + more);
      logger.debug("scrollPane.getVerticalScrollBar().getMaximum()=" + scrollPane.getVerticalScrollBar().getMaximum());
      logger.debug("scrollPane.getVerticalScrollBar().getVisibleAmount()=" + scrollPane.getVerticalScrollBar().getVisibleAmount());
    }
    if (val + more >= scrollPane.getVerticalScrollBar().getMaximum() - scrollPane.getVerticalScrollBar().getVisibleAmount()) {
      atEnd = true;
      label.setText("Hit ENTER or type command to continue");
    }
    else {
      label.setText("-- MORE --");
    }
  }

  private void positionPanel() {
    if (parent == null) return;

    Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, parent);
    setSize(scroll.getSize());

    lineHeight = text.getFontMetrics(text.getFont()).getHeight();
    int count = countLines(text.getText());
    int visLines = getSize().height / lineHeight - 1;
    if (logger.isDebugEnabled()) {
      logger.debug("size.height=" + getSize().height);
      logger.debug("lineHeight=" + lineHeight);
      logger.debug("count=" + count);
      logger.debug("visLines=" + visLines);
    }
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
    if (!Options.getInstance().isSet("more")) {
      // FIX
      scrollOffset(100000);
    }
    else {
      scrollOffset(0);
    }
  }

  private void close() {
    close(null);
  }

  private void close(final KeyEvent e) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        deactivate(false);

        VirtualFile vf = EditorData.getVirtualFile(editor);
        if (vf != null) {
          FileEditorManager.getInstance(EditorData.getProject(editor)).openFile(vf, true);
        }

        if (e != null && e.getKeyChar() != '\n') {
          KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
          Project project = EditorData.getProject(editor);
          List<KeyStroke> keys = new ArrayList<KeyStroke>(1);
          keys.add(key);
          CommandGroups.getInstance().getMacro().playbackKeys(editor,
                                                              new EditorDataContext(editor), project, keys, 0, 0, 1);
        }
      }
    });
  }

  private static class MoreKeyListener extends KeyAdapter {
    public MoreKeyListener(MorePanel parent) {
      this.parent = parent;
    }

    /**
     * Invoked when a key has been pressed.
     */
    public void keyTyped(KeyEvent e) {
      if (parent.atEnd) {
        parent.close(e);
      }
      else {
        switch (e.getKeyChar()) {
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
          case KeyEvent.CHAR_UNDEFINED: {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_ENTER:
                parent.handleEnter();
                break;
              case KeyEvent.VK_ESCAPE:
                parent.close();
                break;
              default:
                if (logger.isDebugEnabled()) logger.debug("e.getKeyCode()=" + e.getKeyCode());
                parent.badKey();
            }
          }
          default:
            if (logger.isDebugEnabled()) logger.debug("e.getKeyChar()=" + (int)e.getKeyChar());
            parent.badKey();
        }
      }
    }

    private MorePanel parent;
  }

  private JLabel label = new JLabel("more");
  private JTextArea text = new JTextArea();
  private JScrollPane scrollPane = new JBScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  private ComponentAdapter adapter;
  private Editor editor = null;
  private JComponent parent = null;
  private boolean atEnd = false;
  private int lineHeight = 0;

  private JComponent oldGlass = null;
  private LayoutManager oldLayout = null;
  private boolean wasOpaque = false;

  private boolean active = false;

  private static MorePanel instance;

  private static Logger logger = Logger.getInstance(MorePanel.class.getName());
}

