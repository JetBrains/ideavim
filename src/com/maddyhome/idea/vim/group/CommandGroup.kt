package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.common.Alias

/**
 * @author Elliot Courant
 */
class CommandGroup {
    private companion object {
        const val overridePrefix = "!"
        val blacklistedAliases = arrayOf("X", "Next", "Print")
    }
    private var aliases = HashMap<String, Alias>()

    fun isAlias(command: String): Boolean {
        val name = this.getAliasName(command)
        // If the first letter is not uppercase then it cannot be an alias
        // and reject immediately.
        if (!name[0].isUpperCase()) {
            return false
        }

        // If the input is blacklisted, then it is not an alias.
        if (blacklistedAliases.any {
            name == it
        }) {
            return false
        }

        return this.hasAlias(name)
    }

    fun hasAlias(name: String): Boolean {
        return this.aliases.containsKey(name)
    }

    fun getAlias(name: String): Alias {
        return this.aliases[name]!!
    }

    fun getAliasCommand(command: String, count: Int): String {
        return this.getAlias(this.getAliasName(command)).getCommand(command, count)
    }

    fun setAlias(name: String, alias: Alias) {
        this.aliases[name] = alias
    }

    fun removeAlias(name: String) {
        this.aliases.remove(name)
    }

    fun listAliases(): Set<Map.Entry<String, Alias>> {
        return this.aliases.entries
    }

    fun resetAliases() {
        this.aliases.clear()
    }

    private fun getAliasName(command: String): String {
        val items = command.split(" ")
        if (items.count() > 1) {
            return items[0].removeSuffix(overridePrefix)
        }
        return command.removeSuffix(overridePrefix)
    }
}