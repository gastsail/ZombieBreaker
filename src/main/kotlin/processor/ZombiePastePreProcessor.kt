package processor

import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import ui.ZombieExplanationDialog

class ZombiePastePreProcessor : CopyPastePreProcessor {

    override fun preprocessOnCopy(file: PsiFile?, startOffsets: IntArray?, endOffsets: IntArray?, text: String?): String? {
        return null
    }

    override fun preprocessOnPaste(
        project: Project,
        file: PsiFile,
        editor: Editor,
        text: String,
        rawText: RawText?
    ): String {

        if (isExternalPaste() && isLikelyCode(text)) {

            val fileContext = file.text
            var pasteAllowed = false

            // Mostrar el diálogo modal
            ApplicationManager.getApplication().invokeAndWait {
                // Pasamos el contexto y el código pegado al diálogo
                val dialog = ZombieExplanationDialog(project, fileContext, text)

                // showAndGet() devuelve 'true' SOLAMENTE si se llamó a super.doOKAction()
                // Y eso solo ocurre si Ollama devolvió APROBADO
                if (dialog.showAndGet()) {
                    pasteAllowed = true
                }
            }

            if (!pasteAllowed) {
                return "" // Cancelamos el pegado si cerró la ventana
            }
        }

        return text // Pegado exitoso
    }

    private fun isExternalPaste(): Boolean {
        val contents = CopyPasteManager.getInstance().contents ?: return true
        val internalFlavors = contents.transferDataFlavors.map { it.mimeType }
        return internalFlavors.none {
            it.contains("jetbrains") || it.contains("intellij")
        }
    }

    private fun isLikelyCode(text: String): Boolean {
        val codeIndicators = listOf("fun ", "class ", "val ", "var ", "import ", "public ", "{", "}")
        return codeIndicators.any { text.contains(it) } && text.length > 20
    }
}