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

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
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
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.messages.MessageBus;
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

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import com.intellij.lexer.XmlLexer;

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

  private static VimPlugin instance;
  private VimTypedActionHandler vimHandler;
  private RegisterActions actions;
  private boolean isBlockCursor = false;
  private boolean isSmoothScrolling = false;
  private String previousKeyMap = "";

  // Make VIM plugin disabled by default to fix mess with keyboard changes
  private boolean enabled = false;
  private static Logger LOG = Logger.getInstance(VimPlugin.class.getName());

  private PropertyChangeListener myLookupPropertiesListener;

  /**
   * Creates the Vim Plugin
   */
  public VimPlugin(final MessageBus bus) {
    LOG.debug("VimPlugin ctr");
    instance = this;

    bus.connect().subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener.Adapter() {
      @Override
      public void appFrameCreated(String[] commandLineArgs, @NotNull Ref<Boolean> willOpenProject) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          public void run() {
            // Ensure that Vim keymap is installed and install if not
            VimKeyMapUtil.installKeyBoardBindings(instance);
            // Turn on proper keymap
            //VimKeyMapUtil.enableKeyBoardBindings(VimPlugin.isEnabled());
          }
        });
      }
    });
  }

  public static VimPlugin getInstance() {
    return instance;
  }

  /**
   * Supplies the name of the plugin
   *
   * @return The plugin name
   */
  @NotNull
  public String getComponentName() {
    return "VimPlugin";
  }

  public String getPreviousKeyMap() {
    return previousKeyMap;
  }

  public void setPreviousKeyMap(final String keymap) {
    previousKeyMap = keymap;
  }

  /**
   * Initialize the Vim Plugin. This plugs the vim key handler into the editor action mananger.
   */
  public void initComponent() {
    LOG.debug("initComponent");

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

  /**
   * This sets up some listeners so we can handle various events that occur
   */
  private void setupListeners() {
    DocumentManager.getInstance().addDocumentListener(new MarkGroup.MarkUpdater());
    DocumentManager.getInstance().addDocumentListener(new SearchGroup.DocumentSearchListener());
    DocumentManager.getInstance().init();

    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      public void editorCreated(EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        isBlockCursor = editor.getSettings().isBlockCursor();
        isSmoothScrolling = editor.getSettings().isAnimatedScrolling();
        EditorData.initializeEditor(editor);
        DocumentManager.getInstance().addListeners(editor.getDocument());

        if (VimPlugin.isEnabled()) {
          // Turn on insert mode if editor doesn't have any file
          if (!EditorData.isFileEditor(editor) && !CommandState.inInsertMode(editor)) {
            CommandGroups.getInstance().getChange().insertBeforeCursor(editor, new EditorDataContext(editor));
          }
          editor.getSettings().setBlockCursor(!CommandState.inInsertMode(editor));
          editor.getSettings().setAnimatedScrolling(false);
        }
      }

      public void editorReleased(EditorFactoryEvent event) {
        EditorData.uninitializeEditor(event.getEditor());
        event.getEditor().getSettings().setAnimatedScrolling(isSmoothScrolling);
        DocumentManager.getInstance().removeListeners(event.getEditor().getDocument());
      }
    });

    // Since the Vim plugin custom actions aren't available to the call to <code>initComponent()</code>
    // we need to force the generation of the key map when the first project is opened.
    ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
      public void projectOpened(Project project) {
        listeners.add(new MotionGroup.MotionEditorChange());
        listeners.add(new FileGroup.SelectionCheck());
        listeners.add(new SearchGroup.EditorSelectionCheck());

        for (FileEditorManagerListener listener : listeners) {
          FileEditorManager.getInstance(project).addFileEditorManagerListener(listener);
        }

        myLookupPropertiesListener = new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            if (LookupManager.PROP_ACTIVE_LOOKUP.equals(evt.getPropertyName())) {
              final Lookup lookup = (Lookup)evt.getNewValue();
              if (lookup != null && lookup.isFocused()) {
                final Editor editor = lookup.getEditor();
                // Do not toggle on insert mode if already in it.
                if (!CommandState.inInsertMode(editor)){
                  CommandGroups.getInstance().getChange().insertBeforeCursor(editor, new EditorDataContext(editor));
                }
              }
            }
          }
        };
        LookupManager.getInstance(project).addPropertyChangeListener(myLookupPropertiesListener);
      }

      public void projectClosed(Project project) {
        for (FileEditorManagerListener listener : listeners) {
          FileEditorManager.getInstance(project).removeFileEditorManagerListener(listener);
        }
        LookupManager.getInstance(project).removePropertyChangeListener(myLookupPropertiesListener);
        listeners.clear();
      }

      ArrayList<FileEditorManagerListener> listeners = new ArrayList<FileEditorManagerListener>();
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

  //@Override
  public void loadState(final Element element) {
    LOG.debug("Loading state");

    // Restore whether the plugin is enabled or not
    Element state = element.getChild("state");
    if (state != null) {
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

  public static void setEnabled(boolean set) {
    if (!set) {
      getInstance().turnOffPlugin();
    }

    getInstance().enabled = set;

    if (set) {
      getInstance().turnOnPlugin();
    }

    VimKeyMapUtil.enableKeyBoardBindings(set);
  }

  /**
   * Indicate to the user that an error has occurred. Just beep.
   */
  public static void indicateError() {
    if (!Options.getInstance().isSet("visualbell")) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void showMode(String msg) {
    showMessage(msg);
  }

  public static void showMessage(String msg) {
    ProjectManager pm = ProjectManager.getInstance();
    Project[] projs = pm.getOpenProjects();
    for (Project proj : projs) {
      StatusBar bar = WindowManager.getInstance().getStatusBar(proj);
      if (msg == null || msg.length() == 0) {
        bar.setInfo("");
      }
      else {
        bar.setInfo("VIM - " + msg);
      }
    }
  }

  public void turnOnPlugin() {
    KeyHandler.getInstance().fullReset(null);
    setCursors(true);
    setSmoothScrolling(false);

    CommandGroups.getInstance().getMotion().turnOn();
  }

  public void turnOffPlugin() {
    KeyHandler.getInstance().fullReset(null);
    setCursors(isBlockCursor);
    setSmoothScrolling(isSmoothScrolling);

    CommandGroups.getInstance().getMotion().turnOff();
  }

  private void setCursors(boolean isBlock) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setBlockCursor(isBlock);
    }
  }

  private void setSmoothScrolling(boolean isOn) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setAnimatedScrolling(isOn);
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
}
