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

/**
 * This annotation is created for test functions (methods)
 * It means that original vim behaviour has small differences from behaviour of IdeaVim
 *
 * E.g. after execution some commands original vim has next text;
 *    Hello1
 *    Hello2
 *    Hello3
 *
 * But IdeaVim gives you:
 *    Hello1
 *
 *    Hello2
 *    Hello3
 *
 * Why this annotation exists?
 * After creating some functionality you can understand that IdeaVim has a bit different behaviour, but you
 *   cannot fix it right now because of any reasons (bugs in IDE, the impossibility of this functionality in IDEA,
 *   leak of time for fixing).
 *   In that case, you should NOT remove the corresponding test or leave it without any marks that this test
 *   not fully convenient with vim, but leave the test with IdeaVim's behaviour and put this annotation
 *   with description of how original vim works.
 *
 * Note that using this annotation should be avoided as much as possible and behaviour of IdeaVim should be as close
 *   to vim as possible.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class VimBehaviourDiffers(
        val originalVimAfter: String = "",
        val description: String = ""
)

/**
 * Function for delegated properties.
 * The property will be delegated to UserData and has nullable type.
 */
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

/**
 * Function for delegated properties.
 * The property will be delegated to UserData and has non-nullable type.
 * [default] action will be executed if UserData doesn't have this property now.
 *   The result of [default] will be put to user data and returned.
 *
 */
fun <T> userDataOr(default: UserDataHolder.() -> T): ReadWriteProperty<UserDataHolder, T> {
    return object : ReadWriteProperty<UserDataHolder, T> {
        private var key: Key<T>? = null
        private fun getKey(property: KProperty<*>): Key<T> {
            if (key == null) {
                key = Key.create(property.name + "by userdata()")
            }
            return key as Key<T>
        }

        override fun getValue(thisRef: UserDataHolder, property: KProperty<*>): T {
            return thisRef.getUserData(getKey(property)) ?: run<ReadWriteProperty<UserDataHolder, T>, T> {
                val defaultValue = thisRef.default()
                thisRef.putUserData(getKey(property), defaultValue)
                defaultValue
            }
        }

        override fun setValue(thisRef: UserDataHolder, property: KProperty<*>, value: T) {
            thisRef.putUserData(getKey(property), value)
        }
    }
}
