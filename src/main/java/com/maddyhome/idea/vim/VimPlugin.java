/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.keymap.impl.DefaultKeymap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.api.VimKeyGroup;
import com.maddyhome.idea.vim.config.VimState;
import com.maddyhome.idea.vim.config.migration.ApplicationConfigurationMigrator;
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar;
import com.maddyhome.idea.vim.group.*;
import com.maddyhome.idea.vim.group.copy.PutGroup;
import com.maddyhome.idea.vim.group.copy.YankGroup;
import com.maddyhome.idea.vim.group.visual.VisualMotionGroup;
import com.maddyhome.idea.vim.helper.MacKeyRepeat;
import com.maddyhome.idea.vim.listener.VimListenerManager;
import com.maddyhome.idea.vim.newapi.IjVimInjector;
import com.maddyhome.idea.vim.ui.StatusBarIconFactory;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage;
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService;
import com.maddyhome.idea.vim.vimscript.services.OptionService;
import com.maddyhome.idea.vim.vimscript.services.VariableService;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.maddyhome.idea.vim.group.EditorGroup.EDITOR_STORE_ELEMENT;
import static com.maddyhome.idea.vim.group.KeyGroup.SHORTCUT_CONFLICTS_ELEMENT;
import static com.maddyhome.idea.vim.vimscript.services.VimRcService.executeIdeaVimRc;

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

  static {
    VimInjectorKt.setInjector(new IjVimInjector());
  }

  private static final String IDEAVIM_PLUGIN_ID = "IdeaVIM";
  public static final int STATE_VERSION = 7;

  private int previousStateVersion = 0;
  private String previousKeyMap = "";

  // It is enabled by default to avoid any special configuration after plugin installation
  private boolean enabled = true;

  private static final Logger LOG = Logger.getInstance(VimPlugin.class);

  private final @NotNull VimState state = new VimState();

  public Disposable onOffDisposable;

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
      return ApplicationManager.getApplication().getService(NotificationService.class);
    }
    else {
      return project.getService(NotificationService.class);
    }
  }

  public static @NotNull VimState getVimState() {
    return getInstance().state;
  }


  public static @NotNull MotionGroup getMotion() {
    return ApplicationManager.getApplication().getService(MotionGroup.class);
  }

  public static @NotNull XMLGroup getXML() {
    return ApplicationManager.getApplication().getService(XMLGroup.class);
  }

  public static @NotNull ChangeGroup getChange() {
    return ((ChangeGroup)VimInjectorKt.getInjector().getChangeGroup());
  }

  public static @NotNull CommandGroup getCommand() {
    return ApplicationManager.getApplication().getService(CommandGroup.class);
  }

  public static @NotNull MarkGroup getMark() {
    return ((MarkGroup)VimInjectorKt.getInjector().getMarkGroup());
  }

  public static @NotNull RegisterGroup getRegister() {
    return ((RegisterGroup)VimInjectorKt.getInjector().getRegisterGroup());
  }

  public static @NotNull FileGroup getFile() {
    return (FileGroup)VimInjectorKt.getInjector().getFile();
  }

  public static @NotNull SearchGroup getSearch() {
    return ApplicationManager.getApplication().getService(SearchGroup.class);
  }

  public static @Nullable SearchGroup getSearchIfCreated() {
    return ApplicationManager.getApplication().getServiceIfCreated(SearchGroup.class);
  }

  public static @NotNull ProcessGroup getProcess() {
    return ((ProcessGroup)VimInjectorKt.getInjector().getProcessGroup());
  }

  public static @NotNull MacroGroup getMacro() {
    return (MacroGroup)VimInjectorKt.getInjector().getMacro();
  }

  public static @NotNull DigraphGroup getDigraph() {
    return (DigraphGroup)VimInjectorKt.getInjector().getDigraphGroup();
  }

  public static @NotNull HistoryGroup getHistory() {
    return ApplicationManager.getApplication().getService(HistoryGroup.class);
  }

  public static @NotNull KeyGroup getKey() {
    return ((KeyGroup)VimInjectorKt.getInjector().getKeyGroup());
  }

  public static @Nullable KeyGroup getKeyIfCreated() {
    return ((KeyGroup)ApplicationManager.getApplication().getServiceIfCreated(VimKeyGroup.class));
  }

  public static @NotNull WindowGroup getWindow() {
    return ((WindowGroup)VimInjectorKt.getInjector().getWindow());
  }

  public static @NotNull TabService getTabService() {
    return ApplicationManager.getApplication().getService(TabService.class);
  }

  public static @NotNull EditorGroup getEditor() {
    return ApplicationManager.getApplication().getService(EditorGroup.class);
  }

  public static @Nullable EditorGroup getEditorIfCreated() {
    return ApplicationManager.getApplication().getServiceIfCreated(EditorGroup.class);
  }

  public static @NotNull VisualMotionGroup getVisualMotion() {
    return (VisualMotionGroup)VimInjectorKt.getInjector().getVisualMotionGroup();
  }

  public static @NotNull YankGroup getYank() {
    return (YankGroup)VimInjectorKt.getInjector().getYank();
  }

  public static @NotNull PutGroup getPut() {
    return (PutGroup)VimInjectorKt.getInjector().getPut();
  }

  public static @NotNull VariableService getVariableService() {
    return ApplicationManager.getApplication().getService(VariableService.class);
  }

  public static @NotNull OptionService getOptionService() {
    return VimInjectorKt.getInjector().getOptionService();
  }

  public static @NotNull IjVimOptionService getOptionServiceImpl() {
    return (IjVimOptionService)VimInjectorKt.getInjector().getOptionService();
  }

  private static @NotNull NotificationService getNotifications() {
    return getNotifications(null);
  }

  private boolean ideavimrcRegistered = false;

  private void registerIdeavimrc() {
    if (ideavimrcRegistered) return;
    ideavimrcRegistered = true;

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      executeIdeaVimRc();
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
    return VimInjectorKt.getInjector().getMessages().isError();
  }

  public static String getMessage() {
    return VimInjectorKt.getInjector().getMessages().getStatusBarMessage();
  }

  /**
   * Indicate to the user that an error has occurred. Just beep.
   */
  public static void indicateError() {
    VimInjectorKt.getInjector().getMessages().indicateError();
  }

  public static void clearError() {
    VimInjectorKt.getInjector().getMessages().clearError();
  }

  public static void showMode(String msg) {
    VimInjectorKt.getInjector().getMessages().showMode(msg);
  }

  public static void showMessage(@Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String msg) {
    VimInjectorKt.getInjector().getMessages().showStatusBarMessage(msg);
  }

  public static @NotNull VimPlugin getInstance() {
    return ApplicationManager.getApplication().getService(VimPlugin.class);
  }

  /**
   * IdeaVim plugin initialization.
   * This is an important operation and some commands ordering should be preserved.
   * Please make sure that the documentation of this function is in sync with the code
   *
   * 1) Update state
   *    This schedules a state update. In most cases it just shows some dialogs to the user. As I know, there are
   *      no special reasons to keep this command as a first line, so it seems safe to move it.
   * 2) Command registration
   *    This block should be located BEFORE ~/.ideavimrc execution. Without it the commands won't be registered
   *      and initialized, but ~/.ideavimrc file may refer or execute some commands or functions.
   *    This block DOES NOT initialize extensions, but only registers the available ones.
   * 3) ~/.ideavimrc execution
   *    3.1 executes commands from the .ideavimrc file and 3.2 initializes extensions.
   *    3.1 MUST BE BEFORE 3.2. This is a flow of vim/IdeaVim initialization, firstly .ideavimrc is executed and then
   *      the extensions are initialized.
   * 4) Components initialization
   *    This should happen after ideavimrc execution because VimListenerManager accesses `number` option
   *      to init line numbers and guicaret to initialize carets.
   *    However, there is a question about listeners attaching. Listeners registration happens after the .ideavimrc
   *      execution, what theoretically may cause bugs (e.g. VIM-2540)
   */
  private void turnOnPlugin() {
    onOffDisposable = Disposer.newDisposable(this, "IdeaVimOnOffDisposer");

    // 1) Update state
    ApplicationManager.getApplication().invokeLater(this::updateState);

    // 2) Command registration
    // 2.1) Register vim actions in command mode
    RegisterActions.registerActions();

    // 2.2) Register extensions
    VimExtensionRegistrar.registerExtensions();

    // 2.3) Register functions
    FunctionStorage.INSTANCE.registerHandlers();

    // 3) ~/.ideavimrc execution
    // 3.1) Execute ~/.ideavimrc
    registerIdeavimrc();

    // 3.2) Initialize extensions. Always after 3.1
    VimExtensionRegistrar.enableDelayedExtensions();

    // Turing on should be performed after all commands registration
    getSearch().turnOn();
    VimListenerManager.INSTANCE.turnOn();
  }

  private void turnOffPlugin() {
    SearchGroup searchGroup = getSearchIfCreated();
    if (searchGroup != null) {
      searchGroup.turnOff();
    }
    VimListenerManager.INSTANCE.turnOff();
    ExEntryPanel.fullReset();

    // Unregister vim actions in command mode
    RegisterActions.unregisterActions();

    Disposer.dispose(onOffDisposable);
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
          // This system property is used in IJ ui robot to hide the startup tips
          boolean showNotification = Boolean.getBoolean("ide.show.tips.on.startup.default.value");
          if (showNotification && VimPlugin.getNotifications().enableRepeatingMode() == Messages.YES) {
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
