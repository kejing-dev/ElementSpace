package com.example.elementspace.p7c3a91e4.domain.model

/** The ten element categories used to tint tiles, ordered left to right across the table. */
enum class ElementCategory {
    ALKALI_METAL,
    ALKALINE_EARTH,
    TRANSITION_METAL,
    POST_TRANSITION,
    METALLOID,
    NONMETAL,
    HALOGEN,
    NOBLE_GAS,
    LANTHANIDE,
    ACTINIDE,
}

/** UI languages. V1 ships Chinese and English; the enum is the extension point for more. */
enum class AppLanguage(val nativeName: String) {
    ZH("简体中文"),
    EN("English"),
}

/** What the main window colours tiles by. Everything except [CATEGORY] is an advanced trend. */
enum class TrendProperty {
    CATEGORY,
    ATOMIC_RADIUS,
    ELECTRONEGATIVITY,
    IONIZATION_ENERGY,
    DENSITY,
}

/** Views of the atom Volume. [CLOUD] and [ALLOTROPE] sit behind the Advanced entry. */
enum class AtomViewMode(val advanced: Boolean) {
    SHELL(false),
    NUCLEUS(false),
    CLOUD(true),
    ALLOTROPE(true),
}

data class Element(
    val atomicNumber: Int,
    val symbol: String,
    val nameZh: String,
    val nameEn: String,
    val category: ElementCategory,
    /** Row in the 18-column layout: 1..7 main periods, 8 lanthanides, 9 actinides. */
    val tableRow: Int,
    /** Column 1..18 in the 18-column layout. */
    val tableColumn: Int,
    val atomicMass: Double,
    val electronegativity: Double?,
    val atomicRadiusPm: Double?,
    val ionizationEnergyKjMol: Double?,
    val densityGcm3: Double?,
) {
    /** Period 1..7; the lanthanide and actinide rows belong to periods 6 and 7. */
    val period: Int
        get() = if (tableRow <= 7) tableRow else tableRow - 2

    /** Group 1..18, or null for the f-block rows, which have no group number. */
    val group: Int?
        get() = if (tableRow <= 7) tableColumn else null

    fun name(language: AppLanguage): String =
        when (language) {
            AppLanguage.ZH -> nameZh
            AppLanguage.EN -> nameEn
        }

    fun trendValue(property: TrendProperty): Double? =
        when (property) {
            TrendProperty.CATEGORY -> null
            TrendProperty.ATOMIC_RADIUS -> atomicRadiusPm
            TrendProperty.ELECTRONEGATIVITY -> electronegativity
            TrendProperty.IONIZATION_ENERGY -> ionizationEnergyKjMol
            TrendProperty.DENSITY -> densityGcm3
        }
}

data class Isotope(
    val massNumber: Int,
    /** Natural abundance in percent, or null for trace / synthetic isotopes. */
    val abundancePercent: Double?,
    /** Half-life as display text per language, or null when stable. */
    val halfLifeZh: String?,
    val halfLifeEn: String?,
) {
    val stable: Boolean
        get() = halfLifeZh == null

    fun neutrons(atomicNumber: Int): Int = massNumber - atomicNumber
}

/** Crystal or molecular structures the allotrope view can build. */
enum class AllotropeStructure {
    DIAMOND,
    GRAPHITE,
    FULLERENE,
    DIATOMIC,
    OZONE,
    S8_RING,
    P4_TETRAHEDRON,
    BCC,
    FCC,
    HCP,
}

data class Allotrope(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    /** Short badge such as "sp³" or "O₃". */
    val badge: String,
    val noteZh: String,
    val noteEn: String,
    val structure: AllotropeStructure,
)

/** Electron subshell such as 2p with its occupancy. */
data class Subshell(val n: Int, val l: Int, val electrons: Int) {
    val label: String
        get() = "$n${"spdf"[l]}"
}

data class ElectronConfiguration(
    /** Electrons per principal shell, K first. */
    val shells: List<Int>,
    /** Occupied subshells in filling order. */
    val subshells: List<Subshell>,
) {
    val valenceElectrons: Int
        get() = shells.lastOrNull() ?: 0

    /** e.g. "1s² 2s² 2p²". */
    fun notation(): String =
        subshells
            .sortedWith(compareBy({ it.n }, { it.l }))
            .joinToString(" ") { it.label + superscript(it.electrons) }

    private fun superscript(value: Int): String =
        value.toString().map { SUPERSCRIPTS[it - '0'] }.joinToString("")

    private companion object {
        const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    }
}
