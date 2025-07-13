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
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.IJSwingUtilities;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimShortcutKeyAction;
import com.maddyhome.idea.vim.api.VimCommandLine;
import com.maddyhome.idea.vim.api.VimCommandLineCaret;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimKeyGroupBase;
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.helper.EngineModeExtensionsKt;
import com.maddyhome.idea.vim.helper.IJVimKeyHelperKt;
import com.maddyhome.idea.vim.helper.SearchHighlightsHelper;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.key.VimKeyStroke;
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptor;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.ui.ExPanelBorder;
import com.maddyhome.idea.vim.vimscript.model.commands.Command;
import com.maddyhome.idea.vim.vimscript.model.commands.GlobalCommand;
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand;
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.ref.WeakReference;

import static com.maddyhome.idea.vim.api.VimInjectorKt.globalOptions;
import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.group.KeyGroup.toShortcutSet;

/**
 * This is used to enter ex commands such as searches and "colon" commands
 */
public class ExEntryPanel extends JPanel implements VimCommandLine {
  public static @Nullable ExEntryPanel instance;

  public boolean isReplaceMode = false;
  public Function1<String, Unit> inputProcessing = null;
  public Character finishOn = null;

  private VimInputInterceptor myInputInterceptor = null;
  private WeakReference<Editor> weakEditor = null;
  private DataContext context = null;
  private int histIndex = 0;
  private String lastEntry;

  private ExEntryPanel() {
    label = new JLabel(" ");
    entry = new ExTextField(this);

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

    // This does not need to be unregistered, it's registered as a custom UI property on this
    EventFacade.getInstance().registerCustomShortcutSet(VimShortcutKeyAction.getInstance(), toShortcutSet(
      ((VimKeyGroupBase)injector.getKeyGroup()).getRequiredShortcutKeys()), entry);

    updateUI();
  }

  public static ExEntryPanel getOrCreateInstance() {
    if (instance == null) {
      instance = new ExEntryPanel();
    }

    return instance;
  }

  public static void fullReset() {
    if (instance != null) {
      instance.reset();
      instance = null;
    }
  }

  public @Nullable Editor getIjEditor() {
    return weakEditor != null ? weakEditor.get() : null;
  }

  public @NotNull VimEditor getEditor() {
    Editor editor = getIjEditor();
    if (editor == null) {
      throw new RuntimeException("Editor was disposed for active command line");
    }
    return new IjVimEditor(editor);
  }

  public void setEditor(@Nullable Editor editor) {
    if (editor == null) {
      weakEditor = null;
    }
    else {
      weakEditor = new WeakReference<>(editor);
    }
  }

  public DataContext getContext() {
    return context;
  }

  public void setContext(DataContext context) {
    this.context = context;
  }

  /**
   * @deprecated use {@link ExEntryPanel#activate(Editor, DataContext, String, String)}
   */
  @Deprecated(forRemoval = true)
  public void activate(@NotNull Editor editor, DataContext context, @NotNull String label, String initText, int count) {
    activate(editor, context, label, initText);
  }

