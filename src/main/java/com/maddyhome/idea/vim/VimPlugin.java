/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.config.VimState;
import com.maddyhome.idea.vim.config.migration.ApplicationConfigurationMigrator;
import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.group.KeyGroup;
import com.maddyhome.idea.vim.group.VimNotifications;
import com.maddyhome.idea.vim.group.VimWindowGroup;
import com.maddyhome.idea.vim.history.VimHistory;
import com.maddyhome.idea.vim.macro.VimMacro;
import com.maddyhome.idea.vim.newapi.IjVimInjectorKt;
import com.maddyhome.idea.vim.newapi.VimLegacyStateLoader;
import com.maddyhome.idea.vim.newapi.VimSearchGroupLegacyLoader;
import com.maddyhome.idea.vim.put.VimPut;
import com.maddyhome.idea.vim.register.VimRegisterGroup;
import com.maddyhome.idea.vim.vimscript.services.VariableService;
import com.maddyhome.idea.vim.yank.YankGroupBase;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  public static final int STATE_VERSION = 7;
  private static final String IDEAVIM_PLUGIN_ID = "IdeaVIM";
  private static final Logger LOG = Logger.getInstance(VimPlugin.class);

  static {
    IjVimInjectorKt.initInjector();
  }

  private final @NotNull VimState state = new VimState();
  public Disposable onOffDisposable;
  int previousStateVersion = 0;
  String previousKeyMap = "";
  // It is enabled by default to avoid any special configuration after plugin installation
  private boolean enabled = true;

  VimPlugin() {
    ApplicationConfigurationMigrator.getInstance().migrate();
  }

  /**
   * @return VimNotifications as applicationService if project is null and projectService otherwise
   */
  public static @NotNull VimNotifications getNotifications(@Nullable Project project) {
    if (project == null) {
      return ApplicationManager.getApplication().getService(VimNotifications.class);
    }
    else {
      return project.getService(VimNotifications.class);
    }
  }

  public static @NotNull VimState getVimState() {
    return getInstance().state;
  }


  public static @NotNull VimMotionGroup getMotion() {
    return VimInjectorKt.getInjector().getMotion();
  }

  public static @NotNull ChangeGroup getChange() {
    return ((ChangeGroup)VimInjectorKt.getInjector().getChangeGroup());
  }

  public static @NotNull VimCommandGroup getCommand() {
    return VimInjectorKt.getInjector().getCommandGroup();
  }

  public static @NotNull VimRegisterGroup getRegister() {
    return VimInjectorKt.getInjector().getRegisterGroup();
  }

  public static @NotNull VimFile getFile() {
    return VimInjectorKt.getInjector().getFile();
  }

  public static @NotNull VimSearchGroup getSearch() {
    return VimInjectorKt.getInjector().getSearchGroup();
  }

  public static @Nullable VimSearchGroup getSearchIfCreated() {
    VimSearchGroup searchGroup = ApplicationManager.getApplication().getServiceIfCreated(VimSearchGroup.class);
    return searchGroup;
  }

  public static @NotNull VimProcessGroup getProcess() {
    return VimInjectorKt.getInjector().getProcessGroup();
  }

  public static @NotNull VimMacro getMacro() {
    return VimInjectorKt.getInjector().getMacro();
  }

  public static @NotNull VimDigraphGroup getDigraph() {
    return VimInjectorKt.getInjector().getDigraphGroup();
  }

  public static @NotNull VimHistory getHistory() {
    return VimInjectorKt.getInjector().getHistoryGroup();
  }

  public static @NotNull KeyGroup getKey() {
    return ((KeyGroup)VimInjectorKt.getInjector().getKeyGroup());
  }

  public static @Nullable KeyGroup getKeyIfCreated() {
    return ApplicationManager.getApplication().getServiceIfCreated(KeyGroup.class);
  }

  public static @NotNull VimWindowGroup getWindow() {
    return VimInjectorKt.getInjector().getWindow();
  }

  public static @NotNull VimEditorGroup getEditor() {
    return VimInjectorKt.getInjector().getEditorGroup();
  }

  public static @Nullable VimEditorGroup getEditorIfCreated() {
    return ApplicationManager.getApplication().getServiceIfCreated(VimEditorGroup.class);
  }

  public static @NotNull VimVisualMotionGroup getVisualMotion() {
    return VimInjectorKt.getInjector().getVisualMotionGroup();
  }

  public static @NotNull YankGroupBase getYank() {
    return (YankGroupBase)VimInjectorKt.getInjector().getYank();
  }

  public static @NotNull VimPut getPut() {
    return VimInjectorKt.getInjector().getPut();
  }

  public static @NotNull VariableService getVariableService() {
    return ApplicationManager.getApplication().getService(VariableService.class);
  }

  public static @NotNull VimOptionGroup getOptionGroup() {
    return VimInjectorKt.getInjector().getOptionGroup();
  }

  private static @NotNull VimNotifications getNotifications() {
    return getNotifications(null);
  }

  public static @NotNull PluginId getPluginId() {
    return PluginId.getId(IDEAVIM_PLUGIN_ID);
  }

  public static @NotNull String getVersion() {
    final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(getPluginId());
    return plugin != null ? plugin.getVersion() : "SNAPSHOT";
  }

  public static boolean isEnabled() {
    return getInstance().enabled;
  }

  public static void setEnabled(final boolean enabled) {
    if (isEnabled() == enabled) return;

    getInstance().enabled = enabled;

    if (enabled) {
      VimInjectorKt.getInjector().getListenersNotifier().notifyPluginTurnedOn();
    }
    else {
      VimInjectorKt.getInjector().getListenersNotifier().notifyPluginTurnedOff();
    }

    if (!enabled) {
      getInstance().turnOffPlugin(true);
    }

    if (enabled) {
      getInstance().turnOnPlugin();
    }

    VimInjectorKt.getInjector().getPluginActivator().updateStatusBarIcon();
  }

  public static boolean isNotEnabled() {
    return !isEnabled();
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

  public static void showMessage(@Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String msg) {
    VimInjectorKt.getInjector().getMessages().showStatusBarMessage(null, msg);
  }

  public static @NotNull VimPlugin getInstance() {
    return ApplicationManager.getApplication().getService(VimPlugin.class);
  }

  public void initialize() {
    LOG.debug("initComponent");

    if (enabled) {
      Application application = ApplicationManager.getApplication();
      if (application.isUnitTestMode()) {
        turnOnPlugin();
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
    turnOffPlugin(false);
    LOG.debug("done");
  }

  /**
   * Activates the IdeaVim plugin. Creates the on/off disposable and delegates
   * the actual activation work to VimPluginActivator.
   */
  private void turnOnPlugin() {
    onOffDisposable = Disposer.newDisposable(this, "IdeaVimOnOffDisposer");
    VimInjectorKt.getInjector().getPluginActivator().activate();
  }

  /**
   * Deactivates the IdeaVim plugin. Delegates work to VimPluginActivator
   * and disposes the on/off disposable.
   */
  private void turnOffPlugin(boolean unsubscribe) {
    VimInjectorKt.getInjector().getPluginActivator().deactivate(unsubscribe);

    if (onOffDisposable != null) {
      Disposer.dispose(onOffDisposable);
      onOffDisposable = null;
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
      VimInjectorKt.getInjector().getMarkService().loadLegacyState(element);
      VimInjectorKt.getInjector().getJumpService().loadLegacyState(element);
      VimRegisterGroup register = getRegister();
      if (register instanceof VimLegacyStateLoader registerLoader) {
        registerLoader.readData(element);
      }
      VimSearchGroup search = getSearch();
      if (search instanceof VimSearchGroupLegacyLoader legacyLoader) {
        legacyLoader.readData(element);
      }
      VimHistory history = getHistory();
      if (history instanceof VimLegacyStateLoader historyLoader) {
        historyLoader.readData(element);
      }
    }
    if (element.getChild("shortcut-conflicts") != null) {
      getKey().loadShortcutConflictsData(element);
    }
    if (element.getChild("editor") != null) {
      getEditor().loadEditorStateData(element);
    }
  }
}
