package com.maddyhome.idea.vim.ui;

import com.intellij.util.Processor;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * @author dhleong
 */
public final class ModalEntry {
  private ModalEntry() {}

  /**
   * Activates modal entry mode, passing all received key strokes to the processor until it returns false.
   */
  public static void activate(@NotNull final Processor<KeyStroke> processor) {
    final EventQueue systemQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    final SecondaryLoop loop = systemQueue.createSecondaryLoop();

    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
      @Override
      public boolean dispatchKeyEvent(KeyEvent e) {
        final KeyStroke stroke;
        if (e.getID() == KeyEvent.KEY_RELEASED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e);
          if (!StringHelper.isCloseKeyStroke(stroke) && stroke.getKeyCode() != KeyEvent.VK_ENTER) {
            return true;
          }
        } else if (e.getID() == KeyEvent.KEY_TYPED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e);
        } else {
          return true;
        }
        if (!processor.process(stroke)) {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
          loop.exit();
        }
        return true;
      }
    });

    loop.enter();
  }
}