  /**
   * Turns on the ex entry field for the given editor
   *
   * @param editor   The editor to use for display
   * @param context  The data context
   * @param label    The label for the ex entry (i.e. :, /, or ?)
   * @param initText The initial text for the entry
   */
  public void activate(@NotNull Editor editor, DataContext context, @NotNull String label, String initText) {
    logger.info("Activate ex entry panel");
    this.label.setText(label);
    this.label.setFont(UiHelper.selectEditorFont(editor, label));
    entry.reset();
    entry.setText(initText);
    entry.setFont(UiHelper.selectEditorFont(editor, initText));
    parent = editor.getContentComponent();

    entry.setForeground(editor.getColorsScheme().getDefaultForeground());
    // TODO: Introduce IdeaVim colour scheme for "SpecialKey"?
    entry.setSpecialKeyForeground(editor.getColorsScheme().getColor(EditorColors.WHITESPACES_COLOR));
    this.label.setForeground(entry.getForeground());

    this.context = context;
    setEditor(editor);

    setHistIndex(VimPlugin.getHistory().getEntries(getHistoryType(), 0, 0).size());

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

  public void deactivate(boolean refocusOwningEditor) {
    deactivate(refocusOwningEditor, true);
  }

  /**
   * Turns off the ex entry field and optionally puts the focus back to the original component
   */
  @Override
  public void deactivate(boolean refocusOwningEditor, boolean resetCaret) {
    logger.info("Deactivate ex entry panel");
    if (!active) return;

    clearPromptCharacter();
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
        final Editor editor = getIjEditor();
        if (editor != null && !editor.isDisposed() && resetCaret) {
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

    isReplaceMode = false;
    setEditor(null);
    context = null;

    // We have this in the end, because `entry.deactivate()` communicates with active panel during deactivation
    active = false;
    finishOn = null;
    myInputInterceptor = null;
    inputProcessing = null;
  }

  private void reset() {
    deactivate(false);
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

  private final @NotNull DocumentListener fontListener = new DocumentAdapter() {
    @Override
    protected void textChanged(@NotNull DocumentEvent e) {
      String text = entry.getText();
      Font newFont = UiHelper.selectEditorFont(getIjEditor(), text);
      if (newFont != entry.getFont()) {
        entry.setFont(newFont);
      }
    }
  };

  private final @NotNull DocumentListener incSearchDocumentListener = new DocumentAdapter() {
    @Override
    protected void textChanged(@NotNull DocumentEvent e) {
      try {
        final Editor editor = getIjEditor();
        if (editor == null) {
          return;
        }

        final String labelText = label.getText(); // Either '/', '?' or ':'boolean searchCommand = false;

        boolean searchCommand = false;
        LineRange searchRange = null;
        char separator = labelText.charAt(0);
        String searchText = getText();
        if (labelText.equals(":")) {
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
          if (!searchText.isEmpty()) {
            searchRange = command.getLineRangeSafe(new IjVimEditor(editor));
          }
          if (searchText.isEmpty() || searchRange == null) {
            // Reset back to the original search highlights after deleting a search from a substitution command.Or if
            // there is no search range (because the user entered an invalid range, e.g. mark not set).
            // E.g. Highlight `whatever`, type `:%s/foo` + highlight `foo`, delete back to `:%s/` and reset highlights
            // back to `whatever`
            VimPlugin.getSearch().resetIncsearchHighlights();
            resetCaretOffset(editor);
            return;
          }
        }

        // Get a snapshot of the count for the in progress command, and coerce it to 1. This value will include all
        // count components - selecting register(s), operator and motions. E.g. `2"a3"b4"c5d6/` will return 720.
        // If we're showing highlights for an Ex command like `:s`, the command builder will be empty, but we'll still
        // get a valid value.
        int count1 = Math.max(1, KeyHandler.getInstance().getKeyHandlerState().getEditorCommandBuilder()
          .calculateCount0Snapshot());

        if (labelText.equals("/") || labelText.equals("?") || searchCommand) {
          final boolean forwards = !labelText.equals("?");  // :s, :g, :v are treated as forwards
          int patternEnd = injector.getSearchGroup().findEndOfPattern(searchText, separator, 0);
          final String pattern = searchText.substring(0, patternEnd);

          VimPlugin.getEditor().closeEditorSearchSession(editor);
          final int matchOffset =
            SearchHighlightsHelper.updateIncsearchHighlights(editor, pattern, count1, forwards, caretOffset,
                                                             searchRange);
          if (matchOffset != -1) {
            // Moving the caret will update the Visual selection, which is only valid while performing a search. We want
            // to remove the Visual selection when the incsearch is for a command, as this is always unrelated to the
            // current selection.
            // E.g. `V/foo` should update the selection to the location of the search result. But `V` followed by
            // `:<C-U>%s/foo` should remove the selection first.
            // We're actually in Command-line with Visual pending. Exiting Visual replaces this with just Command-line
            if (searchCommand) {
              EngineModeExtensionsKt.exitVisualMode(new IjVimEditor(editor));
            }
            new IjVimCaret(editor.getCaretModel().getPrimaryCaret()).moveToOffset(matchOffset);
          }
          else {
            resetCaretOffset(editor);
          }
        }
      }
      catch (Throwable ex) {
        // Make sure the exception doesn't leak out of the handler, because it can break the text entry field and
        // require the editor to be closed/reopened. The worst that will happen is no incsearch highlights
        logger.error("Error while trying to show incsearch highlights", ex);
      }
    }

    @Contract("null -> null")
    private @Nullable Command getIncsearchCommand(@Nullable String commandText) {
      if (commandText == null) return null;
      try {
        final Command exCommand = VimscriptParser.INSTANCE.parseCommand(commandText);
        // TODO: Add smagic and snomagic here if/when the commands are supported
        if (exCommand instanceof SubstituteCommand || exCommand instanceof GlobalCommand) {
          return exCommand;
        }
      }
      catch (Exception e) {
        logger.error("Cannot parse command for incsearch", e);
      }

      return null;
    }
  };

  /**
   * Gets the label for the ex entry. This should be one of ":", "/", or "?"
   *
   * @return The ex entry label
   */
  @Override
  public @NotNull String getLabel() {
    return label.getText();
  }

  @Override
  public void toggleReplaceMode() {
    entry.toggleInsertReplace();
  }

  /**
   * Checks if the ex entry panel is currently active
   *
   * @return true if active, false if not
   */
  public boolean isActive() {
    return active;
  }

  @Override
  public @NotNull String getText() {
    return entry.getText();
  }

  @Override
  public @NotNull String getRenderedText() {
    final StringBuilder stringBuilder = new StringBuilder();
    getRenderedText(entry.getUI().getRootView(entry), stringBuilder);
    if (stringBuilder.charAt(stringBuilder.length() - 1) == '\n') {
      stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    }
    return stringBuilder.toString();
  }

  private void getRenderedText(View view, StringBuilder stringBuilder) {
    if (view.getElement().isLeaf()) {
      if (view instanceof GlyphView glyphView) {
        final Segment text = glyphView.getText(glyphView.getStartOffset(), glyphView.getEndOffset());
        stringBuilder.append(text);

        // GlyphView doesn't render a trailing new line, but uses it to flush the characters in the preceding string
        // Typically, we won't get a newline in the middle of a string, but we do add the prompt to the end of the doc
        if (stringBuilder.charAt(stringBuilder.length() - 1) == '\n') {
          stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
      }
      else {
        stringBuilder.append("<Unknown leaf view. Expected GlyphView, got: ");
        stringBuilder.append(view.getClass().getName());
        stringBuilder.append(">");
      }
    }
    else {
      final int viewCount = view.getViewCount();
      for (int i = 0; i < viewCount; i++) {
        final View child = view.getView(i);
        getRenderedText(child, stringBuilder);
      }
    }
  }

  @Override
  public void setPromptCharacter(char promptCharacter) {
    if (entry.getUI() instanceof ExTextFieldUI exTextFieldUI) {
      exTextFieldUI.setPromptCharacter(promptCharacter);
    }
  }

  @Override
  public void clearPromptCharacter() {
    if (entry.getUI() instanceof ExTextFieldUI exTextFieldUI) {
      exTextFieldUI.clearPromptCharacter();
    }
  }

  public @NotNull ExTextField getEntry() {
    return entry;
  }

  /**
   * Pass the keystroke on to the text field for handling
   * <p>
   * The text field for the command line will forward a pressed or typed keystroke to the key handler, which will either
   * consume it for mapping or a command. If it's not consumed, or if it's mapped, the keystroke is returned to the
   * command line to complete handling. This includes typed characters as well as pressed shortcuts.
   * </p>
   *
   * @param stroke The potentially mapped keystroke
   */
  @Override
  public void handleKey(@NotNull VimKeyStroke stroke) {
    entry.handleKey(IJVimKeyHelperKt.getKeyStroke(stroke));
    if (finishOn != null && stroke.getKeyChar() == finishOn && inputProcessing != null) {
      inputProcessing.invoke(getText());
      close(true, true);
    }
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

      // Make sure the panel is positioned correctly if we're changing font size
      positionPanel();
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
    label.setFont(UiHelper.selectEditorFont(getIjEditor(), label.getText()));
    entry.setFont(UiHelper.selectEditorFont(getIjEditor(), entry.getText()));
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
    return globalOptions(injector).getIncsearch();
  }

  private boolean active;

  // UI stuff
  private @Nullable JComponent parent;
  private final @NotNull JLabel label;
  private final @NotNull ExTextField entry;
  private JComponent oldGlass;
  private LayoutManager oldLayout;
  private boolean wasOpaque;

  // incsearch stuff
  private int verticalOffset;
  private int horizontalOffset;
  private int caretOffset;

  private final @NotNull ComponentListener resizePanelListener = new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
      positionPanel();
    }
  };

  private static final Logger logger = Logger.getInstance(ExEntryPanel.class.getName());

  @Override
  public @NotNull VimCommandLineCaret getCaret() {
    return (VimCommandLineCaret)entry.getCaret();
  }

  @Override
  public void setText(@NotNull String string, boolean updateLastEntry) {
    // It's a feature of Swing that caret is moved when we set new text. However, our API is Swing independent and we do not expect this
    int offset = getCaret().getOffset();
    entry.updateText(string);
    if (updateLastEntry) entry.saveLastEntry();
    getCaret().setOffset(Math.min(offset, getText().length()));
  }

  @Override
  public void deleteText(int offset, int length) {
    entry.deleteText(offset, length);
  }

  @Override
  public void insertText(int offset, @NotNull String string) {
    // Remember that replace mode is different to overwrite! The document handles overwrite, but we must handle replace
    if (isReplaceMode) {
      entry.deleteText(offset, string.length());
    }
    entry.insertText(offset, string);
  }

  @Override
  public void clearCurrentAction() {
    entry.clearCurrentAction();
  }

  @Override
  public boolean isReplaceMode() {
    return isReplaceMode;
  }

  @Override
  public void focus() {
    IdeFocusManager.findInstance().requestFocus(entry, true);
  }

  public @Nullable VimInputInterceptor getInputInterceptor() {
    return myInputInterceptor;
  }

  public void setInputInterceptor(@Nullable VimInputInterceptor vimInputInterceptor) {
    myInputInterceptor = vimInputInterceptor;
  }

  @Override
  public @Nullable Function1<String, Unit> getInputProcessing() {
    return inputProcessing;
  }

  @Override
  public @Nullable Character getFinishOn() {
    return finishOn;
  }

  @Override
  public int getHistIndex() {
    return histIndex;
  }

  @Override
  public void setHistIndex(int i) {
    histIndex = i;
  }

  @NotNull
  @Override
  public String getLastEntry() {
    return lastEntry;
  }

  @Override
  public void setLastEntry(@NotNull String s) {
    lastEntry = s;
  }

  public static class LafListener implements LafManagerListener {
    @Override
    public void lookAndFeelChanged(@NotNull LafManager source) {
      if (VimPlugin.isNotEnabled()) return;

      // Calls updateUI on this and child components
      if (instance != null) {
        IJSwingUtilities.updateComponentTreeUI(instance);
      }
    }
  }
}
