/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.keymap.impl.DefaultKeymap;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.maddyhome.idea.vim.config.VimState;
import com.maddyhome.idea.vim.config.migration.ApplicationConfigurationMigrator;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser;
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar;
import com.maddyhome.idea.vim.group.*;
import com.maddyhome.idea.vim.group.copy.PutGroup;
import com.maddyhome.idea.vim.group.copy.YankGroup;
import com.maddyhome.idea.vim.group.visual.VisualMotionGroup;
import com.maddyhome.idea.vim.helper.MacKeyRepeat;
import com.maddyhome.idea.vim.listener.VimListenerManager;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.ui.StatusBarIconFactory;
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable;
import com.maddyhome.idea.vim.ui.VimRcFileState;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.util.List;

import static com.maddyhome.idea.vim.group.EditorGroup.EDITOR_STORE_ELEMENT;
import static com.maddyhome.idea.vim.group.KeyGroup.SHORTCUT_CONFLICTS_ELEMENT;

/**
 * This plugin attempts to emulate the key binding and general functionality of Vim and gVim. See the supplied
 * documentation for a complete list of supported and unsupported Vim emulation. The code base contains some debugging
 * output that can be enabled in necessary.
 * <p/>
 * This is an application level plugin meaning that all open projects will share a common instance of the plugin.
 * Registers and marks are shared across open projects so you can copy and paste between files of different projects.
 */
@State(name = "VimSettings", storages = {@Storage("$APP_CONFIG$/vim_settings.xml")})
public class VimPlugin implements PersistentStateComponent<Element>, Disposable {
  private static final String IDEAVIM_PLUGIN_ID = "IdeaVIM";
  public static final int STATE_VERSION = 7;

  private static long lastBeepTimeMillis;

  private boolean error = false;
  private String message = null;

  private int previousStateVersion = 0;
  private String previousKeyMap = "";

  // It is enabled by default to avoid any special configuration after plugin installation
  private boolean enabled = true;

  private static final Logger LOG = Logger.getInstance(VimPlugin.class);

  private final @NotNull VimState state = new VimState();

  VimPlugin() {
    ApplicationConfigurationMigrator.getInstance().migrate();
  }

  public void initialize() {
    LOG.debug("initComponent");

    if (enabled) {
      Application application = ApplicationManager.getApplication();
      if (application.isUnitTestMode()) {
        application.invokeAndWait(this::turnOnPlugin);
      }
      else {
        application.invokeLater(this::turnOnPlugin);
      }
    }

    LOG.debug("done");
  }

  @Override
  public void dispose() {
    LOG.debug("disposeComponent");
    turnOffPlugin();
    LOG.debug("done");
  }

  /**
   * @return NotificationService as applicationService if project is null and projectService otherwise
   */
  public static @NotNull NotificationService getNotifications(@Nullable Project project) {
    if (project == null) {
      return ServiceManager.getService(NotificationService.class);
    }
    else {
      return ServiceManager.getService(project, NotificationService.class);
    }
  }

  public static @NotNull VimState getVimState() {
    return getInstance().state;
  }


  public static @NotNull MotionGroup getMotion() {
    return ServiceManager.getService(MotionGroup.class);
  }

  public static @NotNull ChangeGroup getChange() {
    return ServiceManager.getService(ChangeGroup.class);
  }

  public static @NotNull CommandGroup getCommand() {
    return ServiceManager.getService(CommandGroup.class);
  }

  public static @NotNull MarkGroup getMark() {
    return ServiceManager.getService(MarkGroup.class);
  }

  public static @NotNull RegisterGroup getRegister() {
    return ServiceManager.getService(RegisterGroup.class);
  }

  public static @Nullable RegisterGroup getRegisterIfCreated() {
    return ApplicationManager.getApplication().getServiceIfCreated(RegisterGroup.class);
  }

  public static @NotNull FileGroup getFile() {
    return ServiceManager.getService(FileGroup.class);
  }

  public static @NotNull SearchGroup getSearch() {
    return ServiceManager.getService(SearchGroup.class);
  }

  public static @Nullable SearchGroup getSearchIfCreated() {
    return ApplicationManager.getApplication().getServiceIfCreated(SearchGroup.class);
  }

