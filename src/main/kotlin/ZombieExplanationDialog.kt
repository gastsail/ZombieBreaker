package com.gastonsaillen.zombiebreaker

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import java.awt.*
import javax.swing.*
import javax.swing.text.DefaultHighlighter

class ZombieExplanationDialog(
    project: Project,
    private val fileContext: String,
    private val pastedCode: String
) : DialogWrapper(project) {

    private val explanationTextArea = JTextArea()
    private val codePreviewArea = JTextArea(pastedCode)

    private val progressBar = JProgressBar().apply { isIndeterminate = true; isVisible = false }
    private val statusLabel = JBLabel().apply { isVisible = false; font = font.deriveFont(Font.BOLD) }
    private val hintLabel = JBLabel().apply { isVisible = false; foreground = Color.GRAY }

    // Definimos el color del resaltado (Un verde oscuro que contrasta bien con el fondo del código)
    private val successHighlightPainter = DefaultHighlighter.DefaultHighlightPainter(Color(45, 90, 45))

    private var rejectionCount = 0
    private val dialogScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        title = "🧟 Zombie Breaker - Análisis de Código"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, JBUI.scale(10)))

        // 1. Configuración del código (Izquierda)
        codePreviewArea.apply {
            font = Font("Monospaced", Font.PLAIN, 12)
            isEditable = false
            background = Color(43, 43, 43)
            foreground = Color(169, 183, 198)
            margin = JBUI.insets(10)
        }
        val codeScrollPane = JBScrollPane(codePreviewArea).apply {
            border = BorderFactory.createTitledBorder("Código a pegar")
        }

        // 2. Configuración de la explicación (Derecha)
        explanationTextArea.apply {
            lineWrap = true
            wrapStyleWord = true
            margin = JBUI.insets(10)
        }
        val explanationScrollPane = JBScrollPane(explanationTextArea).apply {
            border = BorderFactory.createTitledBorder("Tu explicación")
        }

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, codeScrollPane, explanationScrollPane).apply {
            dividerLocation = 400
            preferredSize = Dimension(850, 400)
        }

        // 3. Panel de Feedback (Inferior)
        val feedbackPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(progressBar)
            add(Box.createVerticalStrut(5))
            add(statusLabel)
            add(Box.createVerticalStrut(5))
            add(hintLabel)
        }

        val headerLabel = JLabel("<html><b>¿Qué hace este código?</b> Explica la función general para evitar ser un 'Copy-Paste Zombie'.</html>")
        headerLabel.border = JBUI.Borders.emptyBottom(10)

        mainPanel.add(headerLabel, BorderLayout.NORTH)
        mainPanel.add(splitPane, BorderLayout.CENTER)
        mainPanel.add(feedbackPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    override fun doOKAction() {
        val explanation = explanationTextArea.text
        if (explanation.isBlank()) {
            showFeedback("Debes escribir algo.", Color.ORANGE)
            return
        }

        setLoading(true)

        dialogScope.launch {
            try {
                val result = OllamaService.validateExplanation(fileContext, pastedCode, explanation)

                if (result.approved) {
                    updateUI {
                        setLoading(false)
                        // Limpiamos hints y bloqueamos para el conteo final
                        hintLabel.isVisible = false
                        explanationTextArea.isEnabled = false
                        isOKActionEnabled = false

                        // En éxito total, pintamos TODO el código de verde
                        highlightUnderstoodCode(pastedCode)
                    }

                    for (i in 5 downTo 1) {
                        updateUI {
                            showFeedback("✅ ¡Aprobado (Precisión: ${result.accuracy}%)! Pegando en $i...", Color.GREEN.darker())
                        }
                        delay(1000)
                    }

                    updateUI { close(OK_EXIT_CODE) }

                } else {
                    // --- CASO RECHAZADO ---
                    updateUI {
                        setLoading(false)
                        rejectionCount++

                        // Mostramos error y resaltamos lo que SÍ entendió
                        handleFailure(result)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                updateUI {
                    setLoading(false)
                    showFeedback("⚠️ Error: ${e.localizedMessage}", Color.RED)
                }
            }
        }
    }

    private fun handleFailure(result: ValidationResult) {
        // 1. Mostrar mensaje de error con el %
        showFeedback("❌ Rechazado (Precisión: ${result.accuracy}%): ${result.reason}", Color.RED)

        // 2. Resaltar en el panel izquierdo las líneas validadas por la IA
        if (!result.understoodCode.isNullOrBlank()) {
            highlightUnderstoodCode(result.understoodCode)
        }

        // 3. Mostrar pista si lleva varios intentos
        if (rejectionCount >= 3) {
            hintLabel.text = "<html><div style='width: 400px;'><b>💡 Pista:</b> ${result.hint ?: "Intenta explicar la intención general."}</div></html>"
            hintLabel.isVisible = true
        }

        // 4. Forzar que Swing redibuje todo el panel
        this.contentPane.revalidate()
        this.contentPane.repaint()
    }

    private fun highlightUnderstoodCode(understoodCode: String) {
        val highlighter = codePreviewArea.highlighter
        // No borramos aquí si queremos acumular, pero para esta lógica mejor limpiar y poner lo nuevo
        highlighter.removeAllHighlights()

        if (understoodCode.isBlank()) return

        // Intentamos encontrar el bloque de código
        val trimmedTarget = understoodCode.trim()
        var startIndex = pastedCode.indexOf(trimmedTarget)

        if (startIndex != -1) {
            try {
                highlighter.addHighlight(startIndex, startIndex + trimmedTarget.length, successHighlightPainter)

                // OPCIONAL: Hacer scroll automático hacia la parte resaltada si el código es muy largo
                codePreviewArea.modelToView2D(startIndex)?.let { rect ->
                    codePreviewArea.scrollRectToVisible(rect.bounds)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.isVisible = loading
        isOKActionEnabled = !loading
        explanationTextArea.isEnabled = !loading
        if (loading) {
            statusLabel.isVisible = false
            hintLabel.isVisible = false
            // Limpiamos el resaltado verde mientras la IA vuelve a pensar
            codePreviewArea.highlighter.removeAllHighlights()
        }
        this.contentPane.revalidate()
        this.contentPane.repaint()
    }

    private fun showFeedback(message: String, color: Color) {
        statusLabel.text = "<html><div style='width: 800px;'>$message</div></html>"
        statusLabel.foreground = color
        statusLabel.isVisible = true
        this.contentPane.revalidate()
        this.contentPane.repaint()
    }

    private fun updateUI(action: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(action, ModalityState.any())
    }

    override fun dispose() {
        dialogScope.cancel()
        super.dispose()
    }
}