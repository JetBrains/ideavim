/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
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
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.VimrcParser;
import com.maddyhome.idea.vim.group.*;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This plugin attempts to emulate the key binding and general functionality of Vim and gVim. See the supplied
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
  storages = {@Storage(
    id = "main",
    file = "$APP_CONFIG$/vim_settings.xml")})
public class VimPlugin implements ApplicationComponent, PersistentStateComponent<Element> {
  private static final String IDEAVIM_COMPONENT_NAME = "VimPlugin";
  private static final String IDEAVIM_PLUGIN_ID = "IdeaVIM";
  public static final String IDEAVIM_NOTIFICATION_ID = "ideavim";
  public static final String IDEAVIM_STICKY_NOTIFICATION_ID = "ideavim-sticky";
  public static final String IDEAVIM_NOTIFICATION_TITLE = "IdeaVim";
  public static final int STATE_VERSION = 3;

  private static final boolean BLOCK_CURSOR_VIM_VALUE = true;
  private static final boolean ANIMATED_SCROLLING_VIM_VALUE = false;
  private static final boolean REFRAIN_FROM_SCROLLING_VIM_VALUE = true;

  private VimTypedActionHandler vimHandler;
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

  private MotionGroup motion;
  private ChangeGroup change;
  private CopyGroup copy;
  private MarkGroup mark;
  private RegisterGroup register;
  private FileGroup file;
  private SearchGroup search;
  private ProcessGroup process;
  private MacroGroup macro;
  private DigraphGroup digraph;
  private HistoryGroup history;
  private KeyGroup key;

  public VimPlugin(final Application app) {
    myApp = app;

    motion = new MotionGroup();
    change = new ChangeGroup();
    copy = new CopyGroup();
    mark = new MarkGroup();
    register = new RegisterGroup();
    file = new FileGroup();
    search = new SearchGroup();
    process = new ProcessGroup();
    macro = new MacroGroup();
    digraph = new DigraphGroup();
    history = new HistoryGroup();
    key = new KeyGroup();

    LOG.debug("VimPlugin ctr");
  }

  @NotNull
  @Override
  public String getComponentName() {
    return IDEAVIM_COMPONENT_NAME;
  }