  public static @NotNull ProcessGroup getProcess() {
    return ServiceManager.getService(ProcessGroup.class);
  }

  public static @NotNull MacroGroup getMacro() {
    return ServiceManager.getService(MacroGroup.class);
  }

  public static @NotNull DigraphGroup getDigraph() {
    return ServiceManager.getService(DigraphGroup.class);
  }

  public static @NotNull HistoryGroup getHistory() {
    return ServiceManager.getService(HistoryGroup.class);
  }

  public static @NotNull KeyGroup getKey() {
    return ServiceManager.getService(KeyGroup.class);
  }

  public static @Nullable KeyGroup getKeyIfCreated() {
    return ApplicationManager.getApplication().getServiceIfCreated(KeyGroup.class);
  }

  public static @NotNull WindowGroup getWindow() {
    return ServiceManager.getService(WindowGroup.class);
  }

  public static @NotNull EditorGroup getEditor() {
    return ServiceManager.getService(EditorGroup.class);
  }

  public static @Nullable EditorGroup getEditorIfCreated() {
    return ApplicationManager.getApplication().getServiceIfCreated(EditorGroup.class);
  }

  public static @NotNull VisualMotionGroup getVisualMotion() {
    return ServiceManager.getService(VisualMotionGroup.class);
  }

  public static @NotNull YankGroup getYank() {
    return ServiceManager.getService(YankGroup.class);
  }

  public static @NotNull PutGroup getPut() {
    return ServiceManager.getService(PutGroup.class);
  }

  private static @NotNull NotificationService getNotifications() {
    return getNotifications(null);
  }

  private boolean ideavimrcRegistered = false;

