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

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import java.awt.Color

// [VERSION UPDATE] 212+ (the whole file)

internal fun getCaretVisualAttributes(color: Color?, weight: CaretVisualAttributes.Weight, shape: String, thickness: Float): CaretVisualAttributes {
  if (buildGreater212()) {
    val constructor = CaretVisualAttributes::class.java.constructors.find { it.parameterCount == 4 }!!
    return constructor.newInstance(color, weight, getShape(shape), thickness) as CaretVisualAttributes
  } else {
    val constrcutor = CaretVisualAttributes::class.java.constructors.find { it.parameterCount == 2 }!!
    return constrcutor.newInstance(color, weight) as CaretVisualAttributes
  }
}

@Suppress("UNCHECKED_CAST")
internal fun getShape(shape: String): Any {
  val shapeClass = CaretVisualAttributes::class.java.classLoader.loadClass("com.intellij.openapi.editor.CaretVisualAttributes\$Shape")
  return java.lang.Enum.valueOf(shapeClass as Class<Enum<*>>, shape)
}

internal fun Caret.shape(): Any? {
  val method = CaretVisualAttributes::class.java.getMethod("getShape")
  return method.invoke(this.visualAttributes)
}

internal fun Caret.thickness(): Any? {
  val method = CaretVisualAttributes::class.java.getMethod("getThickness")
  return method.invoke(this.visualAttributes)
}

internal fun buildGreater212(): Boolean {
  return ApplicationInfo.getInstance().build.baselineVersion >= 212
}
