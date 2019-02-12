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