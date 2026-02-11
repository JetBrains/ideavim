/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.codeWithMe.ClientId
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileOpenedSyncListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.fileEditor.impl.EditorComposite
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.observable.util.addKeyListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.removeUserData
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ExceptionUtil
import com.intellij.util.SlowOperations
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimKeyListener
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.api.LocalOptionInitialisationScenario
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.coerceOffset
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.EditorGroup
import com.maddyhome.idea.vim.group.FileGroup
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.group.IjVimRedrawService
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.OptionGroup
import com.maddyhome.idea.vim.group.ScrollGroup
import com.maddyhome.idea.vim.group.VimMarkServiceImpl
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.group.visual.VimVisualTimer
import com.maddyhome.idea.vim.group.visual.moveCaretOneCharLeftFromSelectionEnd
import com.maddyhome.idea.vim.handler.correctorRequester
import com.maddyhome.idea.vim.handler.keyCheckRequests
import com.maddyhome.idea.vim.helper.CaretVisualAttributesListener
import com.maddyhome.idea.vim.helper.GuicursorChangeListener
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.forceBarCursor
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.resetVimLastColumn
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.helper.vimDisabled
import com.maddyhome.idea.vim.helper.vimInitialised
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.IjVimSearchGroup
import com.maddyhome.idea.vim.newapi.InsertTimeRecorder
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.inSelectMode
import com.maddyhome.idea.vim.state.mode.selectionType
import com.maddyhome.idea.vim.ui.ShowCmdOptionChangeListener
import com.maddyhome.idea.vim.ui.ShowCmdWidgetUpdater
import com.maddyhome.idea.vim.ui.widgets.macro.MacroWidgetListener
import com.maddyhome.idea.vim.ui.widgets.macro.macroWidgetOptionListener
import com.maddyhome.idea.vim.ui.widgets.mode.listeners.ModeWidgetListener
import com.maddyhome.idea.vim.ui.widgets.mode.modeWidgetOptionListener
import org.jetbrains.annotations.TestOnly
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.lang.ref.WeakReference
import java.util.*
import javax.swing.SwingUtilities

/**
 * @author Alex Plate
 */


/**
 * Get the editor that was in use when the new editor was opened
 *
 * We need this to copy its window local options. This will be the editor that hosted the command line for
 * `:edit {file}` or was the source of a ctrl+click navigation.
 *
 * Unfortunately, we're not given enough context to know what caused the new editor to open (and we might be
 * initialising already open editors after disabling/enabling the plugin). So we'll use the currently selected editor.
 * This will be the last focused editor, so will work for Vim command line commands, ctrl+click, etc. It also works for
 * opening a file from Search Everywhere or a tool window such as the Project view or Find Usages results.
 *
 * Make sure the selected editor isn't the new editor, which can happen if there are no other editors open.
 */
private fun getOpeningEditor(newEditor: Editor) = newEditor.project?.let { project ->
  // We can't rely on FileEditorManager.selectedTextEditor because we're trying to retrieve the selected text editor
  // while creating a text editor that is about to become the selected text editor.
  // This worked fine for 2024.2, but internal changes for 2024.3 broke things. It appears that the currently selected
  // text editor is reset to null while the soon-to-be-selected text editor is being created. We therefore track the
  // last selected editor manually.
  // Note that if we ever switch back to FileEditorManager.selectedTextEditor, be careful of recursion, because the
  // actual editor might be created on-demand, which would notify our initialisation method, which would call us...
  VimListenerManager.VimLastSelectedEditorTracker.getLastSelectedEditor(project)?.takeUnless { it == newEditor }
}

internal object VimListenerManager {

  private val logger = Logger.getInstance(VimListenerManager::class.java)
  private val editorListenersDisposableKey = Key.create<Disposable>("IdeaVim listeners disposable")
  private var firstEditorInitialised = false

  fun turnOn() {
    GlobalListeners.enable()
    SlowOperations.knownIssue("VIM-3648, VIM-3649").use {
      EditorListeners.addAll()
    }
    check(correctorRequester.tryEmit(Unit))
    check(keyCheckRequests.tryEmit(Unit))

    val caretVisualAttributesListener = CaretVisualAttributesListener()
    injector.listenersNotifier.myEditorListeners.add(caretVisualAttributesListener)
    injector.listenersNotifier.modeChangeListeners.add(caretVisualAttributesListener)
    injector.listenersNotifier.isReplaceCharListeners.add(caretVisualAttributesListener)
    ApplicationManager.getApplication().invokeAndWait {
      caretVisualAttributesListener.updateAllEditorsCaretsVisual()
    }

    val insertTimeRecorder = InsertTimeRecorder()
    injector.listenersNotifier.modeChangeListeners.add(insertTimeRecorder)

    val modeWidgetListener = ModeWidgetListener()
    injector.listenersNotifier.modeChangeListeners.add(modeWidgetListener)
    injector.listenersNotifier.myEditorListeners.add(modeWidgetListener)
    injector.listenersNotifier.vimPluginListeners.add(modeWidgetListener)

    val macroWidgetListener = MacroWidgetListener()
    injector.listenersNotifier.macroRecordingListeners.add(macroWidgetListener)
    injector.listenersNotifier.vimPluginListeners.add(macroWidgetListener)

    injector.listenersNotifier.myEditorListeners.add(IJEditorFocusListener())
    injector.listenersNotifier.myEditorListeners.add(ShowCmdWidgetUpdater())
  }

