package com.example.i18n

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val regionTag: String,
    val isRtl: Boolean = false
) {
    EN("en", "English", "EN"),
    ES("es", "Español", "ES"),
    FR("fr", "Français", "FR"),
    DE("de", "Deutsch", "DE"),
    IT("it", "Italiano", "IT"),
    PT("pt", "Português", "PT"),
    JA("ja", "日本語", "JA"),
    KO("ko", "한국어", "KO"),
    ZH("zh", "中文", "ZH"),
    HI("hi", "हिन्दी", "HI"),
    AR("ar", "العربية", "AR", isRtl = true);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}
