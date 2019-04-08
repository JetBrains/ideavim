
package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.VimPlugin

/**
 * @author Elliot Courant
 */
class Alias(
        var minimumNumberOfArguments: Int,
        var maximumNumberOfArguments: Int,
        var name: String,
        var command: String
) {
    private companion object {
        const val LessThan = "<lt>"
        const val Count = "<count>"
        const val Arguments = "<args>"
        const val QuotedArguments = "<q-args>"
    }

    fun getNumberOfArguments(): String {
        if (this.minimumNumberOfArguments == 0 && this.maximumNumberOfArguments == 0) {
            return "0" // No arguments
        } else if (this.minimumNumberOfArguments == 0 && this.maximumNumberOfArguments == -1) {
            return "*" // Any number of arguments
        } else if (this.minimumNumberOfArguments == 0 && this.maximumNumberOfArguments == 1) {
            return "?" // Zero or one argument
        } else if (this.minimumNumberOfArguments == 1 && this.maximumNumberOfArguments == -1) {
            return "+" // One or more arguments
        }
        return this.minimumNumberOfArguments.toString() // Specified number of arguments
    }

    fun getCommand(input: String, count: Int): String {
        if (this.maximumNumberOfArguments == 0 && this.maximumNumberOfArguments == 0) {
            return this.command
        }
        var compiledCommand = this.command
        val cleanedInput = input.trim().removePrefix(name).trim()
        if (minimumNumberOfArguments > 0 && cleanedInput.isEmpty()) {
            VimPlugin.showMessage("E471: Argument required")
            VimPlugin.indicateError()
            return ""
        }
        for (symbol in arrayOf(Count, Arguments, QuotedArguments)) {
            compiledCommand = compiledCommand.replace(symbol, when(symbol) {
                Count -> arrayOf(count.toString())
                Arguments -> arrayOf(cleanedInput)
                QuotedArguments ->  arrayOf("'$cleanedInput'")
                else -> emptyArray()
            }.joinToString(", "))
        }

        // We want to escape <lt> after we've dropped in all of our args, if they are
        // using <lt> its because they are escaping something that we don't want to handle
        // yet.
        compiledCommand = compiledCommand.replace(LessThan, "<")

        return compiledCommand
    }
}