package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.helper.StringHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * @author dhleong
 */
public class ModalEntry {

  public interface OnKeyStrokeHandler {
    /**
     * Called on each intercepted KeyStroke.
     *  Return `true` to continue receiving
     *  KeyStrokes, or `false` when done.
     */
    boolean onKeyStroke(KeyStroke stroke);
  }

  /**
   * Activate modal entry mode, passing all received
   *  KeyStrokes to `handler` until it returns false.
   */
  public static void activate(final OnKeyStrokeHandler handler) {

    final SecondaryLoopCompat loop = SecondaryLoopCompat.newInstance();

    KeyboardFocusManager.getCurrentKeyboardFocusManager()
      .addKeyEventDispatcher(new KeyEventDispatcher() {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
          KeyStroke stroke;
          if (e.getID() == KeyEvent.KEY_RELEASED) {
            stroke = KeyStroke.getKeyStrokeForEvent(e);
            if (!StringHelper.isCloseKeyStroke(stroke)
                && stroke.getKeyCode() != KeyEvent.VK_ENTER) {
              return false;
            }
          } else if (e.getID() == KeyEvent.KEY_TYPED) {
            // always
            stroke = KeyStroke.getKeyStrokeForEvent(e);
          } else {
            return false;
          }

          if (!handler.onKeyStroke(stroke)) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
              .removeKeyEventDispatcher(this);
            loop.exit();
          }
          return true;
        }
      });

    loop.enter();
  }

  /**
   * Convenience method to request a single KeyStroke
   */
  public static KeyStroke single() {
    final Ref<KeyStroke> ref = Ref.create();
    activate(new OnKeyStrokeHandler() {
      @Override
      public boolean onKeyStroke(KeyStroke stroke) {
        ref.set(stroke);
        return false;
      }
    });
    return ref.get();
  }
}
