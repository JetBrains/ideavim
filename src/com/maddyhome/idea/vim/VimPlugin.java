/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maddyhome.idea.vim;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.group.*;
import com.maddyhome.idea.vim.helper.DelegateCommandListener;
import com.maddyhome.idea.vim.helper.DocumentManager;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.key.RegisterActions;
import com.maddyhome.idea.vim.option.Options;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * This plugin attempts to emulate the keybinding and general functionality of Vim and gVim. See the supplied
 * documentation for a complete list of supported and unsupported Vim emulation. The code base contains some debugging
 * output that can be enabled in necessary.
 * <p/>
 * This is an application level plugin meaning that all open projects will share a common instance of the plugin.
 * Registers and marks are shared across open projects so you can copy and paste between files of different projects.
 *
 * @version 0.1
 */
@State(
    name = "VimSettings",
    storages = {
        @Storage(
            id = "main",
            file = "$APP_CONFIG$/vim_settings.xml"
        )}
)
public class VimPlugin implements ApplicationComponent, PersistentStateComponent<Element>
{

  private static final String IDEAVIM_COMPONENT_NAME = "VimPlugin";
  public static final String IDEAVIM_NOTIFICATION_ID = "ideavim";
  public static final String IDEAVIM_NOTIFICATION_TITLE = "IdeaVim";
  public static final int STATE_VERSION = 2;

  private static final boolean BLOCK_CURSOR_VIM_VALUE = true;
  private static final boolean ANIMATED_SCROLLING_VIM_VALUE = false;
  private static final boolean REFRAIN_FROM_SCROLLING_VIM_VALUE = true;

  private VimTypedActionHandler vimHandler;
  private RegisterActions actions;
  private boolean isBlockCursor = false;
  private boolean isAnimatedScrolling = false;
  private boolean isRefrainFromScrolling = false;
  private boolean error = false;

  private int previousStateVersion = 0;
  private String previousKeyMap = "";

  // It is enabled by default to avoid any special configuration after plugin installation
  private boolean enabled = true;

  private static Logger LOG = Logger.getInstance(VimPlugin.class);

  private final Application myApp;


  /**
   * Creates the Vim Plugin
   */
  public VimPlugin(final Application app) {
    myApp = app;
    LOG.debug("VimPlugin ctr");
  }

  @NotNull
  public static VimPlugin getInstance() {
    return (VimPlugin)ApplicationManager.getApplication().getComponent(IDEAVIM_COMPONENT_NAME);
  }

  /**
   * Supplies the name of the plugin
   *
   * @return The plugin name
   */
  @NotNull
  public String getComponentName() {
    return IDEAVIM_COMPONENT_NAME;
  }

  public String getPreviousKeyMap() {
    return previousKeyMap;
  }

  public void setPreviousKeyMap(final String keymap) {
    previousKeyMap = keymap;
  }

