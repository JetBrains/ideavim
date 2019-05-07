package com.maddyhome.idea.vim.ui

import com.intellij.openapi.actionSystem.*
import com.maddyhome.idea.vim.VimPlugin
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * An IntelliJ action to forward shortcuts to the ex entry component
 * <p>
 * Key events are processed by the IDE action system before they are dispatched to the actual component, which means
 * they take precedence over the keyboard shortcuts registered with the ex component as Swing actions. This can cause
 * clashes such as <C-R> invoking the Run action instead of the Paste Register ex action, or <BS> being handled by the
 * editor rather than allowing us to cancel the ex entry.
 * <p>
 * This class is an IDE action that is registered with the ex entry component, so is available only when the ex entry
 * component has focus. It registers all shortcuts used by the Swing actions and forwards them directly to the key
 * handler.
 */
class ExShortcutKeyAction(private val exEntryPanel: ExEntryPanel) : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val keyStroke = getKeyStroke(e)
        if (keyStroke != null) {
            VimPlugin.getProcess().processExKey(exEntryPanel.entry.editor, keyStroke)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = exEntryPanel.isActive
    }

    private fun getKeyStroke(e: AnActionEvent): KeyStroke? {
        val inputEvent = e.inputEvent
        if (inputEvent is KeyEvent) {
            return KeyStroke.getKeyStrokeForEvent(inputEvent)
        }
        return null
    }

    fun registerCustomShortcutSet() {

        val shortcuts = ExKeyBindings.bindings.map {
            KeyboardShortcut(it.key, null)
        }.toTypedArray()

        registerCustomShortcutSet({ shortcuts }, exEntryPanel)
    }
}

