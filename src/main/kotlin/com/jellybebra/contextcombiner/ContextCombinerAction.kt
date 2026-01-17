package com.jellybebra.contextcombiner

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ContextCombinerAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val contextFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        // Открываем наш диалог (класс создадим ниже)
        ContextDialog(project, contextFile).show()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        // Кнопка активна, только если есть проект и выбран файл/папка
        e.presentation.isEnabledAndVisible = project != null && file != null
    }

    // ВАЖНО: Это исправляет твою ошибку SEVERE
    // Мы говорим IDE, что проверки (update) нужно делать в фоновом потоке, чтобы не морозить интерфейс
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}