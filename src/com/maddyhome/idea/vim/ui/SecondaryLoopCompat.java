package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.util.Ref;

import java.awt.*;
import java.lang.reflect.*;

/**
 * TODO: Drop this compat layer and use the real SecondaryLoop for IntelliJ 16+ (transition to Java 8)
 *
 * @author dhleong
 */
abstract class SecondaryLoopCompat {
  private static Method jdk7Factory;
  static {
    try {
      jdk7Factory = EventQueue.class.getMethod("createSecondaryLoop");
    }
    catch (NoSuchMethodException ignored) {
    }
  }

  /**
   * Enter a secondary event loop, blocking the current thread until {@link #exit()} is called.
   */
  public abstract void enter();

  /**
   * Exit the secondary event loop, letting the thread that called {@link #enter()} continue execution. This instance
   * will no longer be usable once you exit, so you should acquire a new instance each time you need this functionality.
   */
  public abstract void exit();

  static SecondaryLoopCompat newInstance() {
    // NB: If the JDK7 SecondaryLoop class is available we HAVE to use it, due to a bug in the JDK:
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

  /**
   * @deprecated Drop for IntelliJ 16+ (transition to Java 8).
   */
  @Deprecated
  private static class Jdk6SecondaryLoopCompat extends SecondaryLoopCompat {
    final static Field dispatchThreadField;
    final static Class<?> conditionalClass;
    final static Method pumpEventsMethod;
    static {
      try {
        dispatchThreadField = EventQueue.class.getDeclaredField("dispatchThread");
        dispatchThreadField.setAccessible(true);
        conditionalClass = Class.forName("java.awt.Conditional");
        final Class<?> eventDispatchThreadClass = Class.forName("java.awt.EventDispatchThread");
        pumpEventsMethod = eventDispatchThreadClass.getDeclaredMethod("pumpEvents", conditionalClass);
        pumpEventsMethod.setAccessible(true);
      }
      catch (NoSuchFieldException e) {
        throw new IllegalStateException(e);
      }
      catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
      catch (NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }

    final Ref<Boolean> active = Ref.create(true);
    final EventQueue systemQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();

    final Object conditionalProxy = Proxy.newProxyInstance(
      EventQueue.class.getClassLoader(),
      new Class[]{conditionalClass},
      new InvocationHandler() {
        @Override
        public Object invoke(Object o, Method method, Object[] args) throws Throwable {
          return active.get();
        }
      });

    @Override
    public void enter() {
      try {
        final Object dispatchThread = dispatchThreadField.get(systemQueue);
        pumpEventsMethod.invoke(dispatchThread, conditionalProxy);
      }
      catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
      catch (InvocationTargetException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void exit() {
      active.set(false);
    }
  }

  private static class Jdk7SecondaryLoopCompat extends SecondaryLoopCompat {
    static Method enter, exit;
    static {
      try {
        final Class<?> secondaryLoopClass = Class.forName("java.awt.SecondaryLoop");
        enter = secondaryLoopClass.getMethod("enter");
        exit = secondaryLoopClass.getMethod("exit");
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }

    final private Object mySecondaryLoop;

    Jdk7SecondaryLoopCompat(Object secondaryLoop) {
      mySecondaryLoop = secondaryLoop;

      if (enter == null || exit == null) {
        throw new IllegalStateException(
          "Couldn't find enter() and exit() methods for JDK7 Compat");
      }
    }

    @Override
    public void enter() {
      try {
        enter.invoke(mySecondaryLoop);
      }
      catch (IllegalAccessException e) {
        e.printStackTrace();
      }
      catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void exit() {
      try {
        exit.invoke(mySecondaryLoop);
      }
      catch (IllegalAccessException e) {
        e.printStackTrace();
      }
      catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
  }
}