  fun turnOff() {
    GlobalListeners.disable()
    EditorListeners.removeAll()
    injector.listenersNotifier.reset()

    check(correctorRequester.tryEmit(Unit))
  }

  object GlobalListeners {
    fun enable() {
      val typedAction = TypedAction.getInstance()
      if (typedAction.rawHandler !is VimTypedActionHandler) {
        // Actually this if should always be true, but just as protection
        EventFacade.getInstance().setupTypedActionHandler(VimTypedActionHandler(typedAction.rawHandler))
      } else {
        StrictMode.fail("typeAction expected to be non-vim.")
      }

      val optionGroup = VimPlugin.getOptionGroup()
      optionGroup.addEffectiveOptionValueChangeListener(Options.number, EditorGroup.NumberChangeListener.INSTANCE)
      optionGroup.addEffectiveOptionValueChangeListener(
        IjOptions.relativenumber,
        EditorGroup.NumberChangeListener.INSTANCE
      )
      optionGroup.addEffectiveOptionValueChangeListener(Options.scrolloff, ScrollGroup.ScrollOptionsChangeListener)
      optionGroup.addEffectiveOptionValueChangeListener(Options.guicursor, GuicursorChangeListener)
      optionGroup.addGlobalOptionChangeListener(Options.showcmd, ShowCmdOptionChangeListener)

      // This code is executed after ideavimrc execution, so we trigger onGlobalOptionChanged just in case
      optionGroup.addGlobalOptionChangeListener(Options.showmode, modeWidgetOptionListener)
      optionGroup.addGlobalOptionChangeListener(Options.showmode, macroWidgetOptionListener)
      modeWidgetOptionListener.onGlobalOptionChanged()
      macroWidgetOptionListener.onGlobalOptionChanged()

      // Listen for and initialise new editors
      EventFacade.getInstance()
        .addEditorFactoryListener(VimEditorFactoryListener, VimPlugin.getInstance().onOffDisposable)
      val busConnection =
        ApplicationManager.getApplication().messageBus.connect(VimPlugin.getInstance().onOffDisposable)
      busConnection.subscribe(FileOpenedSyncListener.TOPIC, VimEditorFactoryListener)
    }

    fun disable() {
      EventFacade.getInstance().restoreTypedActionHandler()

      val optionGroup = VimPlugin.getOptionGroup()
      optionGroup.removeEffectiveOptionValueChangeListener(Options.number, EditorGroup.NumberChangeListener.INSTANCE)
      optionGroup.removeEffectiveOptionValueChangeListener(
        IjOptions.relativenumber,
        EditorGroup.NumberChangeListener.INSTANCE
      )
      optionGroup.removeEffectiveOptionValueChangeListener(Options.scrolloff, ScrollGroup.ScrollOptionsChangeListener)
      optionGroup.removeGlobalOptionChangeListener(Options.showcmd, ShowCmdOptionChangeListener)
      optionGroup.removeGlobalOptionChangeListener(Options.showmode, modeWidgetOptionListener)
      optionGroup.removeGlobalOptionChangeListener(Options.showmode, macroWidgetOptionListener)
      optionGroup.removeEffectiveOptionValueChangeListener(Options.guicursor, GuicursorChangeListener)
    }
  }

  object EditorListeners {
    fun addAll() {
      val initialisedEditors = mutableSetOf<Editor>()

      // We are initialising all currently open editors. We treat the currently selected editor (per-project) as the
      // opening editor for all other editors. Make sure it's initialised first, and with FALLBACK to get the settings
      // from `~/.ideavimrc`. All other editors will be initialised as NEW from the project's selected editor
      ProjectManager.getInstanceIfCreated()?.let { projectManager ->
        projectManager.openProjects.forEach { project ->
          FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            val scenario = if (!firstEditorInitialised) {
              LocalOptionInitialisationScenario.FALLBACK
            } else {
              LocalOptionInitialisationScenario.EDIT
            }
            add(editor, injector.fallbackWindow, scenario)
            initialisedEditors.add(editor)
            firstEditorInitialised = true
          }
        }
      }

      // We could have a split window in this list, but since they're all being initialised from the same opening editor
      // there's no need to use the SPLIT scenario
      // Make sure we get all editors, including uninitialised
      injector.editorGroup.getEditorsRaw().forEach { vimEditor ->
        val editor = vimEditor.ij
        if (!initialisedEditors.contains(editor)) {
          add(editor, getOpeningEditor(editor)?.vim ?: injector.fallbackWindow, LocalOptionInitialisationScenario.NEW)
        }
      }
    }

