package com.gastonsaillen.zombiebreaker

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
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

    // --- Componentes Izquierda (Código) ---
    private val codePreviewArea = JTextArea(pastedCode)
    private val successHighlightPainter = DefaultHighlighter.DefaultHighlightPainter(Color(45, 90, 45))

    // --- Componentes Derecha (Chat) ---
    private val chatHistoryPanel = JPanel()
    private val chatScrollPane: JBScrollPane
    private val inputTextArea = JTextArea()
    private val progressBar = JProgressBar().apply { isIndeterminate = true; isVisible = false }

    private var rejectionCount = 0
    private val dialogScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        title = "🧟 Zombie Breaker - Code Review"
        setOKButtonText("Enviar Explicación")
        setCancelButtonText("Cancelar Pegado")

        chatHistoryPanel.layout = BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS)
        chatHistoryPanel.border = JBUI.Borders.empty(10)
        chatScrollPane = JBScrollPane(chatHistoryPanel).apply {
            border = BorderFactory.createTitledBorder("Conversación con el Tech Lead")
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        init()

        // El mensaje inicial no necesita animación para no hacer esperar al usuario
        addChatMessage(
            text = "¡Hola! 👋 Detecté que intentas pegar este bloque de código externo. Para mantener la calidad del proyecto, ¿podrías explicarme brevemente qué hace y cómo se integra aquí?",
            isUser = false
        )
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, JBUI.scale(10)))

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

        inputTextArea.apply {
            lineWrap = true
            wrapStyleWord = true
            margin = JBUI.insets(10)
            rows = 3
        }
        val inputScrollPane = JBScrollPane(inputTextArea)

        val inputPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Tu respuesta")
            add(inputScrollPane, BorderLayout.CENTER)
            add(progressBar, BorderLayout.NORTH)
        }

        val chatContainer = JPanel(BorderLayout(0, 5)).apply {
            add(chatScrollPane, BorderLayout.CENTER)
            add(inputPanel, BorderLayout.SOUTH)
        }

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, codeScrollPane, chatContainer).apply {
            dividerLocation = 400
            preferredSize = Dimension(900, 500)
        }

        mainPanel.add(splitPane, BorderLayout.CENTER)
        return mainPanel
    }

    override fun doOKAction() {
        val explanation = inputTextArea.text
        if (explanation.isBlank()) return

        // Añadimos el mensaje del usuario al instante
        addChatMessage(explanation, isUser = true)
        inputTextArea.text = ""
        setLoading(true)

        dialogScope.launch {
            try {
                // 1. Esperamos a que Ollama responda (Barra de carga activa)
                val result = OllamaService.validateExplanation(fileContext, pastedCode, explanation)

                // 2. Ollama respondió, quitamos el loading e iniciamos la animación de escritura
                updateUI {
                    setLoading(false)
                    isOKActionEnabled = false
                    inputTextArea.isEnabled = false
                }

                if (result.approved) {
                    updateUI { highlightUnderstoodCode(pastedCode) }

                    // EFECTO DE ESCRITURA
                    animateBotMessage("✅ ¡Excelente! Explicación aprobada (Precisión: ${result.accuracy}%).")

                    // Conteo regresivo actualizado en la misma burbuja
                    startCountdown()

                    updateUI { close(OK_EXIT_CODE) }

                } else {
                    rejectionCount++

                    // EFECTO DE ESCRITURA PARA EL RECHAZO
                    animateBotMessage("❌ Mmm, no me convence (Precisión: ${result.accuracy}%).\n\n${result.reason}")

                    updateUI {
                        if (!result.understoodCode.isNullOrBlank()) {
                            highlightUnderstoodCode(result.understoodCode)
                        }

                        // Rehabilitamos el input para que vuelva a intentar
                        inputTextArea.isEnabled = true
                        isOKActionEnabled = true
                        inputTextArea.requestFocusInWindow()
                    }

                    if (rejectionCount >= 3) {
                        delay(500) // Pequeña pausa antes de dar la pista
                        animateBotMessage("💡 Pista: ${result.hint ?: "Intenta explicar la intención general."}", isSystemLog = true)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                updateUI {
                    setLoading(false)
                    inputTextArea.isEnabled = true
                    isOKActionEnabled = true
                }
                animateBotMessage("⚠️ Error de conexión: ${e.localizedMessage}")
            }
        }
    }

    // --- Animacion texto chat ---

    private suspend fun animateBotMessage(fullText: String, isSystemLog: Boolean = false) {
        // 1. Creamos la burbuja vacía en el hilo de la UI y obtenemos su referencia
        var messageArea: JTextArea? = null
        val deferred = CompletableDeferred<Unit>()

        ApplicationManager.getApplication().invokeLater({
            messageArea = addChatMessage("", isUser = false, isSystemLog = isSystemLog)
            deferred.complete(Unit)
        }, ModalityState.any())

        deferred.await() // Esperamos a que la UI esté lista

        // 2. Escribimos letra por letra
        val currentText = StringBuilder()
        for (char in fullText) {
            currentText.append(char)

            ApplicationManager.getApplication().invokeLater({
                messageArea?.text = currentText.toString()

                // Auto-scroll
                val verticalBar = chatScrollPane.verticalScrollBar
                verticalBar.value = verticalBar.maximum
            }, ModalityState.any())

            // Lógica de pausas para imitar tipeo humano o streaming LLM
            val pauseTime = when (char) {
                '.', '!', '?', '\n' -> 250L // Pausa larga al terminar oración
                ',', ':' -> 100L            // Pausa media
                else -> 15L                 // Escritura rápida
            }
            delay(pauseTime)
        }
    }

    private suspend fun startCountdown() {
        var countdownArea: JTextArea? = null
        val deferred = CompletableDeferred<Unit>()

        ApplicationManager.getApplication().invokeLater({
            countdownArea = addChatMessage("⏳ Pegando código en 5...", isUser = false, isSystemLog = true)
            deferred.complete(Unit)
        }, ModalityState.any())

        deferred.await()

        for (i in 4 downTo 1) {
            delay(1000)
            ApplicationManager.getApplication().invokeLater({
                countdownArea?.text = "⏳ Pegando código en $i..."
            }, ModalityState.any())
        }
    }

    // Modificamos addChatMessage para que devuelva el JTextArea creado
    private fun addChatMessage(text: String, isUser: Boolean, isSystemLog: Boolean = false): JTextArea {
        val bubblePanel = JPanel(BorderLayout(10, 10)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(5, 5, 10, 5)
        }

        // Placeholders para tus avatares
        val avatarLabel = JLabel(if (isUser) " [ IMG_USER ] " else " [ IMG_AI ] ").apply {
            font = font.deriveFont(Font.BOLD)
            foreground = if (isUser) Color(100, 150, 200) else Color(100, 200, 100)
            if (isSystemLog) isVisible = false
        }

        val messageArea = JTextArea(text).apply {
            lineWrap = true
            wrapStyleWord = true
            isEditable = false
            isOpaque = false
            font = JBUI.Fonts.label()
            if (isSystemLog) setForeground(Color.GRAY)
        }

        if (isUser) {
            bubblePanel.add(messageArea, BorderLayout.CENTER)
            bubblePanel.add(avatarLabel, BorderLayout.EAST)
        } else {
            bubblePanel.add(avatarLabel, BorderLayout.WEST)
            bubblePanel.add(messageArea, BorderLayout.CENTER)
        }

        chatHistoryPanel.add(bubblePanel)
        chatHistoryPanel.revalidate()
        chatHistoryPanel.repaint()

        SwingUtilities.invokeLater {
            val verticalBar = chatScrollPane.verticalScrollBar
            verticalBar.value = verticalBar.maximum
        }

        return messageArea
    }

    private fun highlightUnderstoodCode(understoodCode: String) {
        val highlighter = codePreviewArea.highlighter
        highlighter.removeAllHighlights()
        if (understoodCode.isBlank()) return

        val trimmedTarget = understoodCode.trim()
        val startIndex = pastedCode.indexOf(trimmedTarget)

        if (startIndex != -1) {
            try {
                highlighter.addHighlight(startIndex, startIndex + trimmedTarget.length, successHighlightPainter)
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
        inputTextArea.isEnabled = !loading
        if (loading) {
            codePreviewArea.highlighter.removeAllHighlights()
        }
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