  @Override
  public void initComponent() {
    LOG.debug("initComponent");

    Notifications.Bus.register(IDEAVIM_STICKY_NOTIFICATION_ID, NotificationDisplayType.STICKY_BALLOON);

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        updateState();
      }
    });

    EditorActionManager manager = EditorActionManager.getInstance();
    TypedAction action = manager.getTypedAction();

    // Replace the default key handler with the Vim key handler
    vimHandler = new VimTypedActionHandler(action.getHandler());
    action.setupHandler(vimHandler);

    // Register vim actions in command mode
    RegisterActions.registerActions();

    // Add some listeners so we can handle special events
    setupListeners();

    // Register ex handlers
    CommandParser.getInstance().registerHandlers();

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      final File vimrc = VimrcParser.findVimrc();
      if (vimrc != null) {
        VimrcParser.executeFile(vimrc);
      }
    }

    LOG.debug("done");
  }

  @Override
  public void disposeComponent() {
    LOG.debug("disposeComponent");
    turnOffPlugin();
    EditorActionManager manager = EditorActionManager.getInstance();
    TypedAction action = manager.getTypedAction();
    action.setupHandler(vimHandler.getOriginalTypedHandler());
    LOG.debug("done");
  }

  @Override
  public Element getState() {
    LOG.debug("Saving state");

    final Element element = new Element("ideavim");
    // Save whether the plugin is enabled or not
    final Element state = new Element("state");
    state.setAttribute("version", Integer.toString(STATE_VERSION));
    state.setAttribute("enabled", Boolean.toString(enabled));
    element.addContent(state);

    mark.saveData(element);
    register.saveData(element);
    search.saveData(element);
    history.saveData(element);
    key.saveData(element);

    return element;
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

    mark.readData(element);
    register.readData(element);
    search.readData(element);
    history.readData(element);
    key.readData(element);
  }

  public static MotionGroup getMotion() {
    return getInstance().motion;
  }

  public static ChangeGroup getChange() {
    return getInstance().change;
  }

  public static CopyGroup getCopy() {
    return getInstance().copy;
  }

  public static MarkGroup getMark() {
    return getInstance().mark;
  }

  public static RegisterGroup getRegister() {
    return getInstance().register;
  }

  public static FileGroup getFile() {
    return getInstance().file;
  }

  public static SearchGroup getSearch() {
    return getInstance().search;
  }

  public static ProcessGroup getProcess() {
    return getInstance().process;
  }

  public static MacroGroup getMacro() {
    return getInstance().macro;
  }

  public static DigraphGroup getDigraph() {
    return getInstance().digraph;
  }

  public static HistoryGroup getHistory() {
    return getInstance().history;
  }

  public static KeyGroup getKey() {
    return getInstance().key;
  }

  public static PluginId getPluginId() {
    return PluginId.getId(IDEAVIM_PLUGIN_ID);
  }

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
  }

  public static boolean isError() {
    return getInstance().error;
  }

  /**
   * Indicate to the user that an error has occurred. Just beep.
   */
  public static void indicateError() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getInstance().error = true;
    }
    else if (!Options.getInstance().isSet("visualbell")) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void clearError() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getInstance().error = false;
    }
  }

  public static void showMode(String msg) {
    showMessage(msg);
  }

  public static void showMessage(@Nullable String msg) {
    ProjectManager pm = ProjectManager.getInstance();
    Project[] projects = pm.getOpenProjects();
    for (Project project : projects) {
      StatusBar bar = WindowManager.getInstance().getStatusBar(project);
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

  @NotNull
  private static VimPlugin getInstance() {
    return (VimPlugin)ApplicationManager.getApplication().getComponent(IDEAVIM_COMPONENT_NAME);
  }

  private void turnOnPlugin() {
    KeyHandler.getInstance().fullReset(null);
    setCursors(BLOCK_CURSOR_VIM_VALUE);
    setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
    setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);

    getMotion().turnOn();
  }

  private void turnOffPlugin() {
    KeyHandler.getInstance().fullReset(null);
    setCursors(isBlockCursor);
    setAnimatedScrolling(isAnimatedScrolling);
    setRefrainFromScrolling(isRefrainFromScrolling);

    getMotion().turnOff();
  }

  private void updateState() {
    if (isEnabled() && !ApplicationManager.getApplication().isUnitTestMode()) {
      boolean requiresRestart = false;
      if (previousStateVersion < 2 && SystemInfo.isMac) {
        final MacKeyRepeat keyRepeat = MacKeyRepeat.getInstance();
        final Boolean enabled = keyRepeat.isEnabled();
        if (enabled == null || !enabled) {
          if (Messages.showYesNoDialog("Do you want to enable repeating keys in Mac OS X on press and hold " +
                                       "(requires restart)?\n\n" +
                                       "(You can do it manually by running 'defaults write -g " +
                                       "ApplePressAndHoldEnabled 0' in the console).", IDEAVIM_NOTIFICATION_TITLE,
                                       Messages.getQuestionIcon()
          ) == Messages.YES) {
            keyRepeat.setEnabled(true);
            requiresRestart = true;
          }
        }
      }
      if (previousStateVersion < 3) {
        final KeymapManagerEx manager = KeymapManagerEx.getInstanceEx();
        if (previousKeyMap != null && !"".equals(previousKeyMap)) {
          new Notification(
            VimPlugin.IDEAVIM_STICKY_NOTIFICATION_ID,
            VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
            String.format("IdeaVim plugin doesn't use the special \"Vim\" keymap any longer. " +
                          "Switching back to \"%s\" keymap.<br/><br/>" +
                          "You can set up Vim or IDE handlers for conflicting shortcuts in " +
                          "<a href='#settings'>Vim Emulation</a> settings.", previousKeyMap),
            NotificationType.INFORMATION,
            new NotificationListener.Adapter() {
              @Override
              protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                ShowSettingsUtil.getInstance().editConfigurable((Project)null, new VimEmulationConfigurable());
              }
            }).notify(null);
          final Keymap keymap = manager.getKeymap(previousKeyMap);
          if (keymap != null) {
            manager.setActiveKeymap(keymap);
          }
          else {
            notify(String.format("Cannot find \"%s\" keymap, please set up a keymap manually.", previousKeyMap),
                   NotificationType.ERROR);
            return;
          }
        }
        final Keymap activeKeymap = manager.getActiveKeymap();
        if (activeKeymap != null && "Vim".equals(activeKeymap.getName())) {
          notify("Cannot disable the obsolete \"Vim\" keymap, please set up a new keymap manually.",
                 NotificationType.ERROR);
          return;
        }
      }
      if (requiresRestart) {
        final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        app.restart();
      }
    }
  }

  private void notify(@NotNull String content, @NotNull NotificationType type) {
    final Notification notification = new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID,
                                                       VimPlugin.IDEAVIM_NOTIFICATION_TITLE, content, type);
    notification.notify(null);
  }

  /**
   * This sets up some listeners so we can handle various events that occur
   */
  private void setupListeners() {
    DocumentManager.getInstance().addDocumentListener(new MarkGroup.MarkUpdater());
    DocumentManager.getInstance().addDocumentListener(new SearchGroup.DocumentSearchListener());
    DocumentManager.getInstance().init();

    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      @Override
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        isBlockCursor = editor.getSettings().isBlockCursor();
        isAnimatedScrolling = editor.getSettings().isAnimatedScrolling();
        isRefrainFromScrolling = editor.getSettings().isRefrainFromScrolling();
        EditorData.initializeEditor(editor);
        DocumentManager.getInstance().addListeners(editor.getDocument());
        final Set<KeyStroke> requiredKeys = VimPlugin.getKey().getRequiredShortcutKeys();
        getShortcutKeyAction().registerCustomShortcutSet(toShortcutSet(requiredKeys), editor.getComponent());

        if (VimPlugin.isEnabled()) {
          // Turn on insert mode if editor doesn't have any file
          if (!EditorData.isFileEditor(editor) && editor.getDocument().isWritable() &&
              !CommandState.inInsertMode(editor)) {
            KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('i'), new EditorDataContext(editor));
          }
          editor.getSettings().setBlockCursor(!CommandState.inInsertMode(editor));
          editor.getSettings().setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
          editor.getSettings().setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);
        }
      }

      @Override
      public void editorReleased(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        EditorData.uninitializeEditor(editor);
        getShortcutKeyAction().unregisterCustomShortcutSet(editor.getComponent());
        editor.getSettings().setAnimatedScrolling(isAnimatedScrolling);
        editor.getSettings().setRefrainFromScrolling(isRefrainFromScrolling);
        DocumentManager.getInstance().removeListeners(editor.getDocument());
      }
    }, myApp);

    // Since the Vim plugin custom actions aren't available to the call to <code>initComponent()</code>
    // we need to force the generation of the key map when the first project is opened.
    ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
      @Override
      public void projectOpened(@NotNull final Project project) {
        project.getMessageBus().connect()
          .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new MotionGroup.MotionEditorChange());
        project.getMessageBus().connect()
          .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileGroup.SelectionCheck());
        project.getMessageBus().connect()
          .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new SearchGroup.EditorSelectionCheck());
      }

      @Override
      public void projectClosed(final Project project) {
      }
    });

    CommandProcessor.getInstance().addCommandListener(DelegateCommandListener.getInstance());
  }

  @NotNull
  private AnAction getShortcutKeyAction() {
    return ActionManagerEx.getInstanceEx().getAction("VimShortcutKeyAction");
  }

  @NotNull
  private ShortcutSet toShortcutSet(@NotNull Collection<KeyStroke> keyStrokes) {
    final List<Shortcut> shortcuts = new ArrayList<Shortcut>();
    for (KeyStroke key : keyStrokes) {
      shortcuts.add(new KeyboardShortcut(key, null));
    }
    return new CustomShortcutSet(shortcuts.toArray(new Shortcut[shortcuts.size()]));
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
}