    fun removeAll() {
      injector.editorGroup.getEditors().forEach { editor ->
        remove(editor.ij)
      }
    }

    fun add(editor: Editor, openingEditor: VimEditor, scenario: LocalOptionInitialisationScenario) {
      // We shouldn't be called with anything other than local editors, but let's just be sure. This will prevent any
      // unsupported editor from incorrectly being initialised.
      // TODO: If the user changes the 'ideavimsupport' option, existing editors won't be initialised
      if (vimDisabled(editor)) return

      // Protect against double initialisation
      if (editor.getUserData(editorListenersDisposableKey) != null) {
        return
      }

      // Make sure we explicitly dispose this per-editor disposable!
      // Because the listeners are registered with a parent disposable, they add child disposables that have to call a
      // method on the editor to remove the listener. This means the disposable contains a reference to the editor (even
      // if the listener handler is a singleton that doesn't hold a reference).
      // Unless the per-editor disposable is disposed, all of these disposables sit in the disposer tree until the
      // parent disposable is disposed, which will mean we leak editor instances.
      // The per-editor disposable is explicitly disposed when the editor is released, and disposed via its parent when
      // the plugin's on/off functionality is toggled, and so also when the plugin is disabled/unloaded by the platform.
      // It doesn't matter if we explicitly remove all listeners before disposing onOffDisposable, as that will remove
      // the per-editor disposable from the disposer tree.
      val perEditorDisposable = Disposer.newDisposable(VimPlugin.getInstance().onOffDisposable)
      editor.putUserData(editorListenersDisposableKey, perEditorDisposable)

      Disposer.register(perEditorDisposable) {
        if (VimListenerTestObject.enabled) {
          VimListenerTestObject.disposedCounter += 1
        }
      }

      // This listener and several below add a reference to the editor to the disposer tree
      editor.contentComponent.addKeyListener(perEditorDisposable, VimKeyListener)

      // Initialise the local options. We MUST do this before anything has the chance to query options
      val vimEditor = editor.vim
      VimPlugin.getOptionGroup().initialiseLocalOptions(vimEditor, openingEditor, scenario)

      val eventFacade = EventFacade.getInstance()
      eventFacade.addEditorMouseListener(editor, EditorMouseHandler, perEditorDisposable)
      eventFacade.addEditorMouseMotionListener(editor, EditorMouseHandler, perEditorDisposable)
      eventFacade.addEditorSelectionListener(editor, EditorSelectionHandler, perEditorDisposable)
      eventFacade.addComponentMouseListener(editor.contentComponent, ComponentMouseListener, perEditorDisposable)
      eventFacade.addCaretListener(editor, EditorCaretHandler, perEditorDisposable)

      VimPlugin.getEditor().editorCreated(editor)
      VimPlugin.getChange().editorCreated(editor, perEditorDisposable)

      (editor as EditorEx).addFocusListener(VimFocusListener, perEditorDisposable)

      injector.listenersNotifier.notifyEditorCreated(vimEditor)

      Disposer.register(perEditorDisposable) {
        ApplicationManager.getApplication().invokeLater {
          VimPlugin.getEditor().editorDeinit(editor)
        }
      }
    }

