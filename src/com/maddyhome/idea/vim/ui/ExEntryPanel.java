/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.IJSwingUtilities;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.regexp.CharPointer;
import com.maddyhome.idea.vim.regexp.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * This is used to enter ex commands such as searches and "colon" commands
 */
public class ExEntryPanel extends JPanel implements LafManagerListener {
  public static ExEntryPanel getInstance() {
    if (instance == null) {
      instance = new ExEntryPanel();
    }

    return instance;
  }

  private ExEntryPanel() {
    label = new JLabel(" ");
    entry = new ExTextField();

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

    new ExShortcutKeyAction(this).registerCustomShortcutSet();

    LafManager.getInstance().addLafManagerListener(this);

    updateUI();
  }

  @Override
  public void lookAndFeelChanged(@NotNull LafManager source) {
    // Calls updateUI on this and child components
    IJSwingUtilities.updateComponentTreeUI(this);
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
    this.label.setText(label);
    this.count = count;
    setFontForElements();
    entry.reset();
    entry.setEditor(editor, context);
    entry.setText(initText);
    entry.setType(label);
    parent = editor.getContentComponent();

    if (isIncSearchEnabled(label)) {
      entry.getDocument().addDocumentListener(incSearchDocumentListener);
      caretOffset = editor.getCaretModel().getOffset();
      verticalOffset = editor.getScrollingModel().getVerticalScrollOffset();
      horizontalOffset = editor.getScrollingModel().getHorizontalScrollOffset();
    }

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      JRootPane root = SwingUtilities.getRootPane(parent);
      oldGlass = (JComponent)root.getGlassPane();
      oldLayout = oldGlass.getLayout();
      wasOpaque = oldGlass.isOpaque();
      oldGlass.setLayout(null);
      oldGlass.setOpaque(false);
      oldGlass.add(this);
      oldGlass.addComponentListener(resizePanelListener);
      positionPanel();
      oldGlass.setVisible(true);
      entry.requestFocusInWindow();
    }
    active = true;
  }

  // Called automatically when the LAF is changed and the component is visible, and manually by the LAF listener handler
  @Override
  public void updateUI() {
    super.updateUI();

    setBorder(new ExPanelBorder());

    // Can be null when called from base constructor
    //noinspection ConstantConditions
    if (entry != null && label != null) {

      setFontForElements();

      // Label background is automatically picked up
      label.setForeground(entry.getForeground());
    }
  }

  // Entry can be null if getForeground is called during base class initialisation
  @SuppressWarnings("ConstantConditions")
  @Override
  public Color getForeground() {
    return entry != null ? entry.getForeground() : super.getForeground();
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public Color getBackground() {
    return entry != null ? entry.getBackground() : super.getBackground();
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
    return entry.getActualText();
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

    // incsearch won't change in the lifetime of this activation
    if (isIncSearchEnabled(label.getText())) {
      entry.getDocument().removeDocumentListener(incSearchDocumentListener);
      final Editor editor = entry.getEditor();
      if (!editor.isDisposed()) {
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), caretOffset);
        editor.getScrollingModel().scrollVertically(verticalOffset);
        editor.getScrollingModel().scrollHorizontally(horizontalOffset);
      }
      // This is somewhat inefficient. We've done the search, highlighted everything and now (if we hit <Enter>), we're
      // removing all the highlights to invoke the search action, to search and highlight everything again. On the plus
      // side, it clears up the current item highlight
      VimPlugin.getSearch().resetIncsearchHighlights();
    }

    entry.deactivate();

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      if (refocusOwningEditor && parent != null) {
        UiHelper.requestFocus(parent);
      }

      oldGlass.removeComponentListener(resizePanelListener);
      oldGlass.setVisible(false);
      oldGlass.remove(this);
      oldGlass.setOpaque(wasOpaque);
      oldGlass.setLayout(oldLayout);
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

  private boolean active;
  private int count;

  // UI stuff
  @Nullable private JComponent parent;
  @NotNull private final JLabel label;
  @NotNull private final ExTextField entry;
  private JComponent oldGlass;
  private LayoutManager oldLayout;
  private boolean wasOpaque;

  // incsearch stuff
  private int verticalOffset;
  private int horizontalOffset;
  private int caretOffset;

  @NotNull private final ComponentListener resizePanelListener = new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
      positionPanel();
    }
  };

  @NotNull private final DocumentListener incSearchDocumentListener = new DocumentAdapter() {
    @Override
    protected void textChanged(@NotNull DocumentEvent e) {
      final Editor editor = entry.getEditor();

      final boolean forwards = !label.getText().equals("?");
      final String searchText = entry.getActualText();
      final CharPointer p = new CharPointer(searchText);
      final CharPointer end = RegExp.skip_regexp(new CharPointer(searchText), forwards ? '/' : '?', true);
      final String pattern = p.substring(end.pointer() - p.pointer());

      VimPlugin.getSearch().updateIncsearchHighlights(editor, pattern, forwards, caretOffset);
    }
  };

  private static ExEntryPanel instance;
  private static final Logger logger = Logger.getInstance(ExEntryPanel.class.getName());
}
