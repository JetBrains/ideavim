package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.util.Ref;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

/**
 * @author dhleong
 */
public abstract class SecondaryLoopCompat {
  static Method jdk7Factory;
  static {
    try {
      jdk7Factory = EventQueue.class.getMethod("createSecondaryLoop");
    }
    catch (NoSuchMethodException e) {
      // on jdk6; no native SecondaryLoop
    }
  }

  /**
   * Enter a secondary event loop,
   *  blocking the current thread until
   *  {@link #exit()} is called. KeyEvents
   *  will be fed to the provided KeyEventDispatcher
   *  while the secondary event loop is active.
   */
  public abstract void enter(KeyEventDispatcher dispatcher);

  /**
   * Exit the secondary event loop, letting
   *  the thread that called {@link #enter(KeyEventDispatcher)}
   *  continue execution. This instance will no longer
   *  be usable once you exit, so you should acquire
   *  a new instance each time you need this functionality.
   */
  public abstract void exit();

  static SecondaryLoopCompat getInstance() {
    // NB: If the JDK7 SecondaryLoop class is available,
    //  we HAVE to use it, due to a bug in the JDK:
    // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8144759
    if (jdk7Factory != null) {
      try {
        final EventQueue systemQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        return new Jdk7SecondaryLoopCompat(jdk7Factory.invoke(systemQueue));
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException("On JDK7 but couldn't instantiate JDK7 compat", e);
      }
      catch (InvocationTargetException e) {
        throw new RuntimeException("On JDK7 but couldn't instantiate JDK7 compat", e);
      }
    } else {
      return new Jdk6SecondaryLoopCompat();
    }
  }



  private static class Jdk6SecondaryLoopCompat extends SecondaryLoopCompat {
    final VimEventQueue secondaryQueue = new VimEventQueue();
    final CountDownLatch latch = new CountDownLatch(1);
    final Ref<Boolean> active = Ref.create(true);
    final EventQueue systemQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();

    @Override
    public void enter(final KeyEventDispatcher dispatcher) {
      System.out.println("Using jdk6 SecondaryLoopCompat");

      // make our secondaryQueue the current one
      systemQueue.push(secondaryQueue);

      // hop onto a thread to consume events passed to
      //  our secondary queue
      new SwingWorker() {
        @Override
        protected Void doInBackground() throws Exception {
          while (active.get()) {
            AWTEvent event = secondaryQueue.getNextEvent();
            if (event instanceof KeyEvent) {
              System.out.println(event);
              dispatcher.dispatchKeyEvent((KeyEvent)event);
            }

            Thread.yield();
          }

          System.out.println("Exit dispatch");
          return null;
        }
      }.execute();

      // wait for exit() to be called
      try {
        latch.await();
      }
      catch (InterruptedException e) {
        // shouldn't happen
        e.printStackTrace();
      }
    }

    @Override
    public void exit() {
      secondaryQueue.pop();
      active.set(false);
      latch.countDown();
    }
  }

  private static class Jdk7SecondaryLoopCompat extends SecondaryLoopCompat {
    static Method enter, exit;
    static {
      try {
        Class<?> secondaryLoopClass = Class.forName("java.awt.SecondaryLoop");
        enter = secondaryLoopClass.getMethod("enter");
        exit = secondaryLoopClass.getMethod("exit");
      }
      catch (ClassNotFoundException e) {
        // NB: Shouldn't happen, but if it does,
        //  this class shouldn't get instantiated anyway
        e.printStackTrace();
      }
      catch (NoSuchMethodException e) {
        // see above
        e.printStackTrace();
      }
    }

    private Object mySecondaryLoop;
    private KeyEventDispatcher dispatcher;

    public Jdk7SecondaryLoopCompat(Object secondaryLoop) {
      mySecondaryLoop = secondaryLoop;

      if (enter == null || exit == null) {
        throw new IllegalStateException(
          "Couldn't find enter() and exit() methods for JDK7 Compat");
      }
    }

    @Override
    public void enter(KeyEventDispatcher dispatcher) {
      System.out.println("Using jdk7 SecondaryLoopCompat");

      // on JDK7 we can simply add the dispatcher
      this.dispatcher = dispatcher;
      KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addKeyEventDispatcher(dispatcher);

      try {
        enter.invoke(mySecondaryLoop);
      }
      catch (IllegalAccessException e) {
        // NB: Shouldn't happen
        e.printStackTrace();
      }
      catch (InvocationTargetException e) {
        // NB: Shouldn't happen
        e.printStackTrace();
      }
    }

    @Override
    public void exit() {
      try {
        exit.invoke(mySecondaryLoop);
      }
      catch (IllegalAccessException e) {
        // NB: Shouldn't happen
        e.printStackTrace();
      }
      catch (InvocationTargetException e) {
        // NB: Shouldn't happen
        e.printStackTrace();
      }

      // ensure it's gone
      KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .removeKeyEventDispatcher(dispatcher);
    }
  }
}
