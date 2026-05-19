package com.voicetel.sdk

/** Library and API version constants. */
public object Version {
    /** SDK semantic version. */
    public const val SDK_VERSION: String = "2.2.10"

    /** VoiceTel REST API version this SDK targets. */
    public const val API_VERSION: String = "v2.2.10"

    /** Production VoiceTel API endpoint. */
    public const val DEFAULT_BASE_URL: String = "https://api.voicetel.com"

    /** Default User-Agent sent on every request unless overridden. */
    public const val DEFAULT_USER_AGENT: String =
        "voicetel-kotlin/$SDK_VERSION (+https://github.com/voicetel/kotlin-sdk)"
}
