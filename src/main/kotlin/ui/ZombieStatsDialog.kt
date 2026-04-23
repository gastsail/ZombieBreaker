package ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import ui.state.ZombiePluginState
import java.awt.*
import java.util.Locale
import javax.swing.*

class ZombieStatsDialog(project: Project) : DialogWrapper(project) {

    private val stats = ZombiePluginState.getInstance().state
    private val isSpanish = Locale.getDefault().language == "es"

    // --- Textos i18n ---
    private val txtTitle = if (isSpanish) "📊 Analíticas de Supervivencia" else "📊 Survival Analytics"
    private val txtApproved = if (isSpanish) "Aprobados" else "Approved"
    private val txtRejected = if (isSpanish) "Rechazados" else "Rejected"
    private val txtBrainPower = if (isSpanish) "🧠 Brain Power (Precisión Media)" else "🧠 Brain Power (Avg Accuracy)"

    private val txtCurrentStreak = if (isSpanish) "🔥 Racha Actual:" else "🔥 Current Streak:"
    private val txtConsecutive = if (isSpanish) "consecutivos" else "consecutive"
    private val txtMax = if (isSpanish) "Máx:" else "Max:"

    private val txtCodeDigested = if (isSpanish) "📏 Código Digerido:" else "📏 Digested Code:"
    private val txtLinesUnderstood = if (isSpanish) "líneas entendidas" else "lines understood"

    private val txtTimeInvested = if (isSpanish) "⏱️ Tiempo Invertido:" else "⏱️ Time Invested:"
    private val txtAnalyzing = if (isSpanish) "analizando código" else "analyzing code"

    init {
        title = txtTitle
        isResizable = false // BLOQUEA LA EXPANSIÓN DE LA VENTANA
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(20, 25)
            // Forzamos un ancho mínimo amplio para que el texto siempre ocupe una sola línea
            preferredSize = Dimension(JBUI.scale(380), JBUI.scale(340))
        }

        // 1. EL GRÁFICO DE LÍNEA (Barra Horizontal)
        val ratioBarPanel = RatioBarPanel(stats.wins, stats.fails).apply {
            preferredSize = Dimension(JBUI.scale(330), JBUI.scale(15))
            maximumSize = Dimension(Short.MAX_VALUE.toInt(), JBUI.scale(15))
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val legendPanel = JPanel(FlowLayout(FlowLayout.LEFT, 15, 0)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            add(createLegendItem("$txtApproved (${stats.wins})", Color(70, 150, 70)))
            add(createLegendItem("$txtRejected (${stats.fails})", Color(180, 70, 70)))
        }

        mainPanel.add(ratioBarPanel)
        mainPanel.add(Box.createVerticalStrut(8))
        mainPanel.add(legendPanel)

        mainPanel.add(Box.createVerticalStrut(25))
        mainPanel.add(JSeparator().apply { maximumSize = Dimension(Short.MAX_VALUE.toInt(), 1) })
        mainPanel.add(Box.createVerticalStrut(25))

        // 2. CÁLCULO CORREGIDO DEL BRAIN POWERF
        val totalAttempts = stats.wins + stats.fails
        val avgAccuracy = if (totalAttempts > 0) stats.totalAccuracy / totalAttempts else 0

        mainPanel.add(JBLabel("<html><b>$txtBrainPower</b></html>").apply { alignmentX = Component.LEFT_ALIGNMENT })
        mainPanel.add(Box.createVerticalStrut(8))
        mainPanel.add(BatteryPanel(avgAccuracy).apply {
            preferredSize = Dimension(JBUI.scale(150), JBUI.scale(30))
            maximumSize = Dimension(JBUI.scale(150), JBUI.scale(30))
            alignmentX = Component.LEFT_ALIGNMENT
        })

        mainPanel.add(Box.createVerticalStrut(20))

        mainPanel.add(JBLabel("<html><b>$txtCurrentStreak</b> ${stats.currentStreak} $txtConsecutive ($txtMax ${stats.maxStreak})</html>").apply {
            font = font.deriveFont(13f)
            alignmentX = Component.LEFT_ALIGNMENT
        })
        mainPanel.add(Box.createVerticalStrut(12))

        mainPanel.add(JBLabel("<html><b>$txtCodeDigested</b> ${stats.linesDigested} $txtLinesUnderstood</html>").apply {
            font = font.deriveFont(13f)
            alignmentX = Component.LEFT_ALIGNMENT
        })
        mainPanel.add(Box.createVerticalStrut(12))

        val timeString = formatTime(stats.timeInvestedSeconds)
        mainPanel.add(JBLabel("<html><b>$txtTimeInvested</b> $timeString $txtAnalyzing</html>").apply {
            font = font.deriveFont(13f)
            alignmentX = Component.LEFT_ALIGNMENT
        })

        return mainPanel
    }

    private fun createLegendItem(text: String, color: Color): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply { isOpaque = false }
        val colorSquare = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.color = color
                g2d.fillRoundRect(0, 0, width, height, 4, 4)
            }
        }.apply {
            preferredSize = Dimension(JBUI.scale(12), JBUI.scale(12))
            isOpaque = false
        }
        panel.add(colorSquare)
        panel.add(JLabel(text).apply { font = font.deriveFont(12f) })
        return panel
    }

    private fun formatTime(totalSeconds: Long): String {
        if (totalSeconds == 0L) return "0s"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 && hours == 0L) append("${seconds}s")
        }.trim()
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)
}

// --- GRÁFICO: BARRA HORIZONTAL ---
class RatioBarPanel(private val success: Int, private val fails: Int) : JPanel() {
    init { isOpaque = false }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val total = success + fails
        val arc = JBUI.scale(10)

        if (total == 0) {
            g2d.color = JBColor.DARK_GRAY
            g2d.fillRoundRect(0, 0, width, height, arc, arc)
            return
        }

        val successWidth = (width * (success.toDouble() / total)).toInt()

        g2d.color = Color(180, 70, 70)
        g2d.fillRoundRect(0, 0, width, height, arc, arc)

        if (successWidth > 0) {
            val oldClip = g2d.clip
            g2d.clipRect(0, 0, successWidth, height)
            g2d.color = Color(70, 150, 70)
            g2d.fillRoundRect(0, 0, width, height, arc, arc)
            g2d.clip = oldClip
        }
    }
}

// --- COMPONENTE BATERÍA (BRAIN POWER) ---
class BatteryPanel(private val percentage: Int) : JPanel() {
    init { isOpaque = false }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = width - JBUI.scale(6)
        val h = height

        g2d.stroke = BasicStroke(2f)
        g2d.color = JBColor.GRAY
        g2d.drawRoundRect(1, 1, w - 2, h - 2, 8, 8)

        g2d.fillRoundRect(w - 1, h / 4, JBUI.scale(4), h / 2, 2, 2)

        val fillColor = when {
            percentage >= 80 -> Color(70, 180, 70)
            percentage >= 50 -> Color(220, 160, 40)
            else -> Color(200, 60, 60)
        }

        val fillWidth = ((w - 6) * (percentage / 100f)).toInt()
        g2d.color = fillColor
        g2d.fillRoundRect(4, 4, fillWidth, h - 8, 4, 4)

        val text = "$percentage%"
        g2d.font = JBUI.Fonts.label().deriveFont(Font.BOLD, 12f)
        val fm = g2d.fontMetrics
        val textWidth = fm.stringWidth(text)

        g2d.color = if (percentage > 40) JBColor.WHITE else JBColor.foreground()
        g2d.drawString(text, (w - textWidth) / 2, (h + fm.ascent - fm.descent) / 2)
    }
}