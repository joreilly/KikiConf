package dev.johnoreilly.confetti.decompose

import com.arkivanov.decompose.ComponentContext
import dev.johnoreilly.confetti.AppSettings
import dev.johnoreilly.confetti.auth.Authentication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeveloperSettings(
    val token: String?
)

/**
 * Represents the settings which the user can edit within the app.
 */
data class UserEditableSettings(
    val darkThemeConfig: DarkThemeConfig,
    val useExperimentalFeatures: Boolean,
)

enum class ThemeBrand {
    DEFAULT, ANDROID
}

enum class DarkThemeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}

interface SettingsComponent {

    val developerSettings: StateFlow<DeveloperSettings?>
    val userEditableSettings: StateFlow<UserEditableSettings?>

    fun updateDarkThemeConfig(darkThemeConfig: DarkThemeConfig)
    fun updateUseExperimentalFeatures(value: Boolean)
    fun enableDeveloperMode()
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val appSettings: AppSettings,
    private val authentication: Authentication,
) : SettingsComponent, ComponentContext by componentContext {

    private val coroutineScope = coroutineScope()
    private val settings = appSettings.settings

    override val developerSettings: StateFlow<DeveloperSettings?> = appSettings.developerModeFlow().flatMapLatest {
        if (!it) {
            flowOf(null)
        } else {
            authentication.currentUser.map { user ->
                DeveloperSettings(token = user?.token(false))
            }
        }
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    override val userEditableSettings: StateFlow<UserEditableSettings?> =
        combine(
            settings.getStringFlow(darkThemeConfigKey, DarkThemeConfig.FOLLOW_SYSTEM.toString()),
            appSettings.experimentalFeaturesEnabledFlow,
        ) { darkThemeConfig, useExperimentalFeatures ->
            UserEditableSettings(
                useExperimentalFeatures = useExperimentalFeatures,
                darkThemeConfig = DarkThemeConfig.valueOf(darkThemeConfig),
            )
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    override fun updateDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        coroutineScope.launch {
            settings.putString(darkThemeConfigKey, darkThemeConfig.toString())
        }
    }

    override fun updateUseExperimentalFeatures(value: Boolean) {
        coroutineScope.launch {
            appSettings.setExperimentalFeaturesEnabled(value)
        }
    }


    override fun enableDeveloperMode() {
        coroutineScope.launch {
            appSettings.setDeveloperMode(true)
        }
    }

    companion object {
        const val darkThemeConfigKey = "darkThemeConfigKey"
    }
}
