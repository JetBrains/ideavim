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

package org.jetbrains.plugins.ideavim

import com.intellij.openapi.components.impl.ComponentManagerImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.util.Disposer
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockManager

/**
 * @author Alex Plate
 */

abstract class VimFileEditorTestCase: VimTestCase() {

    protected lateinit var fileManager: FileEditorManagerEx
    private lateinit var oldManager: FileEditorManager
    private lateinit var oldDockContainers: Set<DockContainer>

    override fun setUp() {
        super.setUp()
        val dockManager = DockManager.getInstance(myFixture.project)
        oldDockContainers = dockManager.containers
        fileManager = FileEditorManagerImpl(myFixture.project, dockManager)
        oldManager = (myFixture.project as ComponentManagerImpl).registerComponentInstance(FileEditorManager::class.java, fileManager)
        (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
    }

    override fun tearDown() {
        try {
            for (container in DockManager.getInstance(myFixture.project).containers) {
                if (container !in oldDockContainers) {
                    Disposer.dispose(container)
                }
            }
            oldDockContainers = setOf()
            (myFixture.project as ComponentManagerImpl).registerComponentInstance(FileEditorManager::class.java, oldManager)
            fileManager.closeAllFiles()
            EditorHistoryManager.getInstance(myFixture.project).removeAllFiles()
            (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
        } finally {
            super.tearDown()
        }
    }
}