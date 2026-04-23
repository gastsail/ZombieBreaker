package ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import javax.swing.Icon

class ZombieStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId() = "ZombieBreakerStatsWidget"
    override fun getDisplayName() = "Zombie Breaker Analytics"
    override fun isAvailable(project: Project) = true
    override fun createWidget(project: Project) = ZombieWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) {}
    override fun canBeEnabledOn(statusBar: StatusBar) = true
}

class ZombieWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.IconPresentation {
    override fun ID() = "ZombieBreakerStatsWidget"
    
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
    
    override fun getIcon(): Icon = AllIcons.General.InspectionsEye
    
    override fun getTooltipText() = "Ver Analíticas de Zombie Breaker"
    
    override fun getClickConsumer() = Consumer<MouseEvent> {
        ZombieStatsDialog(project).show()
    }

    override fun install(statusBar: StatusBar) {}
    override fun dispose() {}
}