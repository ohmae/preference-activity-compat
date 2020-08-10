package build

object ProjectProperties {
    const val groupId: String = "net.mm2d"

    private const val versionMajor: Int = 0
    private const val versionMinor: Int = 2
    private const val versionPatch: Int = 4
    const val versionName: String = "$versionMajor.$versionMinor.$versionPatch"
    const val versionCode: Int = versionMajor * 10000 + versionMinor * 100 + versionPatch

    object Url {
        const val site: String = "https://github.com/ohmae/preference-activity-compat"
        const val github: String = "https://github.com/ohmae/preference-activity-compat"
        const val scm: String = "scm:git:https://github.com/ohmae/preference-activity-compat.git"
    }
}