  private void registerIdeavimrc() {
    if (ideavimrcRegistered) return;
    ideavimrcRegistered = true;

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      try {
        VimScriptParser.INSTANCE.setExecutingVimScript(true);
        executeIdeaVimRc();
      }
      finally {
        VimScriptParser.INSTANCE.setExecutingVimScript(false);
      }
    }
  }

  public void executeIdeaVimRc() {
    final File ideaVimRc = VimScriptParser.findIdeaVimRc();
    if (ideaVimRc != null) {
      LOG.info("Execute ideavimrc file: " + ideaVimRc.getAbsolutePath());
      List<String> parsedLines = VimScriptParser.executeFile(ideaVimRc);
      VimRcFileState.INSTANCE.saveFileState(ideaVimRc.getAbsolutePath(), parsedLines);
    }
    else {
      LOG.info("ideavimrc file isn't found");
    }
  }

  public static @NotNull PluginId getPluginId() {
    return PluginId.getId(IDEAVIM_PLUGIN_ID);
  }

  public static @NotNull String getVersion() {
    final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(getPluginId());
    if (!ApplicationManager.getApplication().isInternal()) {
      return plugin != null ? plugin.getVersion() : "SNAPSHOT";
    }
    else {
      return "INTERNAL" + (plugin != null ? " - " + plugin.getVersion() : "");
    }
  }

  public static boolean isEnabled() {
    return getInstance().enabled;
  }

  public static void setEnabled(final boolean enabled) {
    if (isEnabled() == enabled) return;

    if (!enabled) {
      getInstance().turnOffPlugin();
    }

    getInstance().enabled = enabled;

    if (enabled) {
      getInstance().turnOnPlugin();
    }

    StatusBarIconFactory.Companion.updateIcon();
  }

  public static boolean isError() {
    return getInstance().error;
  }

  public static String getMessage() {
    return getInstance().message;
  }

  /**
   * Indicate to the user that an error has occurred. Just beep.
   */
  public static void indicateError() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getInstance().error = true;
    }
    else if (!OptionsManager.INSTANCE.getVisualbell().isSet()) {
      // Vim only allows a beep once every half second - :help 'visualbell'
      final long currentTimeMillis = System.currentTimeMillis();
      if (currentTimeMillis - lastBeepTimeMillis > 500) {
        Toolkit.getDefaultToolkit().beep();
        lastBeepTimeMillis = currentTimeMillis;
      }
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

  public static void showMessage(@Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String msg) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getInstance().message = msg;
    }
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

  public static @NotNull VimPlugin getInstance() {
    return ServiceManager.getService(VimPlugin.class);
  }

  private void turnOnPlugin() {
    ApplicationManager.getApplication().invokeLater(this::updateState);

    // Register vim actions in command mode
    RegisterActions.registerActions();

    // Register ex handlers
    CommandParser.INSTANCE.registerHandlers();

    // Register extensions
    VimExtensionRegistrar.registerExtensions();

    // Execute ~/.ideavimrc
    registerIdeavimrc();

    // Initialize extensions
    VimExtensionRegistrar.enableDelayedExtensions();

    // Turing on should be performed after all commands registration
    getSearch().turnOn();
    VimListenerManager.INSTANCE.turnOn();
  }

  private void turnOffPlugin() {
    KeyHandler.getInstance().fullReset(null);

    SearchGroup searchGroup = getSearchIfCreated();
    if (searchGroup != null) {
      searchGroup.turnOff();
    }
    VimListenerManager.INSTANCE.turnOff();
    ExEntryPanel.fullReset();

    // Unregister vim actions in command mode
    RegisterActions.unregisterActions();

    // Unregister ex handlers
    CommandParser.INSTANCE.unregisterHandlers();
  }

  private boolean stateUpdated = false;

  private void updateState() {
    if (stateUpdated) return;
    if (isEnabled() && !ApplicationManager.getApplication().isUnitTestMode()) {
      stateUpdated = true;
      if (SystemInfo.isMac) {
        final MacKeyRepeat keyRepeat = MacKeyRepeat.getInstance();
        final Boolean enabled = keyRepeat.isEnabled();
        final Boolean isKeyRepeat = getEditor().isKeyRepeat();
        if ((enabled == null || !enabled) && (isKeyRepeat == null || isKeyRepeat)) {
          if (VimPlugin.getNotifications().enableRepeatingMode() == Messages.YES) {
            getEditor().setKeyRepeat(true);
            keyRepeat.setEnabled(true);
          }
          else {
            getEditor().setKeyRepeat(false);
          }
        }
      }
      if (previousStateVersion > 0 && previousStateVersion < 3) {
        final KeymapManagerEx manager = KeymapManagerEx.getInstanceEx();
        Keymap keymap = null;
        if (previousKeyMap != null) {
          keymap = manager.getKeymap(previousKeyMap);
        }
        if (keymap == null) {
          keymap = manager.getKeymap(DefaultKeymap.getInstance().getDefaultKeymapName());
        }
        assert keymap != null : "Default keymap not found";
        VimPlugin.getNotifications().specialKeymap(keymap, new NotificationListener.Adapter() {
          @Override
          protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
            ShowSettingsUtil.getInstance().editConfigurable((Project)null, new VimEmulationConfigurable());
          }
        });
        manager.setActiveKeymap(keymap);
      }
      if (previousStateVersion > 0 && previousStateVersion < 4) {
        VimPlugin.getNotifications().noVimrcAsDefault();
      }
    }
  }

  @Override
  public void loadState(final @NotNull Element element) {
    LOG.debug("Loading state");

    // Restore whether the plugin is enabled or not
    Element state = element.getChild("state");
    if (state != null) {
      try {
        previousStateVersion = Integer.parseInt(state.getAttributeValue("version"));
      }
      catch (NumberFormatException ignored) {
      }
      enabled = Boolean.parseBoolean(state.getAttributeValue("enabled"));
      previousKeyMap = state.getAttributeValue("keymap");
    }

    legacyStateLoading(element);
    this.state.readData(element);
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

    this.state.saveData(element);

    return element;
  }

  private void legacyStateLoading(@NotNull Element element) {
    if (previousStateVersion > 0 && previousStateVersion < 5) {
      // Migrate settings from 4 to 5 version
      getMark().readData(element);
      getRegister().readData(element);
      getSearch().readData(element);
      getHistory().readData(element);
    }
    if (element.getChild(SHORTCUT_CONFLICTS_ELEMENT) != null) {
      getKey().readData(element);
    }
    if (element.getChild(EDITOR_STORE_ELEMENT) != null) {
      getEditor().readData(element);
    }
  }
}
