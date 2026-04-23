package ui.state

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "ZombieBreakerSettings",
    storages = [Storage("ZombieBreakerStats.xml")]
)
class ZombiePluginState : PersistentStateComponent<ZombiePluginState.State> {

    class State {
        var wins: Int = 0
        var fails: Int = 0
        var totalAccuracy: Int = 0 
        var currentStreak: Int = 0
        var maxStreak: Int = 0
        var linesDigested: Int = 0
        var timeInvestedSeconds: Long = 0L
    }

    private var pluginState = State()

    override fun getState(): State {
        return pluginState
    }

    override fun loadState(state: State) {
        pluginState = state
    }

    fun recordWin(accuracy: Int, linesOfCode: Int, timeSpentSeconds: Long) {
        pluginState.wins++
        pluginState.totalAccuracy += accuracy
        pluginState.linesDigested += linesOfCode
        pluginState.timeInvestedSeconds += timeSpentSeconds
        
        pluginState.currentStreak++
        if (pluginState.currentStreak > pluginState.maxStreak) {
            pluginState.maxStreak = pluginState.currentStreak
        }
    }

    fun recordFail(timeSpentSeconds: Long) {
        pluginState.fails++
        pluginState.currentStreak = 0 // ¡Pierde la racha!
        pluginState.timeInvestedSeconds += timeSpentSeconds
    }

    companion object {
        fun getInstance(): ZombiePluginState {
            return ApplicationManager.getApplication().getService(ZombiePluginState::class.java)
        }
    }
}