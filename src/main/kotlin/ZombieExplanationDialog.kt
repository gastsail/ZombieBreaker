import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.Locale
import javax.swing.*
import javax.swing.text.DefaultHighlighter
import kotlin.text.iterator

class ZombieExplanationDialog(
    project: Project,
    private val fileContext: String,
    private val pastedCode: String
) : DialogWrapper(project) {

    // --- Detectamos el idioma del sistema ---
    private val isSpanish = Locale.getDefault().language == "es"

    // --- Textos i18n ---
    private val txtTitle = if (isSpanish) "🧟 Zombie Breaker - Code Review" else "🧟 Zombie Breaker - Code Review"
    private val txtSendBtn = if (isSpanish) "Enviar Explicación" else "Send Explanation"
    private val txtCancelBtn = if (isSpanish) "Cancelar Pegado" else "Cancel Paste"
    private val txtSkipBtn = if (isSpanish) "Saltar esta vez" else "Skip this time"
    private val txtCodePanel = if (isSpanish) "Código a pegar" else "Code to paste"
    private val txtInputPanel = if (isSpanish) "Tu respuesta" else "Your answer"
    private val txtChatPanel = if (isSpanish) "Conversación con el Tech Lead" else "Chat with Tech Lead"
    private val txtGreeting = if (isSpanish) "¡Hola! 👋 Detecté que intentas pegar este bloque de código externo. Para mantener la calidad del proyecto, ¿podrías explicarme brevemente qué hace y cómo se integra aquí?" else "Hi! 👋 I noticed you are pasting external code. To keep our project clean, could you briefly explain what it does and how it fits here?"
    private val txtEmptyInput = if (isSpanish) "Debes escribir algo." else "You must write something."

    // --- Array de textos para el loading ---
    private val loadingTextsEs = arrayOf("Pensando...", "Analizando código...", "Leyendo tu explicación...", "Consultando al Tech Lead...")
    private val loadingTextsEn = arrayOf("Thinking...", "Analyzing code...", "Reading your explanation...", "Consulting Tech Lead...")
    private val loadingTexts = if (isSpanish) loadingTextsEs else loadingTextsEn
    private var loadingJob: Job? = null

    // --- Componentes ---
    private val codePreviewArea = JTextArea(pastedCode)
    private val successHighlightPainter = DefaultHighlighter.DefaultHighlightPainter(Color(45, 90, 45))
    private val chatHistoryPanel = JPanel()
    private val chatScrollPane: JBScrollPane
    private val inputTextArea = JTextArea()
    private val progressBar = JProgressBar().apply { isIndeterminate = true; isVisible = false }
    private val progressLabel = JLabel("").apply {
        isVisible = false
        font = font.deriveFont(10f)
        foreground = Color.GRAY
        horizontalAlignment = SwingConstants.CENTER
    }

    private var rejectionCount = 0
    private val dialogScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        title = txtTitle
        setOKButtonText(txtSendBtn)
        setCancelButtonText(txtCancelBtn)

        chatHistoryPanel.layout = BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS)
        chatHistoryPanel.border = JBUI.Borders.empty(10)
        chatScrollPane = JBScrollPane(chatHistoryPanel).apply {
            border = BorderFactory.createTitledBorder(txtChatPanel)
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        init()

        addChatMessage(text = txtGreeting, isUser = false)
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
            border = BorderFactory.createTitledBorder(txtCodePanel)
        }

        inputTextArea.apply {
            lineWrap = true
            wrapStyleWord = true
            margin = JBUI.insets(10)
            rows = 3
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                        e.consume() // Evita el salto de línea
                        if (isOKActionEnabled) doOKAction()
                    }
                }
            })
        }
        val inputScrollPane = JBScrollPane(inputTextArea)

        val loadingPanel = JPanel(BorderLayout()).apply {
            add(progressBar, BorderLayout.CENTER)
            add(progressLabel, BorderLayout.SOUTH)
        }

        val inputPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder(txtInputPanel)
            add(inputScrollPane, BorderLayout.CENTER)
            add(loadingPanel, BorderLayout.NORTH)
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

    override fun createActions(): Array<Action> {
        val skipAction = object : DialogWrapperAction(txtSkipBtn) {
            override fun doAction(e: ActionEvent?) {
                // Si hace skip, cerramos con éxito para que pegue el código
                close(OK_EXIT_CODE)
            }
        }
        return arrayOf(okAction, cancelAction, skipAction)
    }

    override fun doOKAction() {
        val explanation = inputTextArea.text.trim()
        if (explanation.isBlank()) {
            inputTextArea.text = "" // Limpia posibles enters
            return
        }

        addChatMessage(explanation, isUser = true)
        inputTextArea.text = ""
        setLoading(true)

        dialogScope.launch {
            try {
                val languageToUse = if (isSpanish) "Spanish" else "English"
                val result = OllamaService.validateExplanation(fileContext, pastedCode, explanation, languageToUse)

                updateUI {
                    setLoading(false)
                    isOKActionEnabled = false
                    inputTextArea.isEnabled = false
                }

                if (result.approved) {
                    updateUI { highlightUnderstoodCode(pastedCode) }

                    val successMsg = if (isSpanish) "✅ ¡Excelente! Explicación aprobada (Precisión: ${result.accuracy}%)."
                    else "✅ Excellent! Explanation approved (Accuracy: ${result.accuracy}%)."
                    animateBotMessage(successMsg)

                    startCountdown()
                    updateUI { close(OK_EXIT_CODE) }

                } else {
                    rejectionCount++

                    val rejectMsg = if (isSpanish) "❌ Mmm, no me convence (Precisión: ${result.accuracy}%).\n\n${result.reason}"
                    else "❌ Hmm, I'm not convinced (Accuracy: ${result.accuracy}%).\n\n${result.reason}"
                    animateBotMessage(rejectMsg)

                    updateUI {
                        if (!result.understoodCode.isNullOrBlank()) {
                            highlightUnderstoodCode(result.understoodCode)
                        }
                        inputTextArea.isEnabled = true
                        isOKActionEnabled = true
                        inputTextArea.requestFocusInWindow()
                    }

                    if (rejectionCount >= 3) {
                        delay(500)
                        val hintPrefix = if (isSpanish) "💡 Pista: " else "💡 Hint: "
                        val fallbackHint = if (isSpanish) "Intenta explicar la intención general." else "Try explaining the general intent."
                        animateBotMessage("$hintPrefix ${result.hint ?: fallbackHint}", isSystemLog = true)
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
                val errorMsg = if (isSpanish) "⚠️ Error de conexión: " else "⚠️ Connection error: "
                animateBotMessage("$errorMsg ${e.localizedMessage}")
            }
        }
    }

    private suspend fun animateBotMessage(fullText: String, isSystemLog: Boolean = false) {
        var messageArea: JTextArea? = null

        ApplicationManager.getApplication().invokeAndWait({
            messageArea = addChatMessage("", isUser = false, isSystemLog = isSystemLog)
        }, ModalityState.any())

        val currentText = StringBuilder()
        for (char in fullText) {
            currentText.append(char)

            ApplicationManager.getApplication().invokeLater({
                messageArea?.text = currentText.toString()
                messageArea?.parent?.revalidate()
                chatHistoryPanel.revalidate()
                chatHistoryPanel.repaint()

                // Auto-scroll
                val verticalBar = chatScrollPane.verticalScrollBar
                verticalBar.value = verticalBar.maximum
            }, ModalityState.any())

            val pauseTime = when (char) {
                '.', '!', '?', '\n' -> 200L
                ',', ':' -> 100L
                else -> 15L
            }
            delay(pauseTime)
        }
    }

    private suspend fun startCountdown() {
        var countdownArea: JTextArea? = null
        val initMsg = if (Locale.getDefault().language == "es") "⏳ Pegando código en 5..." else "⏳ Pasting code in 5..."

        ApplicationManager.getApplication().invokeAndWait({
            countdownArea = addChatMessage(initMsg, isUser = false, isSystemLog = true)
        }, ModalityState.any())

        for (i in 4 downTo 1) {
            delay(1000)
            val stepMsg = if (Locale.getDefault().language == "es") "⏳ Pegando código en $i..." else "⏳ Pasting code in $i..."
            ApplicationManager.getApplication().invokeLater({
                countdownArea?.text = stepMsg

                countdownArea?.parent?.revalidate()
                chatHistoryPanel.revalidate()
                chatHistoryPanel.repaint()
            }, ModalityState.any())
        }
    }


    private fun addChatMessage(text: String, isUser: Boolean, isSystemLog: Boolean = false): JTextArea {
        val bubblePanel = JPanel(BorderLayout(6, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(5, 5, 10, 5)
        }

        val rawIcon = if (isUser) AllIcons.General.User else AllIcons.CodeWithMe.CwmVerified
        val bgColor = if (isUser) Color(70, 130, 180) else Color(60, 120, 60) // Azul para usuario, Verde para IA

        val avatarIcon = CircleBackgroundIcon(rawIcon, bgColor, diameter = JBUI.scale(26))

        val avatarLabel = JLabel(avatarIcon).apply {
            if (isSystemLog) isVisible = false
        }

        val avatarContainer = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(avatarLabel, BorderLayout.NORTH)
            border = JBUI.Borders.emptyTop(2)
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
            bubblePanel.add(avatarContainer, BorderLayout.EAST)
        } else {
            bubblePanel.add(avatarContainer, BorderLayout.WEST)
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

    private fun setLoading(loading: Boolean) {
        progressBar.isVisible = loading
        progressLabel.isVisible = loading
        isOKActionEnabled = !loading
        inputTextArea.isEnabled = !loading

        if (loading) {
            codePreviewArea.highlighter.removeAllHighlights()

            // Animación de textos del loading
            loadingJob?.cancel()
            loadingJob = dialogScope.launch {
                var index = 0
                while (isActive) {
                    updateUI { progressLabel.text = loadingTexts[index] }
                    index = (index + 1) % loadingTexts.size
                    delay(1500) // Cambia el texto cada 1.5 segundos
                }
            }
        } else {
            loadingJob?.cancel()
        }

        this.contentPane.revalidate()
        this.contentPane.repaint()
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

    private fun updateUI(action: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(action, ModalityState.any())
    }

    override fun dispose() {
        dialogScope.cancel()
        super.dispose()
    }
}