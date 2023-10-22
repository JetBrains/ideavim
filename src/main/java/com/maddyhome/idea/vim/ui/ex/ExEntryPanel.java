/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex;

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
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.helper.SearchHighlightsHelper;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.regexp.CharPointer;
import com.maddyhome.idea.vim.regexp.RegExp;
import com.maddyhome.idea.vim.ui.ExPanelBorder;
import com.maddyhome.idea.vim.vimscript.model.commands.Command;
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand;
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser;
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

import static com.maddyhome.idea.vim.api.VimInjectorKt.globalOptions;
import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;

/**
 * This is used to enter ex commands such as searches and "colon" commands
 */
public class ExEntryPanel extends JPanel {
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

  public static boolean isInstanceWithShortcutsActive() {
    return instance != null;
  }

  public static boolean isInstanceWithoutShortcutsActive() {
    return instanceWithoutShortcuts != null;
  }

  public static void fullReset() {
    if (isInstanceWithShortcutsActive()) {
      instance.reset();
      instance = null;
    }
    if (isInstanceWithoutShortcutsActive()) {
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
    logger.info("Activate ex entry panel");
    this.label.setText(label);
    this.label.setFont(UiHelper.selectFont(label));
    this.count = count;
    entry.reset();
    entry.setEditor(editor, context);
    entry.setText(initText);
    entry.setFont(UiHelper.selectFont(initText));
    entry.setType(label);
    parent = editor.getContentComponent();

    entry.getDocument().addDocumentListener(fontListener);
    if (isIncSearchEnabled()) {
      entry.getDocument().addDocumentListener(incSearchDocumentListener);
      caretOffset = editor.getCaretModel().getOffset();
      verticalOffset = editor.getScrollingModel().getVerticalScrollOffset();
      horizontalOffset = editor.getScrollingModel().getHorizontalScrollOffset();
    }

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      JRootPane root = SwingUtilities.getRootPane(parent);
      if (root == null) return;
      oldGlass = (JComponent) root.getGlassPane();
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
      entry.getDocument().removeDocumentListener(fontListener);
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
    } finally {

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
    JTextField.removeKeymap(ExTextField.KEYMAP_NAME);
  }

  private void resetCaretOffset(@NotNull Editor editor) {
    // Reset the original caret, with original scroll offsets
    final Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
    if (primaryCaret.getOffset() != caretOffset) {
      new IjVimCaret(primaryCaret).moveToOffset(caretOffset);
    }
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    if (scrollingModel.getHorizontalScrollOffset() != horizontalOffset ||
      scrollingModel.getVerticalScrollOffset() != verticalOffset) {
      scrollingModel.scroll(horizontalOffset, verticalOffset);
    }
  }

  private final @NotNull
  DocumentListener fontListener = new DocumentAdapter() {
    @Override
    protected void textChanged(@NotNull DocumentEvent e) {
      String text = entry.getActualText();
      Font newFont = UiHelper.selectFont(text);
      if (newFont != entry.getFont()) {
        entry.setFont(newFont);
      }
    }
  };

  private final @NotNull
  DocumentListener incSearchDocumentListener = new DocumentAdapter() {
    @Override
    protected void textChanged(@NotNull DocumentEvent e) {
      final Editor editor = entry.getEditor();

      boolean searchCommand = false;
      LineRange searchRange = null;
      char separator = label.getText().charAt(0);
      String searchText = entry.getActualText();
      if (label.getText().equals(":")) {
        if (searchText.isEmpty()) return;
        final Command command = getIncsearchCommand(searchText);
        if (command == null) {
          return;
        }
        searchCommand = true;
        searchText = "";
        final String argument = command.getCommandArgument();
        if (argument.length() > 1) {  // E.g. skip '/' in `:%s/`. `%` is range, `s` is command, `/` is argument
          separator = argument.charAt(0);
          searchText = argument.substring(1);
        }
        if (searchText.length() == 0) {
          // Reset back to the original search highlights after deleting a search from a substitution command.
          // E.g. Highlight `whatever`, type `:%s/foo` + highlight `foo`, delete back to `:%s/` and reset highlights
          // back to `whatever`
          VimPlugin.getSearch().resetIncsearchHighlights();
          return;
        }
        searchRange = command.getLineRange(new IjVimEditor(editor));
      }

      final String labelText = label.getText();
      if (labelText.equals("/") || labelText.equals("?") || searchCommand) {
        final boolean forwards = !labelText.equals("?");  // :s, :g, :v are treated as forwards
        final String pattern;
        final CharPointer p = new CharPointer(searchText);
        final CharPointer end = RegExp.skip_regexp(new CharPointer(searchText), separator, true);
        pattern = p.substring(end.pointer() - p.pointer());

        VimPlugin.getEditor().closeEditorSearchSession(editor);
        final int matchOffset = SearchHighlightsHelper.updateIncsearchHighlights(editor, pattern, forwards, caretOffset, searchRange);
        if (matchOffset != -1) {
          new IjVimCaret(editor.getCaretModel().getPrimaryCaret()).moveToOffset(matchOffset);
        } else {
          resetCaretOffset(editor);
        }
      }
    }

    @Contract("null -> null")
    private @Nullable
    Command getIncsearchCommand(@Nullable String commandText) {
      if (commandText == null) return null;
      try {
        final Command exCommand = VimscriptParser.INSTANCE.parseCommand(commandText);
        // TODO: Add global, vglobal, smagic and snomagic here when the commands are supported
        if (exCommand instanceof SubstituteCommand) {
          return exCommand;
        }
      } catch (Exception e) {
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
  public @NotNull
  String getText() {
    return entry.getActualText();
  }

  public @NotNull
  ExTextField getEntry() {
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
    label.setFont(UiHelper.selectFont(label.getText()));
    entry.setFont(UiHelper.selectFont(entry.getActualText()));
  }

  private void positionPanel() {
    if (parent == null) return;

    Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, parent);
    int height = (int) getPreferredSize().getHeight();
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
    return globalOptions(injector).getIncsearch();
  }

  private boolean active;
  private int count;

  // UI stuff
  private @Nullable
  JComponent parent;
  private final @NotNull
  JLabel label;
  private final @NotNull
  ExTextField entry;
  private JComponent oldGlass;
  private LayoutManager oldLayout;
  private boolean wasOpaque;

  // incsearch stuff
  private int verticalOffset;
  private int horizontalOffset;
  private int caretOffset;

  private final @NotNull
  ComponentListener resizePanelListener = new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
      positionPanel();
    }
  };

  private static final Logger logger = Logger.getInstance(ExEntryPanel.class.getName());

  public static class LafListener implements LafManagerListener {
    @Override
    public void lookAndFeelChanged(@NotNull LafManager source) {
      if (!VimPlugin.isEnabled()) return;
      // Calls updateUI on this and child components
      if (ExEntryPanel.isInstanceWithShortcutsActive()) {
        IJSwingUtilities.updateComponentTreeUI(ExEntryPanel.getInstance());
      }
      if (ExEntryPanel.isInstanceWithoutShortcutsActive()) {
        IJSwingUtilities.updateComponentTreeUI(ExEntryPanel.getInstanceWithoutShortcuts());
      }
    }
  }
}
