package org.dicthub.lang

enum class Lang constructor(val code: String) {

    EN("en"),
    ZH_CN("zh-CN"),
    ZH_TW("zh-TW"),
    JA("ja"),

    AF("af"),
    SQ("sq"),
    AR("ar"),
    AZ("az"),
    EU("eu"),
    BN("bn"),
    BE("be"),
    BG("bg"),
    CA("ca"),
    HR("hr"),
    CS("cs"),
    DA("da"),
    NL("nl"),
    EO("eo"),
    ET("et"),
    TL("tl"),
    FI("fi"),
    FR("fr"),
    GL("gl"),
    KA("ka"),
    DE("de"),
    EL("el"),
    GU("gu"),
    HT("ht"),
    IW("iw"),
    HI("hi"),
    HU("hu"),
    IS("is"),
    ID("id"),
    GA("ga"),
    IT("it"),
    KN("kn"),
    KO("ko"),
    LA("la"),
    LV("lv"),
    LT("lt"),
    MK("mk"),
    MS("ms"),
    MT("mt"),
    NO("no"),
    FA("fa"),
    PL("pl"),
    PT("pt"),
    RO("ro"),
    RU("ru"),
    SR("sr"),
    SK("sk"),
    SL("sl"),
    ES("es"),
    SW("sw"),
    SV("sv"),
    TA("ta"),
    TE("te"),
    TH("th"),
    TR("tr"),
    UK("uk"),
    UR("ur"),
    VI("vi"),
    CY("cy"),
    YI("yi");

    fun getCode(): String {
        return code
    }
}

private val mapping: Map<String, Lang> = Lang.values().associateBy {it.code}

/**
 * Find [Lang] or throw [Exception] if failed matching
 *
 * @param code iso 639-1 standard language code
 */
fun String.toLang() = mapping[this]!!

/**
 * Find optional [Lang] mapping from string representation
 *
 * @param code iso 639-1 standard language code
 */
fun fromCode(code: String) = mapping[code] ?: mapping[code.toLowerCase().split("-").first()]