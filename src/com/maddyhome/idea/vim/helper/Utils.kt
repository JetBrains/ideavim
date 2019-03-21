/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// TODO: 2019-03-21 Add kotlindoc. Create properly annotations
@Target(AnnotationTarget.FUNCTION)
annotation class VimBehaviourDiffers(
        val originalVimAfter: String = "",
        val trimIndent: Boolean = false,
        val description: String = ""
)

fun <T> userData(): ReadWriteProperty<UserDataHolder, T?> {
    return object : ReadWriteProperty<UserDataHolder, T?> {
        private var key: Key<T>? = null
        private fun getKey(property: KProperty<*>): Key<T> {
            if (key == null) {
                key = Key.create(property.name + "by userData()")
            }
            return key as Key<T>
        }

        override fun getValue(thisRef: UserDataHolder, property: KProperty<*>): T? {
            return thisRef.getUserData(getKey(property))
        }

        override fun setValue(thisRef: UserDataHolder, property: KProperty<*>, value: T?) {
            thisRef.putUserData(getKey(property), value)
        }
    }
}

fun <T> userDataOr(default: UserDataHolder.() -> T): ReadWriteProperty<UserDataHolder, T> {
    return object : ReadWriteProperty<UserDataHolder, T> {
        private var key: Key<T>? = null
        private fun getKey(property: KProperty<*>): Key<T> {
            if (key == null) {
                key = Key.create(property.name + "by userData()")
            }
            return key as Key<T>
        }

        override fun getValue(thisRef: UserDataHolder, property: KProperty<*>): T {
            return thisRef.getUserData(getKey(property)) ?: thisRef.default()
        }

        override fun setValue(thisRef: UserDataHolder, property: KProperty<*>, value: T) {
            thisRef.putUserData(getKey(property), value)
        }
    }
}
