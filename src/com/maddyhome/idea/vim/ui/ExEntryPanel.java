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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ui;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.IJSwingUtilities;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.LineRange;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.regexp.CharPointer;
import com.maddyhome.idea.vim.regexp.RegExp;
import org.jetbrains.annotations.Contract;
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
  private static ExEntryPanel instance;
  private static ExEntryPanel instanceWithoutShortcuts;

  private ExEntryPanel(boolean enableShortcuts) {
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

    if (enableShortcuts) {
      // This does not need to be unregistered, it's registered as a custom UI property on this
      new ExShortcutKeyAction(this).registerCustomShortcutSet();
    }

    // [VERSION UPDATE] 193+
    //noinspection deprecation
    LafManager.getInstance().addLafManagerListener(this);

    updateUI();
  }

  public static ExEntryPanel getInstance() {
    if (instance == null) {
      instance = new ExEntryPanel(true);
    }

    return instance;
  }

  public static ExEntryPanel getInstanceWithoutShortcuts() {
    if (instanceWithoutShortcuts == null) {
      instanceWithoutShortcuts = new ExEntryPanel(false);
    }

    return instanceWithoutShortcuts;
  }

  public static void fullReset() {
    if (instance != null) {
      instance.reset();
      instance = null;
    }
    if (instanceWithoutShortcuts != null) {
      instanceWithoutShortcuts.reset();
      instanceWithoutShortcuts = null;
    }
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

    if (isIncSearchEnabled()) {
      entry.getDocument().addDocumentListener(incSearchDocumentListener);
      caretOffset = editor.getCaretModel().getOffset();
      verticalOffset = editor.getScrollingModel().getVerticalScrollOffset();
      horizontalOffset = editor.getScrollingModel().getHorizontalScrollOffset();
    }

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      JRootPane root = SwingUtilities.getRootPane(parent);
      if (root == null) return;
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

  public static void deactivateAll() {
    if (instance != null && instance.active) {
      instance.deactivate(false);
    }
    if (instanceWithoutShortcuts != null && instanceWithoutShortcuts.active) {
      instanceWithoutShortcuts.deactivate(false);
    }
  }

  public void deactivate(boolean refocusOwningEditor) {
    deactivate(refocusOwningEditor, true);
  }

  /**
   * Turns off the ex entry field and optionally puts the focus back to the original component
   */
  public void deactivate(boolean refocusOwningEditor, boolean resetCaret) {
    logger.info("Deactivate ex entry panel");
    if (!active) return;
    active = false;

    try {
      // incsearch won't change in the lifetime of this activation
      if (isIncSearchEnabled()) {
        entry.getDocument().removeDocumentListener(incSearchDocumentListener);

        // TODO: Reduce the amount of unnecessary work here
        // If incsearch and hlsearch are enabled, and if this is a search panel, we'll have all of the results correctly
        // highlighted. But because we don't know why we're being closed, and what handler is being called next, we need
        // to reset state. This will remove all highlights and reset back to the last accepted search results. This is
        // fine for <Esc>. But if we hit <Enter>, the search handler will remove the highlights again, perform the same
        // search that we did for incsearch and add highlights back. The `:nohlsearch` command, even if bound to a
        // shortcut, is still processed by the ex entry panel, so deactivating will force update remove, search and add
        // of the current search results before the `NoHLSearchHandler` will remove all highlights again
        final Editor editor = entry.getEditor();
        if (!editor.isDisposed() && resetCaret) {
          resetCaretOffset(editor);
        }

        VimPlugin.getSearch().resetIncsearchHighlights();
      }

      entry.deactivate();
    }
    finally {

      // Make sure we hide the UI, especially if something goes wrong
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
  }

  private void reset() {
    deactivate(false);
    LafManager.getInstance().removeLafManagerListener(this);
  }

  private void resetCaretOffset(@NotNull Editor editor) {
    // Reset the original caret, with original scroll offsets
    final Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
    if (primaryCaret.getOffset() != caretOffset) {
      MotionGroup.moveCaret(editor, primaryCaret, caretOffset);
    }
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    if (scrollingModel.getHorizontalScrollOffset() != horizontalOffset ||
        scrollingModel.getVerticalScrollOffset() != verticalOffset) {
      scrollingModel.scroll(horizontalOffset, verticalOffset);
    }
  }

  @NotNull private final DocumentListener incSearchDocumentListener = new DocumentAdapter() {
    @Override
    protected void textChanged(@NotNull DocumentEvent e) {
      final Editor editor = entry.getEditor();

      boolean searchCommand = false;
      LineRange searchRange = null;
      char separator = label.getText().charAt(0);
      String searchText = entry.getActualText();
      if (label.getText().equals(":")) {
        final ExCommand command = getIncsearchCommand(searchText);
        if (command == null) {
          return;
        }
        searchCommand = true;
        searchText = "";
        final String argument = command.getArgument();
        if (argument.length() > 1) {  // E.g. skip '/' in `:%s/`. `%` is range, `s` is command, `/` is argument
          separator = argument.charAt(0);
          searchText = argument.substring(1);
        }
        if (searchText.length() == 0) {
          VimPlugin.getSearch().resetIncsearchHighlights();
          return;
        }
        searchRange = command.getLineRange(editor);
      }

      final String labelText = label.getText();
      if (labelText.equals("/") || labelText.equals("?") || searchCommand) {
        final boolean forwards = !labelText.equals("?");  // :s, :g, :v are treated as forwards
        final String pattern;
        final CharPointer p = new CharPointer(searchText);
        final CharPointer end = RegExp.skip_regexp(new CharPointer(searchText), separator, true);
        pattern = p.substring(end.pointer() - p.pointer());

        VimPlugin.getEditor().closeEditorSearchSession(editor);
        final int matchOffset = VimPlugin.getSearch().updateIncsearchHighlights(editor, pattern, forwards, caretOffset, searchRange);
        if (matchOffset != -1) {
          MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), matchOffset);
        }
        else {
          resetCaretOffset(editor);
        }
      }
    }

    @Contract("null -> null")
    @Nullable
    private ExCommand getIncsearchCommand(@Nullable String commandText) {
      if (commandText == null) return null;
      try {
        final ExCommand exCommand = CommandParser.getInstance().parse(commandText);
        final String command = exCommand.getCommand();
        // TODO: Add global, vglobal, smagic and snomagic here when the commands are supported
        if ("substitute".startsWith(command)) {
          return exCommand;
        }
      }
      catch(Exception e) {
        logger.warn("Cannot parse command for incsearch", e);
      }

      return null;
    }
  };

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
   * Checks if the ex entry panel is currently active
   *
   * @return true if active, false if not
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Gets the text entered by the user. This includes any initial text but does not include the label
   *
   * @return The user entered text
   */
  @NotNull
  public String getText() {
    return entry.getActualText();
  }

  @NotNull
  public ExTextField getEntry() {
    return entry;
  }

  /**
   * Pass the keystroke on to the text edit for handling
   *
   * @param stroke The keystroke
   */
  public void handleKey(@NotNull KeyStroke stroke) {
    entry.handleKey(stroke);
  }

  @Override
  public void lookAndFeelChanged(@NotNull LafManager source) {
    // Calls updateUI on this and child components
    IJSwingUtilities.updateComponentTreeUI(this);
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

  private void setFontForElements() {
    final Font font = UiHelper.getEditorFont();
    label.setFont(font);
    entry.setFont(font);
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

  private boolean isIncSearchEnabled() {
    return OptionsManager.INSTANCE.getIncsearch().isSet();
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

  private static final Logger logger = Logger.getInstance(ExEntryPanel.class.getName());
}
