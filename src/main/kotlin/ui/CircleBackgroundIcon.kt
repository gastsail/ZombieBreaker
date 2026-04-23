package ui

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon

class CircleBackgroundIcon(
    private val baseIcon: Icon,
    private val bgColor: Color,
    private val diameter: Int
) : Icon {

    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        val g2d = g?.create() as? Graphics2D ?: return

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2d.color = bgColor
        g2d.fillOval(x, y, diameter, diameter)
        val iconX = x + (diameter - baseIcon.iconWidth) / 2
        val iconY = y + (diameter - baseIcon.iconHeight) / 2

        baseIcon.paintIcon(c, g2d, iconX, iconY)

        g2d.dispose()
    }

    override fun getIconWidth(): Int = diameter
    override fun getIconHeight(): Int = diameter
}