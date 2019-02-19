package com.maddyhome.idea.vim.command

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import java.util.*
import javax.swing.KeyStroke

/**
 * This represents a single Vim command to be executed. It may optionally include an argument if appropriate for
 * the command. The command has a count and a type.
 */
class Command(
        var rawCount: Int,
        val actionId: String?,
        var action: AnAction?,
        val type: Type,
        var flags: EnumSet<CommandFlags>
) {

    init {
        if (action is EditorAction) {
            val handler = (action as EditorAction).handler
            if (handler is EditorActionHandlerBase) {
                handler.process(this)
            }
        }
    }

    var count: Int
        get() = if (rawCount == 0) 1 else rawCount
        set(value) {
            rawCount = value
        }

    var keys: List<KeyStroke> = emptyList()
    var argument: Argument? = null

    enum class Type {
        /**
         * Represents undefined commands.
         */
        UNDEFINED,
        /**
         * Represents commands that actually move the cursor and can be arguments to operators.
         */
        MOTION,
        /**
         * Represents commands that insert new text into the editor.
         */
        INSERT,
        /**
         * Represents commands that remove text from the editor.
         */
        DELETE,
        /**
         * Represents commands that change text in the editor.
         */
        CHANGE,
        /**
         * Represents commands that copy text in the editor.
         */
        COPY,
        PASTE,
        RESET,
        /**
         * Represents commands that select the register.
         */
        SELECT_REGISTER,
        OTHER_READONLY,
        OTHER_WRITABLE,
        OTHER_READ_WRITE,
        /**
         * Represent commands that don't require an outer read or write action for synchronization.
         */
        OTHER_SELF_SYNCHRONIZED,
        COMPLETION;

        val isRead: Boolean
            get() = when (this) {
                MOTION, COPY, SELECT_REGISTER, OTHER_READONLY, OTHER_READ_WRITE, COMPLETION -> true
                else -> false
            }

        val isWrite: Boolean
            get() = when (this) {
                INSERT, DELETE, CHANGE, PASTE, RESET, OTHER_WRITABLE, OTHER_READ_WRITE -> true
                else -> false
            }
    }
}