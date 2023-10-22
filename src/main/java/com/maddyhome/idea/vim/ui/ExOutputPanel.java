/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.IJSwingUtilities;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.helper.HelperKt;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.helper.UserDataManager;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static com.maddyhome.idea.vim.api.VimInjectorKt.globalOptions;
import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;

/**
 * This panel displays text in a <code>more</code> like window.
 */
public class ExOutputPanel extends JPanel {
  private final @NotNull
  Editor myEditor;

  private final @NotNull
  JLabel myLabel = new JLabel("more");
  private final @NotNull
  JTextArea myText = new JTextArea();
  private final @NotNull
  JScrollPane myScrollPane =
    new JBScrollPane(myText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  private final @NotNull
  ComponentAdapter myAdapter;
  private boolean myAtEnd = false;
  private int myLineHeight = 0;

  private @Nullable
  JComponent myOldGlass = null;
  private @Nullable
  LayoutManager myOldLayout = null;
  private boolean myWasOpaque = false;

  private boolean myActive = false;

  private ExOutputPanel(@NotNull Editor editor) {
    myEditor = editor;

    // Create a text editor for the text and a label for the prompt
    BorderLayout layout = new BorderLayout(0, 0);
    setLayout(layout);
    add(myScrollPane, BorderLayout.CENTER);
    add(myLabel, BorderLayout.SOUTH);

    // Set the text area read only, and support wrap
    myText.setEditable(false);
    myText.setLineWrap(true);

    myAdapter = new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        positionPanel();
      }
    };

    // Setup some listeners to handle keystrokes
    MoreKeyListener moreKeyListener = new MoreKeyListener(this);
    addKeyListener(moreKeyListener);
    myText.addKeyListener(moreKeyListener);

