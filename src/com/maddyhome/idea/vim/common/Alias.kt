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
        const val FunctionArguments = "<f-args>"
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
        for (symbol in arrayOf(Count, Arguments, QuotedArguments, FunctionArguments)) {
            compiledCommand = compiledCommand.replace(symbol, when(symbol) {
                Count -> arrayOf(count.toString())
                Arguments -> arrayOf(cleanedInput)
                QuotedArguments ->  arrayOf("'$cleanedInput'")
                FunctionArguments -> {
                    // Vim only parses input into "multiple" arguments when there is the
                    // possibility of multiple arguments.
                    if (maximumNumberOfArguments > 1 || maximumNumberOfArguments == -1) {
                        this.getFunctionArguments(cleanedInput)
                    } else {
                        arrayOf("'$cleanedInput'")
                    }
                }
                else -> emptyArray()
            }.joinToString(", "))
        }

        // We want to escape <lt> after we've dropped in all of our args, if they are
        // using <lt> its because they are escaping something that we don't want to handle
        // yet.
        compiledCommand = compiledCommand.replace(LessThan, "<")

        return compiledCommand
    }

    private fun getFunctionArguments(input: String): Array<String> {
        val inputs = input.split(" ")
        val arguments = mutableListOf<String>()
        // This will handle the space escape sequence and splitting.
        /*
            command            <f-args>
            XX ab              'ab'
            XX a\b             'a\b'
            XX a\ b            'a b'
            XX a\  b           'a ', 'b'
            XX a\\b            'a\b'
            XX a\\ b           'a\', 'b'
            XX a\\\b           'a\\b'
            XX a\\\ b          'a\ b'
            XX a\\\\b          'a\\b'
            XX a\\\\ b         'a\\', 'b'
         */
        inputs.forEachIndexed { index, s ->
            // We split the input by space, so if there were any items that end in
            // \ then they were escaping the space and we need to join the following
            // item to that one and clean up the escape sequence. If not then just
            // add the current item normally.
            if (index > 0 && inputs[index - 1].last() == '\\') {
                var previous = arguments[arguments.count() - 1]
                previous = previous.slice(0..previous.length - 2)
                arguments[arguments.count() - 1] = "$previous $s"
            } else {
                arguments.add(s)
            }
        }
        return arguments.map { "'$it'" } .toTypedArray()
    }
}