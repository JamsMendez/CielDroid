package com.jamsmendez.cieldroid.scenes

import com.jamsmendez.cieldroid.domain.SceneVariant

object SceneHtml {
    const val ORIGINAL_CSP = "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; script-src qrc:; style-src qrc:; font-src qrc:; connect-src 'none'; base-uri 'none'; form-action 'none'; frame-src 'none'\" />"
    val LOCAL_CSP = ORIGINAL_CSP.replace("qrc:", "'self'")
    /** Separate adaptation stylesheet; same origin, so the served 'self' CSP admits it. */
    const val MINI_LINK = "<link rel=\"stylesheet\" href=\"/assets/adaptations/mini-transparent.css\" />"
    /** Only call for a trusted inventory entrypoint, not arbitrary HTML. */
    fun serve(original: String, variant: SceneVariant = SceneVariant.FULL): String {
        val markers = Regex("content-security-policy", RegexOption.IGNORE_CASE)
        require(markers.findAll(original).count() == 1 && original.contains(ORIGINAL_CSP)) {
            "Expected exactly the sole original CSP tag"
        }
        val served = original.replace(ORIGINAL_CSP, LOCAL_CSP)
        // FULL output stays exactly the CSP rewrite; only MINI gains the link, after the original styles.
        if (variant == SceneVariant.FULL) return served
        require(Regex("</head>").findAll(served).count() == 1) { "Expected exactly one head close tag" }
        return served.replace("</head>", "$MINI_LINK\n</head>")
    }
}
