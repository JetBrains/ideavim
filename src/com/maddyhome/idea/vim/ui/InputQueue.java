package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayDeque;
import java.util.List;

/**
 * @author dhleong
 */
public class InputQueue {

  private static InputQueue instance = new InputQueue();

  private ArrayDeque<KeyStroke> queue = new ArrayDeque<KeyStroke>();

  private InputQueue() {}

  public static @Nullable KeyStroke dequeue() {
    if (instance.queue.isEmpty()) {
      return null;
    }
    return instance.queue.pop();
  }

  /**
   * Execute every KeyStroke enqueued as if in normal mode
   */
  public static void executeNormal(@NotNull Editor editor, @NotNull DataContext context) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {

      KeyStroke key;
      while ((key = dequeue()) != null) {
        KeyHandler.getInstance().handleKey(editor, key, context);
      }

    } else {

      // when not in a Test, we need to invokeLater each
      //  instance of handleKey, so that any mapping actions
      //  can appropriately happen "before" the extra strokes.
      //  NB: Multiple recursive mappings might still break....
      new DrainTask(editor, context).run();
    }
  }

  public static void enqueue(@NotNull List<KeyStroke> strokes) {
    instance.queue.addAll(strokes);
  }

  public static void insert(List<KeyStroke> keys) {
    final int size = keys.size();
    for (int i = size-1; i >= 0; i--) {
      instance.queue.addFirst(keys.get(i));
    }
  }

  static class DrainTask implements Runnable {

    private final Editor myEditor;
    private final DataContext myContext;

    public DrainTask(Editor editor, DataContext context) {
      myEditor = editor;
      myContext = context;
    }

    @Override
    public void run() {
      KeyStroke key = dequeue();
      if (key != null) {
        KeyHandler.getInstance().handleKey(myEditor, key, myContext);

        final Application application = ApplicationManager.getApplication();
        application.invokeLater(this);
      }
    }
  }
}
