/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.DocumentAdapter;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.group.SearchGroup;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * This is used to enter ex commands such as searches and "colon" commands
 */
public class ExEntryPanel extends JPanel {
  public static ExEntryPanel getInstance() {
    if (instance == null) {
      instance = new ExEntryPanel();
    }

    return instance;
  }

  private ExEntryPanel() {
    setBorder(BorderFactory.createEtchedBorder());
    label = new JLabel(" ");
    entry = new ExTextField();
    entry.setBorder(null);

    setFontForElements();

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
    setBorder(BorderFactory.createEtchedBorder());

    adapter = new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        positionPanel();
      }
    };
  }

  private void setFontForElements() {
    final Font font = UiHelper.getEditorFont();
    label.setFont(font);
    entry.setFont(font);
  }

  /**
   * Turns on the ex entry field for the given editor
   *
   * @param editor   The editor to use for display
   * @param context  The data context
   * @param label    The label for the ex entry (i.e. :, /, or ?)
   * @param initText The initial text for the entry
   * @param count    A holder for the ex entry count
   */
  public void activate(@NotNull Editor editor, DataContext context, @NotNull String label, String initText, int count) {
    entry.setEditor(editor, context);
    this.label.setText(label);
    this.count = count;
    setFontForElements();
    entry.setDocument(entry.createDefaultModel());
    entry.setText(initText);
    entry.setType(label);
    parent = editor.getContentComponent();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
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
      if (isIncSearchEnabled(label)) {
        entry.getDocument().addDocumentListener(documentListener);
        verticalOffset = editor.getScrollingModel().getVerticalScrollOffset();
        horizontalOffset = editor.getScrollingModel().getHorizontalScrollOffset();
      }
      entry.requestFocusInWindow();
    }
    active = true;
  }

  /**
   * Gets the label for the ex entry. This should be one of ":", "/", or "?"
   *
   * @return The ex entry label
   */
  public String getLabel() {
    return label.getText();
  }

  /**
   * Gets the count given during activation
   *
   * @return The count
   */
  public int getCount() {
    return count;
  }

  /**
   * Pass the keystroke on to the text edit for handling
   *
   * @param stroke The keystroke
   */
  public void handleKey(@NotNull KeyStroke stroke) {
    entry.handleKey(stroke);
  }

  private void positionPanel() {
    if (parent == null) return;

    Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, parent);
    int height = (int)getPreferredSize().getHeight();
    if (scroll != null) {
      Rectangle bounds = scroll.getBounds();
      bounds.translate(0, scroll.getHeight() - height);
      bounds.height = height;
      Point pos = SwingUtilities.convertPoint(scroll.getParent(), bounds.getLocation(), oldGlass);
      bounds.setLocation(pos);
      setBounds(bounds);
      repaint();
    }
  }

  /**
   * Gets the text entered by the user. This includes any initial text but does not include the label
   *
   * @return The user entered text
   */
  public String getText() {
    return entry.getText();
  }

  @NotNull
  public ExTextField getEntry() {
    return entry;
  }

  /**
   * Turns off the ex entry field and optionally puts the focus back to the original component
   */
  public void deactivate(boolean refocusOwningEditor) {
    logger.info("deactivate");
    if (!active) return;
    active = false;
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      if (refocusOwningEditor && parent != null) {
        UiHelper.requestFocus(parent);
      }

      oldGlass.removeComponentListener(adapter);
      oldGlass.setVisible(false);
      oldGlass.remove(this);
      oldGlass.setOpaque(wasOpaque);
      oldGlass.setLayout(oldLayout);
      if (isIncSearchEnabled(label.getText())) {
        entry.getDocument().removeDocumentListener(documentListener);
        final Editor editor = entry.getEditor();
        editor.getScrollingModel().scrollVertically(verticalOffset);
        editor.getScrollingModel().scrollHorizontally(horizontalOffset);
        if (incHighlighter != null) {
          editor.getMarkupModel().removeHighlighter(incHighlighter);
        }
      }
    }
    parent = null;
  }

  private boolean isIncSearchEnabled(@NotNull String labelText) {
    return (labelText.equals("/") || labelText.equals("?")) && Options.getInstance().isSet(Options.INCREMENTAL_SEARCH);
  }

  /**
   * Checks if the ex entry panel is currently active
   *
   * @return true if active, false if not
   */
  public boolean isActive() {
    return active;
  }

  @Nullable private JComponent parent;
  @NotNull private final JLabel label;
  @NotNull private final ExTextField entry;
  private JComponent oldGlass;
  private LayoutManager oldLayout;
  private boolean wasOpaque;
  @NotNull private final ComponentAdapter adapter;
  private int count;
  @Nullable private RangeHighlighter incHighlighter = null;
  private int verticalOffset;
  private int horizontalOffset;

  @NotNull private final DocumentListener documentListener = new DocumentAdapter() {
    @Override
    protected void textChanged(DocumentEvent e) {
      final Editor editor = entry.getEditor();
      final boolean forwards = !label.getText().equals("?");
      if (incHighlighter != null) {
        editor.getMarkupModel().removeHighlighter(incHighlighter);
      }
      final String pattern = entry.getText();
      final TextRange range = SearchGroup.findNext(editor, pattern, editor.getCaretModel().getOffset(), true, forwards);
      if (range != null) {
        final TextAttributes color = editor.getColorsScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        incHighlighter = SearchGroup.highlightMatch(editor, range.getStartOffset(), range.getEndOffset());
        incHighlighter.setErrorStripeMarkColor(color.getBackgroundColor());
        incHighlighter.setErrorStripeTooltip(pattern);
        MotionGroup.scrollPositionIntoView(editor, editor.offsetToVisualPosition(range.getStartOffset()), true);
      }
    }
  };

  private boolean active;

  private static ExEntryPanel instance;

  private static final Logger logger = Logger.getInstance(ExEntryPanel.class.getName());
}
