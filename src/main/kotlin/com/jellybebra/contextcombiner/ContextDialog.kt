package com.jellybebra.contextcombiner

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel

class ContextDialog(
    private val project: Project,
    private val contextFile: VirtualFile
) : DialogWrapper(project) {

    private lateinit var rootNode: CheckedTreeNode
    private lateinit var tree: CheckboxTree

    init {
        title = "Select Context for LLM"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())
        dialogPanel.border = JBUI.Borders.empty(10)
        dialogPanel.preferredSize = JBUI.size(500, 400) // Размер окна по умолчанию

        // 1. Создаем корневой узел и строим дерево
        rootNode = createTreeNode(contextFile)

        // 2. Создаем компонент CheckboxTree
        // Renderer отвечает за то, как выглядят элементы (иконка + имя)
        tree = CheckboxTree(object : CheckboxTree.CheckboxTreeCellRenderer() {
            override fun customizeRenderer(
                tree: JTree?,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                if (value is CheckedTreeNode) {
                    val file = value.userObject as? VirtualFile
                    if (file != null) {
                        // Показываем имя файла и стандартную иконку
                        textRenderer.append(file.name)
                        if (file.isDirectory) {
                            textRenderer.icon = AllIcons.Nodes.Folder
                        } else {
                            textRenderer.icon = file.fileType.icon ?: AllIcons.FileTypes.Text
                        }
                    }
                }
            }
        }, rootNode)

        // Разворачиваем корневой элемент
        (tree.model as DefaultTreeModel).nodeStructureChanged(rootNode)
        tree.expandRow(0)

        // Добавляем дерево в панель с прокруткой
        dialogPanel.add(JBScrollPane(tree), BorderLayout.CENTER)

        return dialogPanel
    }

    /**
     * Рекурсивная функция для создания структуры дерева
     */
    private fun createTreeNode(file: VirtualFile): CheckedTreeNode {
        val node = CheckedTreeNode(file)

        // Скрытые и gitignored-файлы показываем в дереве, но не выбираем по умолчанию.
        node.isChecked = !isHiddenFile(file) && !isGitIgnored(file)

        if (file.isDirectory) {
            val children = file.children
            for (child in children) {
                if (!shouldSkipFile(child)) {
                    node.add(createTreeNode(child))
                }
            }
        }
        return node
    }

    /**
     * Логика фильтрации:
     * 1. Игнорировать бинарные файлы (картинки и т.д.)
     */
    private fun shouldSkipFile(file: VirtualFile): Boolean {
        // Пропускаем бинарные файлы (чтобы не копировать кракозябры)
        return !file.isDirectory && file.fileType.isBinary
    }

    private fun isHiddenFile(file: VirtualFile): Boolean {
        return file.name.startsWith(".")
    }

    private fun isGitIgnored(file: VirtualFile): Boolean {
        val changeListManager = ChangeListManager.getInstance(project)
        return changeListManager.isIgnoredFile(file)
    }

    /**
     * Действие при нажатии кнопки OK
     */
    override fun doOKAction() {
        val sb = StringBuilder()
        var copiedFilesCount = 0

        // Собираем выбранные файлы
        copiedFilesCount = collectCheckedFiles(rootNode, sb)

        if (sb.isNotEmpty()) {
            // Это работает надежнее для вставки в браузер/блокнот
            val selection = StringSelection(sb.toString())
            java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)

            NotificationGroupManager.getInstance()
                .getNotificationGroup("ContextCombiner.Notifications")
                .createNotification(
                    "Copied $copiedFilesCount file${if (copiedFilesCount == 1) "" else "s"} to clipboard",
                    NotificationType.INFORMATION
                )
                .notify(project)
        }

        super.doOKAction()
    }

    private fun collectCheckedFiles(node: CheckedTreeNode, sb: StringBuilder): Int {
        val file = node.userObject as? VirtualFile
        var copiedFilesCount = 0

        // ИСПРАВЛЕНИЕ 2: Логика обхода
        if (file != null && !file.isDirectory) {
            // Если это файл — проверяем галочку ТОЛЬКО здесь
            if (node.isChecked) {
                try {
                    val content = String(file.contentsToByteArray(), file.charset)
                    // Используем contextFile как базу, чтобы пути были красивыми (относительными)
                    val relativePath = getRelativePath(contextFile, file)

                    sb.append("\n### $relativePath\n")
                    sb.append("```\n")
                    sb.append(content)
                    if (!content.endsWith("\n")) sb.append("\n")
                    sb.append("```\n")
                    copiedFilesCount++
                } catch (_: Exception) {
                    sb.append("\n### ERROR READING: ${file.path}\n")
                }
            }
        } else {
            for (i in 0 until node.childCount) {
                val child = node.getChildAt(i) as? CheckedTreeNode
                if (child != null) {
                    copiedFilesCount += collectCheckedFiles(child, sb)
                }
            }
        }

        return copiedFilesCount
    }

    // Простой хелпер для получения пути относительно папки
    private fun getRelativePath(base: VirtualFile, target: VirtualFile): String {
        if (base == target) return target.name
        val basePath = base.path
        val targetPath = target.path
        if (targetPath.startsWith(basePath)) {
            // +1 чтобы убрать слеш
            return targetPath.substring(basePath.length + 1)
        }
        return target.path
    }
}
