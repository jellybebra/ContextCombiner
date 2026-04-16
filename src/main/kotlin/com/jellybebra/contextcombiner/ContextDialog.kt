package com.jellybebra.contextcombiner

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.Locale
import javax.swing.JComponent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.JTree
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultTreeModel

class ContextDialog(
    private val project: Project,
    private val contextFile: VirtualFile
) : DialogWrapper(project) {

    private lateinit var rootNode: CheckedTreeNode
    private lateinit var tree: CheckboxTree
    private lateinit var selectionSummaryLabel: JBLabel
    private val fileMetricsCache = mutableMapOf<String, FileMetrics>()
    private val secondaryTextAttributes = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY)
    // CheckedTreeNode is two-state, so parent and child nodes should stay synchronized.
    private val treeCheckPolicy = CheckboxTreeBase.CheckPolicy(true, true, true, true)

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
                            textRenderer.append(" (${formatMetrics(getFileMetrics(file))})", secondaryTextAttributes)
                        }
                    }
                }
            }
        }, rootNode, treeCheckPolicy)
        tree.isRootVisible = true
        tree.showsRootHandles = true

        // Разворачиваем корневой элемент
        (tree.model as DefaultTreeModel).nodeStructureChanged(rootNode)
        tree.expandRow(0)

        val headerPanel = createHeaderPanel()

        installSummaryUpdates()
        updateSelectionSummary()

        // Добавляем дерево в панель с прокруткой
        dialogPanel.add(headerPanel, BorderLayout.NORTH)
        dialogPanel.add(JBScrollPane(tree), BorderLayout.CENTER)

        return dialogPanel
    }

    private fun createHeaderPanel(): JComponent {
        selectionSummaryLabel = JBLabel()

        val headerPanel = JPanel(BorderLayout(JBUI.scale(8), 0))
        headerPanel.isOpaque = false
        headerPanel.border = JBUI.Borders.emptyBottom(8)

        val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0))
        actionsPanel.isOpaque = false

        val collapseAllButton = JButton("Collapse all")
        collapseAllButton.addActionListener { collapseAllTreeRows() }

        val expandAllButton = JButton("Expand all")
        expandAllButton.addActionListener { expandAllTreeRows() }

        actionsPanel.add(collapseAllButton)
        actionsPanel.add(expandAllButton)

        headerPanel.add(selectionSummaryLabel, BorderLayout.CENTER)
        headerPanel.add(actionsPanel, BorderLayout.EAST)
        return headerPanel
    }

    /**
     * Рекурсивная функция для создания структуры дерева
     */
    private fun createTreeNode(file: VirtualFile, parentCheckedByDefault: Boolean = true): CheckedTreeNode {
        val node = CheckedTreeNode(file)
        val isCheckedByDefault = parentCheckedByDefault && !isHiddenFile(file) && !isGitIgnored(file)

        // Скрытые и gitignored-элементы показываем в дереве, но не выбираем по умолчанию.
        // Если папка снята по умолчанию, всё её поддерево тоже стартует снятым.
        node.isChecked = isCheckedByDefault

        if (file.isDirectory) {
            val children = file.children
            for (child in children) {
                if (!shouldSkipFile(child)) {
                    node.add(createTreeNode(child, isCheckedByDefault))
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

    private fun installSummaryUpdates() {
        val updateLater = {
            SwingUtilities.invokeLater {
                updateSelectionSummary()
            }
        }

        (tree.model as? DefaultTreeModel)?.addTreeModelListener(object : TreeModelListener {
            override fun treeNodesChanged(e: TreeModelEvent?) = updateLater()
            override fun treeNodesInserted(e: TreeModelEvent?) = updateLater()
            override fun treeNodesRemoved(e: TreeModelEvent?) = updateLater()
            override fun treeStructureChanged(e: TreeModelEvent?) = updateLater()
        })

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent?) = updateLater()
        })

        tree.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) = updateLater()
        })
    }

    private fun updateSelectionSummary() {
        val metrics = collectSelectionMetrics(rootNode)
        val fileLabel = if (metrics.fileCount == 1) "file" else "files"
        selectionSummaryLabel.text =
            "Selected: ${metrics.fileCount} $fileLabel (${formatBytes(metrics.totalSizeBytes)} / ~${formatNumber(metrics.estimatedTokens)} tokens)"
    }

    private fun expandAllTreeRows() {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row)
            row++
        }
    }

    private fun collapseAllTreeRows() {
        for (row in tree.rowCount - 1 downTo 0) {
            tree.collapseRow(row)
        }
    }

    private fun collectSelectionMetrics(node: CheckedTreeNode): SelectionMetrics {
        val file = node.userObject as? VirtualFile ?: return SelectionMetrics()
        if (!node.isChecked) return SelectionMetrics()

        if (!file.isDirectory) {
            val metrics = getFileMetrics(file)
            return SelectionMetrics(
                fileCount = 1,
                totalSizeBytes = metrics.sizeBytes,
                estimatedTokens = metrics.estimatedTokens
            )
        }

        var fileCount = 0
        var totalSizeBytes = 0L
        var estimatedTokens = 0L
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode ?: continue
            val childMetrics = collectSelectionMetrics(child)
            fileCount += childMetrics.fileCount
            totalSizeBytes += childMetrics.totalSizeBytes
            estimatedTokens += childMetrics.estimatedTokens
        }
        return SelectionMetrics(fileCount, totalSizeBytes, estimatedTokens)
    }

    private fun getFileMetrics(file: VirtualFile): FileMetrics {
        return fileMetricsCache.getOrPut(file.path) {
            val sizeBytes = file.length.coerceAtLeast(0L)
            FileMetrics(
                sizeBytes = sizeBytes,
                estimatedTokens = estimateTokens(sizeBytes)
            )
        }
    }

    private fun estimateTokens(sizeBytes: Long): Long {
        if (sizeBytes <= 0L) return 0L
        return ((sizeBytes + 3) / 4).coerceAtLeast(1L)
    }

    private fun formatMetrics(metrics: FileMetrics): String {
        return "${formatBytes(metrics.sizeBytes)} / ~${formatNumber(metrics.estimatedTokens)} tokens"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"

        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = -1
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }

        val formattedValue = if (value >= 10) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
        }
        return "$formattedValue ${units[unitIndex]}"
    }

    private fun formatNumber(value: Long): String = String.format(Locale.US, "%,d", value)

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
        if (file == null || !node.isChecked) return 0

        // ИСПРАВЛЕНИЕ 2: Логика обхода
        if (!file.isDirectory) {
            // Если это файл — проверяем галочку ТОЛЬКО здесь
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

    private data class FileMetrics(
        val sizeBytes: Long,
        val estimatedTokens: Long
    )

    private data class SelectionMetrics(
        val fileCount: Int = 0,
        val totalSizeBytes: Long = 0L,
        val estimatedTokens: Long = 0L
    )
}
