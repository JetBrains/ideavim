package com.maddyhome.idea.vim;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.event.DocumentListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vlan
 */
public class EventFacade {
  @NotNull private static final EventFacade ourInstance = new EventFacade();

  @Nullable private TypedActionHandler myOriginalTypedActionHandler;

  private EventFacade() {
  }

  @NotNull
  public static EventFacade getInstance() {
    return ourInstance;
  }

  public void setupTypedActionHandler(@NotNull TypedActionHandler handler) {
    final TypedAction typedAction = getTypedAction();
    myOriginalTypedActionHandler = typedAction.getHandler();
    typedAction.setupHandler(handler);
  }

  public void restoreTypedActionHandler() {
    if (myOriginalTypedActionHandler != null) {
      getTypedAction().setupHandler(myOriginalTypedActionHandler);
    }
  }

  public void addDocumentListener(@NotNull Document document, @NotNull DocumentListener listener) {
    document.addDocumentListener(listener);
  }

  public void removeDocumentListener(@NotNull Document document, @NotNull DocumentListener listener) {
    document.removeDocumentListener(listener);
  }

  @NotNull
  private TypedAction getTypedAction() {
    return EditorActionManager.getInstance().getTypedAction();
  }
}
