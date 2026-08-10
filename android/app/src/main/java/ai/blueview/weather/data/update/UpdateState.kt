package ai.blueview.weather.data.update

/**
 * Shared result type for [UpdateChecker].
 *
 * UpdateChecker itself is flavour-specific (github = real GitHub Releases check,
 * play = no-op, because Play forbids self-updating), but AboutViewModel and
 * AboutScreen live in `main` and must see exactly ONE definition of this type.
 * Declaring it in a flavour source set would either duplicate it or leave `main`
 * unable to compile, so it belongs here.
 */
sealed class UpdateState {
    object Idle                                                 : UpdateState()
    object Checking                                             : UpdateState()
    object UpToDate                                             : UpdateState()
    data class Available(val version: String, val url: String)  : UpdateState()
    data class Error(val message: String)                       : UpdateState()
}