    updateUI();
  }

  public static boolean isPanelActive(@NotNull Editor editor) {
    return UserDataManager.getVimMorePanel(editor) != null;
  }

  public static @NotNull
  ExOutputPanel getInstance(@NotNull Editor editor) {
    ExOutputPanel panel = UserDataManager.getVimMorePanel(editor);
    if (panel == null) {
      panel = new ExOutputPanel(editor);
      UserDataManager.setVimMorePanel(editor, panel);
    }
    return panel;
  }

  private static int countLines(@NotNull String text) {
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

  // Called automatically when the LAF is changed and the component is visible, and manually by the LAF listener handler
  @Override
  public void updateUI() {
    super.updateUI();

    setBorder(new ExPanelBorder());

    // Can be null when called from base constructor
    //noinspection ConstantConditions
    if (myText != null && myLabel != null && myScrollPane != null) {
      setFontForElements();
      myText.setBorder(null);
      myScrollPane.setBorder(null);
      myLabel.setForeground(myText.getForeground());
    }
  }

  public void setText(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String data) {
    if (data.length() > 0 && data.charAt(data.length() - 1) == '\n') {
      data = data.substring(0, data.length() - 1);
    }

    myText.setText(data);
    myText.setFont(UiHelper.selectFont(data));
    myText.setCaretPosition(0);
    if (data.length() > 0) {
      activate();
    }
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public Color getForeground() {
    return myText != null ? myText.getForeground() : super.getForeground();
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public Color getBackground() {
    return myText != null ? myText.getBackground() : super.getBackground();
  }

  /**
   * Turns off the ex entry field and optionally puts the focus back to the original component
   */
  public void deactivate(boolean refocusOwningEditor) {
    if (!myActive) return;
    myActive = false;
    myText.setText("");
    if (refocusOwningEditor) {
      UiHelper.requestFocus(myEditor.getContentComponent());
    }
    if (myOldGlass != null) {
      myOldGlass.removeComponentListener(myAdapter);
      myOldGlass.setVisible(false);
      myOldGlass.remove(this);
      myOldGlass.setOpaque(myWasOpaque);
      myOldGlass.setLayout(myOldLayout);
    }
  }

  /**
   * Turns on the more window for the given editor
   */
  private void activate() {
    JRootPane root = SwingUtilities.getRootPane(myEditor.getContentComponent());
    myOldGlass = (JComponent) root.getGlassPane();
    if (myOldGlass != null) {
      myOldLayout = myOldGlass.getLayout();
      myWasOpaque = myOldGlass.isOpaque();
      myOldGlass.setLayout(null);
      myOldGlass.setOpaque(false);
      myOldGlass.add(this);
      myOldGlass.addComponentListener(myAdapter);
    }

    setFontForElements();
    positionPanel();

    if (myOldGlass != null) {
      myOldGlass.setVisible(true);
    }

    myActive = true;
    UiHelper.requestFocus(myText);
  }

  private void setFontForElements() {
    myText.setFont(UiHelper.selectFont(myText.getText()));
    myLabel.setFont(UiHelper.selectFont(myLabel.getText()));
  }

  private void scrollLine() {
    scrollOffset(myLineHeight);
  }

  private void scrollPage() {
    scrollOffset(myScrollPane.getVerticalScrollBar().getVisibleAmount());
  }

  private void scrollHalfPage() {
    double sa = myScrollPane.getVerticalScrollBar().getVisibleAmount() / 2.0;
    double offset = Math.ceil(sa / myLineHeight) * myLineHeight;
    scrollOffset((int) offset);
  }

  private void handleEnter() {
    if (myAtEnd) {
      close();
    } else {
      scrollLine();
    }
  }

  private void badKey() {
    myLabel.setText(MessageHelper.message("more.ret.line.space.page.d.half.page.q.quit"));
    myLabel.setFont(UiHelper.selectFont(myLabel.getText()));
  }

  private void scrollOffset(int more) {
    myAtEnd = false;
    int val = myScrollPane.getVerticalScrollBar().getValue();
    myScrollPane.getVerticalScrollBar().setValue(val + more);
    myScrollPane.getHorizontalScrollBar().setValue(0);
    if (val + more >=
      myScrollPane.getVerticalScrollBar().getMaximum() - myScrollPane.getVerticalScrollBar().getVisibleAmount()) {
      myAtEnd = true;
      myLabel.setText(MessageHelper.message("hit.enter.or.type.command.to.continue"));
    } else {
      myLabel.setText(MessageHelper.message("ex.output.panel.more"));
    }
    myLabel.setFont(UiHelper.selectFont(myLabel.getText()));
  }

  private void positionPanel() {
    final JComponent contentComponent = myEditor.getContentComponent();
    Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, contentComponent);
    setSize(scroll.getSize());

    myLineHeight = myText.getFontMetrics(myText.getFont()).getHeight();
    int count = countLines(myText.getText());
    int visLines = getSize().height / myLineHeight - 1;
    int lines = Math.min(count, visLines);
    setSize(getSize().width,
      lines * myLineHeight + myLabel.getPreferredSize().height + getBorder().getBorderInsets(this).top * 2);

    int height = getSize().height;
    Rectangle bounds = scroll.getBounds();
    bounds.translate(0, scroll.getHeight() - height);
    bounds.height = height;
    Point pos = SwingUtilities.convertPoint(scroll.getParent(), bounds.getLocation(),
      SwingUtilities.getRootPane(contentComponent).getGlassPane());
    bounds.setLocation(pos);
    setBounds(bounds);

    myScrollPane.getVerticalScrollBar().setValue(0);
    if (!globalOptions(injector).getMore()) {
      // FIX
      scrollOffset(100000);
    } else {
      scrollOffset(0);
    }
  }

  private void close() {
    close(null);
  }

  private void close(final @Nullable KeyEvent e) {
    ApplicationManager.getApplication().invokeLater(() -> {
      deactivate(true);

      final Project project = myEditor.getProject();

      if (project != null && e != null && e.getKeyChar() != '\n') {
        final KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
        final List<KeyStroke> keys = new ArrayList<>(1);
        keys.add(key);
        KeyHandler.getInstance().getKeyStack().addKeys(keys);
        ExecutionContext.Editor context = injector.getExecutionContextManager().onEditor(new IjVimEditor(myEditor), null);
        VimPlugin.getMacro().playbackKeys(new IjVimEditor(myEditor), context, 1);
      }
    });
  }

  private static class MoreKeyListener extends KeyAdapter {
    private final ExOutputPanel myExOutputPanel;

    public MoreKeyListener(ExOutputPanel panel) {
      this.myExOutputPanel = panel;
    }

    /**
     * Invoked when a key has been pressed.
     */
    @Override
    public void keyTyped(@NotNull KeyEvent e) {
      if (myExOutputPanel.myAtEnd) {
        myExOutputPanel.close(e);
      } else {
        switch (e.getKeyChar()) {
          case ' ':
            myExOutputPanel.scrollPage();
            break;
          case 'd':
            myExOutputPanel.scrollHalfPage();
            break;
          case 'q':
          case '\u001b':
            myExOutputPanel.close();
            break;
          case '\n':
            myExOutputPanel.handleEnter();
            break;
          case KeyEvent.CHAR_UNDEFINED: {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_ENTER:
                myExOutputPanel.handleEnter();
                break;
              case KeyEvent.VK_ESCAPE:
                myExOutputPanel.close();
                break;
              default:
                myExOutputPanel.badKey();
            }
          }
          default:
            myExOutputPanel.badKey();
        }
      }
    }
  }

  public static class LafListener implements LafManagerListener {
    @Override
    public void lookAndFeelChanged(@NotNull LafManager source) {
      if (!VimPlugin.isEnabled()) return;
      // Calls updateUI on this and child components
      for (Editor editor : HelperKt.localEditors()) {
        if (!ExOutputPanel.isPanelActive(editor)) continue;
        IJSwingUtilities.updateComponentTreeUI(ExOutputPanel.getInstance(editor));
      }
    }
  }
}
