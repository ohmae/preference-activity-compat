package build

object ProjectProperties {
    const val groupId: String = "net.mm2d.preference"
    const val name: String = "PreferenceActivityCompat"
    const val description: String = "This is a compatibility library of PreferenceActivity."
    const val developerId: String = "ryo"
    const val developerName: String = "ryosuke"

    private const val versionMajor: Int = 0
    private const val versionMinor: Int = 2
    private const val versionPatch: Int = 7
    const val versionName: String = "$versionMajor.$versionMinor.$versionPatch"
    const val versionCode: Int = versionMajor * 10000 + versionMinor * 100 + versionPatch

    object Url {
        const val site: String = "https://github.com/ohmae/preference-activity-compat"
        const val github: String = "https://github.com/ohmae/preference-activity-compat"
        const val scm: String = "scm:git@github.com:ohmae/preference-activity-compat.git"
    }
}
