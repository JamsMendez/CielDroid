package com.jamsmendez.cieldroid.scenes

object SceneHtml {
    const val ORIGINAL_CSP = "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; script-src qrc:; style-src qrc:; font-src qrc:; connect-src 'none'; base-uri 'none'; form-action 'none'; frame-src 'none'\" />"
    val LOCAL_CSP = ORIGINAL_CSP.replace("qrc:", "'self'")
    /** Only call for a trusted inventory entrypoint, not arbitrary HTML. */
    fun serve(original: String): String {
        val markers = Regex("content-security-policy", RegexOption.IGNORE_CASE)
        require(markers.findAll(original).count() == 1 && original.contains(ORIGINAL_CSP)) {
            "Expected exactly the sole original CSP tag"
        }
        return original.replace(ORIGINAL_CSP, LOCAL_CSP)
    }
}
