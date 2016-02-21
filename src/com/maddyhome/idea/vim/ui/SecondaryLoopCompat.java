package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.util.Ref;

import java.awt.*;
import java.lang.reflect.*;

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
   *  {@link #exit()} is called.
   */
  public abstract void enter();

  /**
   * Exit the secondary event loop, letting
   *  the thread that called {@link #enter()}
   *  continue execution. This instance will no longer
   *  be usable once you exit, so you should acquire
   *  a new instance each time you need this functionality.
   */
  public abstract void exit();

  static SecondaryLoopCompat newInstance() {
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

    // The EventDispatchThread class and the Conditional class,
    //  both of which are at the core of the real SecondaryLoop
    //  implementation, are package-private in java.awt, which is
    //  a protected package---we can't just put our own classes
    //  in there to get access. So, we have to use a Proxy and
    //  a bunch of reflection.
    static Field dispatchThreadField;
    static Class<?> conditionalClass;
    static Method pumpEventsMethod;
    static {
      try {
        dispatchThreadField = EventQueue.class.getDeclaredField("dispatchThread");
        dispatchThreadField.setAccessible(true);

        conditionalClass = Class.forName("java.awt.Conditional");

        Class<?> eventDispatchThreadClass = Class.forName("java.awt.EventDispatchThread");
        pumpEventsMethod = eventDispatchThreadClass.getDeclaredMethod(
          "pumpEvents", conditionalClass);
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
        public Object invoke(Object o,
                             Method method,
                             Object[] args) throws Throwable {
          // NB: Conditional has just a single method,
          //  evaluate(), which should return `true`
          //  for as long as we wish to continue
          //  pumping events
          return active.get();
        }
      });

    @Override
    public void enter() {
      // The real SecondaryLoop on JDK7 does some other
      //  fancy stuff, but we will always be called
      //  from a valid dispatch thread, so this should
      //  be sufficient: basically, just manually pump
      //  events from the system EventQueue until exit()
      //  is called.
      try {
        Object dispatchThread = dispatchThreadField.get(systemQueue);
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

  /**
   * On JDK7 we can use the real SecondaryLoop using reflection
   *  to ensure that there aren't any edge cases we've missed
   *  with our hacky Proxy stuff above.
   */
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

    public Jdk7SecondaryLoopCompat(Object secondaryLoop) {
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
    }
  }
}