    fun remove(editor: Editor) {
      val editorDisposable = editor.removeUserData(editorListenersDisposableKey)
      if (editorDisposable != null) {
        Disposer.dispose(editorDisposable)
      } else {
        // We definitely do not expect this to happen
        StrictMode.fail("Editor doesn't have disposable attached. $editor")
      }
    }
  }

  /**
   * Notifies other IdeaVim components of focus gain/loss, e.g. the mode widget. This will be called with non-local Code
   * With Me editors.
   */
  private object VimFocusListener : FocusChangeListener {
    override fun focusGained(editor: Editor) {
      if (vimDisabled(editor)) return
      injector.listenersNotifier.notifyEditorFocusGained(editor.vim)
    }

    override fun focusLost(editor: Editor) {
      if (vimDisabled(editor)) return
      injector.listenersNotifier.notifyEditorFocusLost(editor.vim)
    }
  }

  /**
   * Notifies other IdeaVim components of document changes. This will be called for all documents, even those only
   * open in non-local Code With Me guest editors, which we still want to process (e.g. to update marks when a guest
   * edits a file. Updating search highlights will be a no-op if there are no open local editors)
   */
  class VimDocumentListener : DocumentListener {
    override fun beforeDocumentChange(event: DocumentEvent) {
      VimMarkServiceImpl.MarkUpdater.beforeDocumentChange(event)
      IjVimSearchGroup.DocumentSearchListener.INSTANCE.beforeDocumentChange(event)
      IjVimRedrawService.RedrawListener.beforeDocumentChange(event)
    }

    override fun documentChanged(event: DocumentEvent) {
      VimMarkServiceImpl.MarkUpdater.documentChanged(event)
      IjVimSearchGroup.DocumentSearchListener.INSTANCE.documentChanged(event)
      IjVimRedrawService.RedrawListener.documentChanged(event)
    }
  }

  internal object VimLastSelectedEditorTracker {
    // This stores a weak reference to an editor against a weak reference to a project, which means there is nothing
    // keeping the project or editor from being garbage collected at any time. Stale keys are automatically expunged
    // whenever the map is used.
    private val selectedEditors = WeakHashMap<Project, WeakReference<Editor>>()

    fun getLastSelectedEditor(project: Project): Editor? = selectedEditors[project]?.get()

    internal fun setLastSelectedEditor(fileEditor: FileEditor?) {
      (fileEditor as? TextEditor)?.editor?.let { editor ->
        editor.project?.let { project -> selectedEditors[project] = WeakReference(editor) }
      }
    }

    @TestOnly
    internal fun resetLastSelectedEditor(project: Project) {
      selectedEditors.remove(project)
    }
  }

  /**
   * Called when the selected file editor changes. In other words, when the user selects a new tab. Used to remember the
   * last selected file, update search highlights in the new tab, etc. This will be called with non-local Code With Me
   * guest editors.
   */
  class VimFileEditorManagerListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
      // We can't rely on being passed a non-null editor, so check for Code With Me scenarios explicitly
      if (VimPlugin.isNotEnabled() || !ClientId.isCurrentlyUnderLocalId) return

      injector.outputPanel.getCurrentOutputPanel()?.close()
      MotionGroup.fileEditorManagerSelectionChangedCallback(event)
      FileGroup.fileEditorManagerSelectionChangedCallback(event)
      VimPlugin.getSearch().fileEditorManagerSelectionChangedCallback(event)
      IjVimRedrawService.fileEditorManagerSelectionChangedCallback(event)
      VimLastSelectedEditorTracker.setLastSelectedEditor(event.newEditor)
    }
  }

  /**
   * Listen to editor creation events in order to initialise IdeaVim compatible editors. This listener is called for all
   * editors, including non-local hidden Code With Me editors.
   */
  private object VimEditorFactoryListener : EditorFactoryListener, FileOpenedSyncListener {
    private data class OpeningEditor(
      val editor: Editor,
      val owningEditorWindow: EditorWindow?,
      val isPreview: Boolean,
      val canBeReused: Boolean,
    )

    private val openingEditorKey: Key<OpeningEditor> = Key("IdeaVim::OpeningEditor")

    override fun editorCreated(event: EditorFactoryEvent) {
      if (vimDisabled(event.editor)) return

      // This callback is called when an editor is created, but we cannot completely rely on it to initialise options.
      // We can find the currently selected editor, which we can use as the opening editor, and we're given the new
      // editor, but we don't know enough about it - this function is called before the new editor is added to an
      // EditorComposite and before that is added to an EditorWindow's tabbed container and finally an EditorsSplitter.
      // If it's a main file editor, the `FileOpenedSyncListener.fileOpenedSync` callback will allow us to find out, but
      // by that point, the opening editor might have been closed (i.e. if it's a preview tab or if the user has
      // selected to reuse unmodified tabs). If it's not a main editor, or backed by a real file, then that callback
      // isn't called, so we need to initialise early.
      val openingEditor = getOpeningEditor(event.editor)

      if (event.editor.virtualFile == null || event.editor.editorKind != EditorKind.MAIN_EDITOR || openingEditor == null) {
        // If we don't have an opening editor, use the fallback window. If it's the first time, use the FALLBACK
        // scenario and make a full copy to get everything set in `~/.ideavimrc`. If it's not, then use EDIT, as if we
        // still had a current window and we are just replacing the buffer. If we do have an opening window, it's NEW.
        // Preview and reused tabs are handled below
        val scenario = when {
          openingEditor == null && !firstEditorInitialised -> LocalOptionInitialisationScenario.FALLBACK
          openingEditor == null -> LocalOptionInitialisationScenario.EDIT
          else -> LocalOptionInitialisationScenario.NEW
        }
        SlowOperations.knownIssue("VIM-3648").use {
          EditorListeners.add(event.editor, openingEditor?.vim ?: injector.fallbackWindow, scenario)
        }
        firstEditorInitialised = true
      } else {
        // We've got a virtual file, so FileOpenedSyncListener will be called. Save data
        val project = openingEditor.project ?: return
        val virtualFile = openingEditor.virtualFile ?: return
        val manager = FileEditorManager.getInstance(project)

        // If the opening tab is a preview tab, and the new editor is in the same split, the preview tab will be
        // replaced, and we should use EDIT. If the new editor is in a different split, then it would be NEW
        val isPreview = manager.getComposite(virtualFile)?.isPreview ?: false

        // If the user has enabled "Open declaration source in the same tab", the opening editor will be replaced as
        // long as it's not pinned, and it's not modified, and we're in the same split
        val canBeReused = UISettings.getInstance().reuseNotModifiedTabs &&
          (manager.getComposite(virtualFile) as? EditorComposite)?.let { composite ->
            !composite.isPinned && !composite.isModified
          } ?: false

        // Keep a track of the owner of the opening editor, so we can compare later, potentially after the opening
        // editor has been closed. This is nullable, but should always have a value
        val owningEditorWindow = getOwningEditorWindow(openingEditor)

        event.editor.putUserData(
          openingEditorKey,
          OpeningEditor(openingEditor, owningEditorWindow, isPreview, canBeReused)
        )
      }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
      if (vimDisabled(event.editor)) return
      val vimEditor = event.editor.vim
      EditorListeners.remove(event.editor)
      injector.listenersNotifier.notifyEditorReleased(vimEditor)
      injector.markService.editorReleased(vimEditor)

      // This ticket will have a different stack trace, but it's the same problem. Originally, we tracked the last
      // editor closing based on file selection (closing an editor would select the next editor - so a null selection
      // was taken to mean that there were no more editors to select). This assumption broke in 242, so it's changed to
      // check when the editor is released.
      // However, the actions taken when the last editor closes can still be expensive/slow because we copy options, and
      // some options are backed by PSI options. E.g. 'textwidth' is mapped to
      // CodeStyle.getSettings(ijEditor).isWrapOnTyping(language)), and getting the document's PSI language is a slow
      // operation. This underlying issue still needs to be addressed, even though the method has moved
      SlowOperations.knownIssue("VIM-3658").use {
        OptionGroup.editorReleased(event.editor)
      }
    }

    override fun fileOpenedSync(
      source: FileEditorManager,
      file: VirtualFile,
      editorsWithProviders: List<FileEditorWithProvider>,
    ) {
      // This callback is called once all editors are created for a file being opened. The EditorComposite has been
      // created (and the list of editors and providers is passed here) and added to an EditorWindow tab, inside a
      // splitter. We now know where the new editor is located, and we have stored the details of the opening editor
      // (which might no longer be open). It is still safe to use the editor itself because we're still synchronous to
      // where it's been removed, and we only need its user data, but make sure not to hold on to it and leak it
      // Note that the default scenario is NEW, because IdeaVim does not currently implement `:edit {file}` correctly.
      // It does not edit in place, in the current window, but opens a new window, so behaves like `:new {file}`. At
      // some point we should implement `:edit` like we do preview tabs, or reusing tabs, by opening a new editor and
      // closing the old one. We could identify this by adding user data to the virtual file in EditFileCommand, or
      // possibly by temporarily enabling the "reuse unmodified tabs" setting. We would also need to handle if the
      // editor is modified
      editorsWithProviders.forEach {
        (it.fileEditor as? TextEditor)?.editor?.let { editor ->
          if (vimDisabled(editor)) return@let

          // Protect against double initialisation, in case the editor was already initialised in editorCreated
          if (editor.vimInitialised) return@let

          val openingEditor = editor.removeUserData(openingEditorKey)
          val owningEditorWindow = getOwningEditorWindow(editor)
          val isInSameSplit = owningEditorWindow == openingEditor?.owningEditorWindow

          // Sometimes the platform will not reuse a tab when you expect it to, e.g. when reuse tabs is enabled and
          // navigating to derived class. We'll confirm our heuristics by checking to see if the editor is still around
          val openingEditorIsClosed = editor.project?.let { p ->
            FileEditorManagerEx.getInstanceEx(p).allEditors.filterIsInstance<TextEditor>().all { textEditor ->
              textEditor.editor != openingEditor?.editor
            }
          } ?: false

          // Use fallback if there's no editor, but only once. Next time round, use the fallback window, but treat it as
          // the EDIT scenario as though we hadn't closed the last window (Vim never does)
          val scenario = when {
            openingEditor == null -> if (!firstEditorInitialised) LocalOptionInitialisationScenario.FALLBACK else LocalOptionInitialisationScenario.EDIT
            editor.document == openingEditor.editor.document -> LocalOptionInitialisationScenario.SPLIT
            (openingEditor.canBeReused || openingEditor.isPreview) && isInSameSplit && openingEditorIsClosed -> LocalOptionInitialisationScenario.EDIT
            else -> LocalOptionInitialisationScenario.NEW
          }
          EditorListeners.add(editor, openingEditor?.editor?.vim ?: injector.fallbackWindow, scenario)
          firstEditorInitialised = true
        }
      }
    }

    private fun getOwningEditorWindow(editor: Editor) = editor.project?.let { p ->
      FileEditorManagerEx.getInstanceEx(p).windows.find { editorWindow ->
        editorWindow.allComposites.any { composite ->
          composite.allEditors.filterIsInstance<TextEditor>().any { it.editor == editor }
        }
      }
    }
  }


  /**
   * Callback for when an editor's text selection changes. Only registered for editors that we're interested in (so only
   * local editors). Fixes incorrect mouse selection at end of line, and synchronises selections across other editors.
   */
  private object EditorSelectionHandler : SelectionListener {
    /**
     * This event is executed for each caret using [com.intellij.openapi.editor.CaretModel.runForEachCaret]
     */
    override fun selectionChanged(selectionEvent: SelectionEvent) {
      VimVisualTimer.drop()
      val editor = selectionEvent.editor
      val document = editor.document
      val ijVimEditor = IjVimEditor(editor)

      logger.trace { "Selection changed" }
      logger.trace { ExceptionUtil.currentStackTrace() }

      //region Unselected last character protection
      // There is currently a bug in IJ for IdeaVim where on selecting from EOL
      // and dragging left or vertically upwards, the last character prior to EOL
      // remains unselected. It's not clear why this happens, but this code fixes it.
      val caret = editor.caretModel.currentCaret
      val caretOffset = ApplicationManager.getApplication().runReadAction<Int> { caret.offset }
      val lineStart = ijVimEditor.getLineStartForOffset(caretOffset)
      val lineEnd = ijVimEditor.getLineEndForOffset(caretOffset)
      val startOffset = selectionEvent.newRange.startOffset
      val endOffset = selectionEvent.newRange.endOffset

      // TODO: It is very confusing that this logic is split between EditorSelectionHandler and EditorMouseHandler
      if (MouseEventsDataHolder.dragEventCount < MouseEventsDataHolder.allowedSkippedDragEvents
        && lineStart != lineEnd && startOffset == caretOffset
      ) {
        if (lineEnd == endOffset - 1) {
          // When starting on an empty line and dragging vertically upwards onto
          // another line, the selection should include the entirety of the empty line
          caret.setSelection(
            ijVimEditor.coerceOffset(endOffset + 1),
            ijVimEditor.coerceOffset(startOffset),
          )
        } else if (lineEnd == startOffset + 1 && startOffset == endOffset) {
          // When dragging left from EOL on a non-empty line, the selection
          // should include the last character on the line
          caret.setSelection(
            ijVimEditor.coerceOffset(lineEnd),
            ijVimEditor.coerceOffset(lineEnd - 1),
          )
        }
      }
      //endregion

      if (SelectionVimListenerSuppressor.isNotLocked) {
        logger.debug("Adjust non vim selection change")
        IdeaSelectionControl.controlNonVimSelectionChange(editor)
      }

      if (document is DocumentEx && document.isInEventsHandling) {
        return
      }
    }
  }

  /**
   * Listener for mouse events registered with editors that we are interested (so only local editors). Responsible for:
   * * Hiding ex entry and output panels when clicking inside editor area (but not when right-clicking)
   * * Removing secondary carets on mouse click (such as visual block selection)
   * * Exiting visual or select mode on mouse click
   * * Resets the dragEventCount on mouse press + release
   * * Fix up Vim selected mode on mouse release, after dragging
   * * Force bar cursor while dragging, which looks better because IntelliJ selects a character once selection has got
   *   over halfway through the char, while Vim selects when it enters the character bounding box
   *
   * @see ComponentMouseListener
   */
  // TODO: Can we merge this with ComponentMouseListener to fully handle all mouse actions in one place?
  private object EditorMouseHandler : EditorMouseListener, EditorMouseMotionListener {
    private var mouseDragging = false
    private var cutOffFixed = false

    override fun mouseDragged(e: EditorMouseEvent) {
      val editor = e.editor
      if (editor.isIdeaVimDisabledHere) return
      val caret = editor.caretModel.primaryCaret

      clearFirstSelectionEvents(e)

      if (mouseDragging && caret.hasSelection()) {
        /**
         * We force the bar caret while dragging because it matches IntelliJ's selection model better.
         * * Vim's drag selection is based on character bounding boxes. When 'selection' is set to "inclusive" (the
         *   default), Vim selects a character when the mouse cursor drags the text caret into its bounding box (LTR).
         *   The character at the text caret is selected and the block caret is drawn to cover the character (the bar
         *   caret would be between the selection and the last character of the selection, which is weird). See "v" in
         *   'guicursor'. When 'selection' is "exclusive", Vim will select a character when the mouse cursor drags the
         *   text caret out of its bounding box. The character at the text caret is not selected and the bar caret is
         *   drawn at the start of this character to make it more obvious that it is unselected. See "ve" in
         *   'guicursor'.
         * * IntelliJ's selection is based on character mid-points. E.g. the caret is moved to the start of offset 2
         *   when the second half of offset 1 is clicked, and a character is selected when the mouse is moved from the
         *   first half to the second half. This means:
         *   1) While dragging, the selection is always exclusive - the character at the text caret is not selected. We
         *   convert to an inclusive selection when the mouse is released, by moving back one character. It makes
         *   sense to match Vim's bar caret here.
         *   2) An exclusive selection should trail behind the mouse cursor, but IntelliJ doesn't, because the selection
         *   boundaries are mid-points - the text caret can be in front of/to the right of the mouse cursor (LTR).
         *   Using a block caret would push the block further out passed the selection and the mouse cursor, and
         *   feels wrong. The bar caret is a better user experience.
         *   RTL probably introduces other fun issues
         * We can implement inclusive/exclusive 'selection' with normal text movement, but unless we can change the way
         * selection works while dragging, I don't think we can match Vim's selection behaviour exactly.
         */
        caret.forceBarCursor()

        if (!cutOffFixed && ComponentMouseListener.cutOffEnd) {
          cutOffFixed = true
          SelectionVimListenerSuppressor.lock().use {
            if (caret.selectionEnd == e.editor.document.getLineEndOffset(caret.logicalPosition.line) - 1 &&
              caret.leadSelectionOffset == caret.selectionEnd
            ) {
              // A small but important customization. Because IdeaVim doesn't allow to put the caret on the line end,
              //   the selection can omit the last character if the selection was started in the middle on the
              //   last character in line and has a negative direction.
              caret.setSelection(caret.selectionStart, caret.selectionEnd + 1)
            }
            // This is the same correction, but for the newer versions of the IDE: 213+
            if (caret.selectionEnd == e.editor.document.getLineEndOffset(caret.logicalPosition.line) &&
              caret.selectionEnd == caret.selectionStart + 1
            ) {
              caret.setSelection(caret.selectionEnd, caret.selectionEnd)
            }
          }
        }
      }
      MouseEventsDataHolder.dragEventCount -= 1
    }

    /**
     * When user places the caret, sometimes they perform a small drag. This doesn't affect clear IJ, but with IdeaVim
     * it may introduce unwanted selection. Here we remove any selection if "dragging" happens for less than 3 events.
     * This is because the first click moves the caret passed the end of the line, is then received in
     * [ComponentMouseListener] and the caret is moved back to the start of the last character of the line. If there is
     * a drag, this translates to a selection of the last character. In this case, remove the selection.
     * We force the bar caret simply because it looks better - the block caret is dragged to the end, becomes a less
     * intrusive bar caret and snaps back to the last character (and block caret) when the mouse is released.
     * TODO: Vim supports selection of the character after the end of line
     * (Both with mouse and with v$. IdeaVim treats v$ as an exclusive selection)
     */
    private fun clearFirstSelectionEvents(e: EditorMouseEvent) {
      if (MouseEventsDataHolder.dragEventCount > 0) {
        logger.debug("Mouse dragging")
        VimVisualTimer.swingTimer?.stop()
        if (!mouseDragging) {
          SelectionVimListenerSuppressor.lock()
        }
        mouseDragging = true

        val caret = e.editor.caretModel.primaryCaret
        if (onLineEnd(caret)) {
          SelectionVimListenerSuppressor.lock().use {
            caret.removeSelection()
            caret.forceBarCursor()
          }
        }
      }
    }

    private fun onLineEnd(caret: Caret): Boolean {
      val editor = caret.editor
      val lineEnd = IjVimEditor(editor).getLineEndForOffset(caret.offset)
      val lineStart = IjVimEditor(editor).getLineStartForOffset(caret.offset)
      return caret.offset == lineEnd && lineEnd != lineStart && caret.offset - 1 == caret.selectionStart && caret.offset == caret.selectionEnd
    }

    override fun mousePressed(event: EditorMouseEvent) {
      if (event.editor.isIdeaVimDisabledHere) return
      MouseEventsDataHolder.dragEventCount = MouseEventsDataHolder.allowedSkippedDragEvents
      SelectionVimListenerSuppressor.reset()
    }

    /**
     * This method may not be called
     * Known cases:
     * - Click-hold and close editor (ctrl-w)
     * - Click-hold and switch editor (ctrl-tab)
     */
    override fun mouseReleased(event: EditorMouseEvent) {
      if (event.editor.isIdeaVimDisabledHere) return
      SelectionVimListenerSuppressor.unlock()

      clearFirstSelectionEvents(event)
      MouseEventsDataHolder.dragEventCount = MouseEventsDataHolder.allowedSkippedDragEvents
      if (mouseDragging) {
        logger.debug("Release mouse after dragging")
        val editor = event.editor
        SelectionVimListenerSuppressor.lock().use {
          val predictedMode = injector.application
            .runReadAction { IdeaSelectionControl.predictMode(editor, SelectionSource.MOUSE) }
          IdeaSelectionControl.controlNonVimSelectionChange(editor, SelectionSource.MOUSE)
          // TODO: This should only be for 'selection'=inclusive
          moveCaretOneCharLeftFromSelectionEnd(editor, predictedMode)

          // Reset caret after forceBarShape while dragging
          editor.updateCaretsVisualAttributes()
        }

        mouseDragging = false
        cutOffFixed = false
      }
    }

    override fun mouseClicked(event: EditorMouseEvent) {
      if (event.editor.isIdeaVimDisabledHere) return
      logger.debug("Mouse clicked")

      if (event.area == EditorMouseEventArea.EDITING_AREA) {
        val editor = event.editor
        injector.commandLine.getActiveCommandLine()?.close(refocusOwningEditor = true, resetCaret = false)
        injector.modalInput.getCurrentModalInput()?.deactivate(refocusOwningEditor = true, resetCaret = false)

        injector.outputPanel.getCurrentOutputPanel()?.close()

        val caretModel = editor.caretModel
        if (editor.vim.mode.selectionType != null) {
          caretModel.removeSecondaryCarets()
        }

        // Removing selection on just clicking.
        //
        // Actually, this event should not be fired on right click (when the menu appears), but since 202 it happens
        //   sometimes. To prevent unwanted selection removing, an assertion for isRightClick was added.
        // See:
        //   https://youtrack.jetbrains.com/issue/IDEA-277716
        //   https://youtrack.jetbrains.com/issue/VIM-2368
        if (event.mouseEvent.clickCount == 1 && !SwingUtilities.isRightMouseButton(event.mouseEvent)) {
          val hasSelection = ApplicationManager.getApplication().runReadAction<Boolean> {
            editor.selectionModel.hasSelection(true)
          }
          if (!hasSelection) {
            if (editor.inVisualMode) {
              editor.vim.exitVisualMode()
            } else if (editor.vim.inSelectMode) {
              editor.vim.exitSelectMode(false)
              KeyHandler.getInstance().reset(editor.vim)
            }
          }
        }
      } else if (event.area != EditorMouseEventArea.ANNOTATIONS_AREA &&
        event.area != EditorMouseEventArea.FOLDING_OUTLINE_AREA &&
        event.mouseEvent.button != MouseEvent.BUTTON3
      ) {
        injector.commandLine.getActiveCommandLine()?.close(refocusOwningEditor = true, resetCaret = false)
        injector.modalInput.getCurrentModalInput()?.deactivate(refocusOwningEditor = true, resetCaret = false)

        injector.outputPanel.getCurrentOutputPanel()?.close()
      }
    }
  }

  /**
   * A mouse listener registered to the editor component for editors that we are interested in (so only local editors).
   * Fixes some issues with mouse selection, namely:
   * * Clicking at the end of the line will place the caret on the last character rather than after it
   * * Double-clicking a word will place the caret on the last character rather than after it
   *
   * @see EditorMouseHandler
   */
  // TODO: Can we merge this with ComponentMouseListener to fully handle all mouse actions in one place?
  private object ComponentMouseListener : MouseAdapter() {

    var cutOffEnd = false

    override fun mousePressed(e: MouseEvent?) {
      val editor = (e?.component as? EditorComponentImpl)?.editor ?: return
      if (editor.isIdeaVimDisabledHere) return
      val predictedMode = injector.application.runReadAction {
        IdeaSelectionControl.predictMode(editor, SelectionSource.MOUSE)
      }
      when (e.clickCount) {
        1 -> {
          // If you click after the line, the caret is placed by IJ after the last symbol.
          // This is not allowed in some vim modes, so we move the caret over the last symbol.
          if (!editor.vim.isEndAllowed(predictedMode)) {
            @Suppress("ideavimRunForEachCaret")
            editor.caretModel.runForEachCaret { caret ->
              val lineEnd = IjVimEditor(editor).getLineEndForOffset(caret.offset)
              val lineStart = IjVimEditor(editor).getLineStartForOffset(caret.offset)
              cutOffEnd = if (caret.offset == lineEnd && lineEnd != lineStart) {
                caret.moveToInlayAwareOffset(caret.offset - 1)
                true
              } else {
                false
              }
            }
          } else {
            cutOffEnd = false
          }
        }
        // Double-clicking a word in IntelliJ will select the word and locate the caret at the end of the selection,
        // on the following character. When using a bar caret, this is drawn as between the end of selection and the
        // following char. With a block caret, this draws the caret "over" the following character.
        // In Vim, when 'selection' is "inclusive" (default), double clicking a word will select the last character of
        // the word and leave the caret on the last character, drawn as a block caret. We move one character left to
        // match this behaviour.
        // When 'selection' is exclusive, the caret is placed *after* the end of the word, and is drawn using the 've'
        // option of 'guicursor' - as a bar, so it appears to be in between the end of the word and the start of the
        // following character.
        // TODO: Modify this to support 'selection' set to "exclusive"
        2 -> moveCaretOneCharLeftFromSelectionEnd(editor, predictedMode)
      }
    }
  }

  /**
   * Caret listener registered only for editors that we're interested in. Used to update caret shapes when carets are
   * added/removed. Also tracks the expected last column location of the caret.
   */
  private object EditorCaretHandler : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
      event.caret?.resetVimLastColumn()
    }

    override fun caretAdded(event: CaretEvent) {
      event.editor.updateCaretsVisualAttributes()
    }

    override fun caretRemoved(event: CaretEvent) {
      event.editor.updateCaretsVisualAttributes()
    }
  }

  enum class SelectionSource {
    MOUSE,
    OTHER,
  }
}

internal object VimListenerTestObject {
  var enabled: Boolean = false
  var disposedCounter = 0
}

private object MouseEventsDataHolder {
  const val allowedSkippedDragEvents = 3
  var dragEventCount = allowedSkippedDragEvents
}