  /**
   * Initialize the Vim Plugin. This plugs the vim key handler into the editor action manager.
   */
  public void initComponent() {
    LOG.debug("initComponent");

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        updateState();
        checkAndInstallKeymap();
      }
    });

    EditorActionManager manager = EditorActionManager.getInstance();
    TypedAction action = manager.getTypedAction();

    // Replace the default key handler with the Vim key handler
    vimHandler = new VimTypedActionHandler(action.getHandler());
    action.setupHandler(vimHandler);

    // Add some listeners so we can handle special events
    setupListeners();

    getActions();

    LOG.debug("done");
  }

  private void updateState() {
    if (isEnabled()) {
      boolean requiresRestart = false;
      if (previousStateVersion < 1) {
        if (Messages.showYesNoDialog("Vim keymap generator has been updated to create keymaps more compatible " +
                                     "with base keymaps.\n\nDo you want to reconfigure your Vim keymap?\n\n" +
                                     "(You can do it later using Tools | Reconfigure Vim Keymap).",
                                     IDEAVIM_NOTIFICATION_TITLE,
                                     Messages.getQuestionIcon()) == Messages.YES) {
          KeyHandler.executeAction("VimReconfigureKeymap", SimpleDataContext.getProjectContext(null));
        }
      }
      if (previousStateVersion < 2 && SystemInfo.isMac) {
        if (Messages.showYesNoDialog("Do you want to enable repeating keys in Mac OS X on press and hold " +
                                     "(requires restart)?",
                                     IDEAVIM_NOTIFICATION_TITLE,
                                     Messages.getQuestionIcon()) == Messages.YES) {
          try {
            final String command = "defaults write -globalDomain ApplePressAndHoldEnabled 0";
            final Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            requiresRestart = true;
          }
          catch (IOException ignored) {
            Notifications.Bus.notify(new Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
                                                      "Cannot enable repeating keys in Mac OS X",
                                                      NotificationType.ERROR));
          }
          catch (InterruptedException ignored) {
          }
        }
      }
      if (requiresRestart) {
        final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        app.restart();
      }
    }
  }

  private static void checkAndInstallKeymap() {
    // Ensure that Vim keymap is installed and install if not.
    // Moreover we can use installed keymap as indicator of the first time installed plugin
    if (VimPlugin.isEnabled()) {
      boolean vimKeyMapInstalled = VimKeyMapUtil.isVimKeymapInstalled();
      // In case if keymap wasn't installed, we assume that this is the first launch after installation
      if (!vimKeyMapInstalled) {
        vimKeyMapInstalled = VimKeyMapUtil.installKeyBoardBindings();
        if (!vimKeyMapInstalled) {
          if (Messages.showYesNoDialog("It is crucial to use Vim keymap for IdeaVim plugin correct work, " +
                                       "however it was not installed correctly.\nDo you want " +
                                       ApplicationManagerEx.getApplicationEx().getName() +
                                       " to disable Vim emulation?", IDEAVIM_NOTIFICATION_TITLE, Messages.getQuestionIcon()) == Messages.YES) {
            VimPlugin.getInstance().turnOffPlugin();
            return;
          }

        }
        // Enable proper keymap bindings
        VimKeyMapUtil.switchKeymapBindings(true);
      }
      // In this case we should warn if user doesn't use vim keymap
      else {
        if (!VimKeyMapUtil.isVimKeymapUsed()) {
          Notifications.Bus.notify(new Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
                                                    "Vim keymap is not active, IdeaVim plugin may work incorrectly",
                                                    NotificationType.WARNING));
        }
      }
    }
  }

  /**
   * This sets up some listeners so we can handle various events that occur
   */
  private void setupListeners() {
    DocumentManager.getInstance().addDocumentListener(new MarkGroup.MarkUpdater());
    DocumentManager.getInstance().addDocumentListener(new SearchGroup.DocumentSearchListener());
    DocumentManager.getInstance().init();

    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        isBlockCursor = editor.getSettings().isBlockCursor();
        isAnimatedScrolling = editor.getSettings().isAnimatedScrolling();
        isRefrainFromScrolling = editor.getSettings().isRefrainFromScrolling();
        EditorData.initializeEditor(editor);
        DocumentManager.getInstance().addListeners(editor.getDocument());

        if (VimPlugin.isEnabled()) {
          // Turn on insert mode if editor doesn't have any file
          if (!EditorData.isFileEditor(editor) && !CommandState.inInsertMode(editor)) {
            KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('i'), new EditorDataContext(editor));
          }
          editor.getSettings().setBlockCursor(!CommandState.inInsertMode(editor));
          editor.getSettings().setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
          editor.getSettings().setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);
        }
      }

      public void editorReleased(@NotNull EditorFactoryEvent event) {
        EditorData.uninitializeEditor(event.getEditor());
        event.getEditor().getSettings().setAnimatedScrolling(isAnimatedScrolling);
        event.getEditor().getSettings().setRefrainFromScrolling(isRefrainFromScrolling);
        DocumentManager.getInstance().removeListeners(event.getEditor().getDocument());
      }
    }, myApp);

    // Since the Vim plugin custom actions aren't available to the call to <code>initComponent()</code>
    // we need to force the generation of the key map when the first project is opened.
    ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
      public void projectOpened(@NotNull final Project project) {
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new MotionGroup.MotionEditorChange());
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileGroup.SelectionCheck());
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new SearchGroup.EditorSelectionCheck());
      }

      public void projectClosed(final Project project) {
      }
    });

    CommandProcessor.getInstance().addCommandListener(DelegateCommandListener.getInstance());
  }

  /**
   * This shuts down the Vim plugin. All we need to do is reinstall the original key handler
   */
  public void disposeComponent() {
    LOG.debug("disposeComponent");
    turnOffPlugin();
    EditorActionManager manager = EditorActionManager.getInstance();
    TypedAction action = manager.getTypedAction();
    action.setupHandler(vimHandler.getOriginalTypedHandler());
    LOG.debug("done");
  }

  @Override
  public void loadState(@NotNull final Element element) {
    LOG.debug("Loading state");

    // Restore whether the plugin is enabled or not
    Element state = element.getChild("state");
    if (state != null) {
      try {
        previousStateVersion = Integer.valueOf(state.getAttributeValue("version"));
      }
      catch (NumberFormatException ignored) {
      }
      enabled = Boolean.valueOf(state.getAttributeValue("enabled"));
      previousKeyMap = state.getAttributeValue("keymap");
    }

    CommandGroups.getInstance().readData(element);
  }

  @Override
  public Element getState() {
    LOG.debug("Saving state");

    final Element element = new Element("ideavim");
    // Save whether the plugin is enabled or not
    final Element state = new Element("state");
    state.setAttribute("version", Integer.toString(STATE_VERSION));
    state.setAttribute("enabled", Boolean.toString(enabled));
    state.setAttribute("keymap", previousKeyMap);
    element.addContent(state);

    CommandGroups.getInstance().saveData(element);
    return element;
  }

  /**
   * Indicates whether the user has enabled or disabled the plugin
   *
   * @return true if the Vim plugin is enabled, false if not
   */
  public static boolean isEnabled() {
    return getInstance().enabled;
  }

  public static void setEnabled(final boolean enabled) {
    if (!enabled) {
      getInstance().turnOffPlugin();
    }

    getInstance().enabled = enabled;

    if (enabled) {
      getInstance().turnOnPlugin();
    }

    VimKeyMapUtil.switchKeymapBindings(enabled);
  }

  /**
   * Inidicate to the user that an error has occurred. Just beep.
   */
  public static void indicateError() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getInstance().error = true;
    }
    else if (!Options.getInstance().isSet("visualbell")) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void showMode(String msg) {
    showMessage(msg);
  }

  public static void showMessage(@Nullable String msg) {
    ProjectManager pm = ProjectManager.getInstance();
    Project[] projs = pm.getOpenProjects();
    for (Project proj : projs) {
      StatusBar bar = WindowManager.getInstance().getStatusBar(proj);
      if (bar != null) {
        if (msg == null || msg.length() == 0) {
          bar.setInfo("");
        }
        else {
          bar.setInfo("VIM - " + msg);
        }
      }
    }
  }

  public void turnOnPlugin() {
    KeyHandler.getInstance().fullReset(null);
    setCursors(BLOCK_CURSOR_VIM_VALUE);
    setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
    setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);

    CommandGroups.getInstance().getMotion().turnOn();
  }

  public void turnOffPlugin() {
    KeyHandler.getInstance().fullReset(null);
    setCursors(isBlockCursor);
    setAnimatedScrolling(isAnimatedScrolling);
    setRefrainFromScrolling(isRefrainFromScrolling);

    CommandGroups.getInstance().getMotion().turnOff();
  }

  private void setCursors(boolean isBlock) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      // Vim plugin should be turned on in insert mode
      ((EditorEx)editor).setInsertMode(true);
      editor.getSettings().setBlockCursor(isBlock);
    }
  }

  private void setAnimatedScrolling(boolean isOn) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setAnimatedScrolling(isOn);
    }
  }

  private void setRefrainFromScrolling(boolean isOn) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setRefrainFromScrolling(isOn);
    }
  }

  private RegisterActions getActions() {
    if (actions == null) {
      // Register vim actions in command mode
      actions = RegisterActions.getInstance();
      // Register ex handlers
      CommandParser.getInstance().registerHandlers();
    }

    return actions;
  }

  public boolean isError() {
    return error;
  }

  public static void clearError() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getInstance().error = false;
    }
  }
}
