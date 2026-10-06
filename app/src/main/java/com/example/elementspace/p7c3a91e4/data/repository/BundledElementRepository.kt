package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.Allotrope
import com.example.elementspace.p7c3a91e4.domain.model.AllotropeStructure
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.Isotope
import kotlin.math.roundToInt

/**
 * Offline dataset shipped inside the APK.
 *
 * Values are reference figures typed in for the first build and must be re-checked against a
 * licensed source before release (see the PRD data section). Missing figures are left null rather
 * than estimated.
 */
class BundledElementRepository : ElementRepository {

    private val all: List<Element> by lazy { ROWS.mapIndexed { index, row -> parse(index + 1, row) } }

    override fun elements(): List<Element> = all

    override fun element(atomicNumber: Int): Element? = all.getOrNull(atomicNumber - 1)

    override fun isotopes(atomicNumber: Int): List<Isotope> {
        val element = element(atomicNumber) ?: return emptyList()
        ISOTOPES[atomicNumber]?.let { return it }
        // Fallback: the isotope closest to the standard atomic mass.
        val radioactive = atomicNumber > 82 || atomicNumber == 43 || atomicNumber == 61
        return listOf(
            Isotope(
                massNumber = element.atomicMass.roundToInt(),
                abundancePercent = null,
                halfLifeZh = if (radioactive) "放射性" else null,
                halfLifeEn = if (radioactive) "radioactive" else null,
            )
        )
    }

    override fun allotropes(atomicNumber: Int): List<Allotrope> =
        ALLOTROPES[atomicNumber] ?: emptyList()

    private fun parse(z: Int, row: String): Element {
        val f = row.split('|')
        val (tableRow, tableColumn) = position(z)
        return Element(
            atomicNumber = z,
            symbol = f[0],
            nameZh = f[1],
            nameEn = f[2],
            category = category(z),
            tableRow = tableRow,
            tableColumn = tableColumn,
            atomicMass = f[3].toDouble(),
            electronegativity = f[4].toDoubleOrNull(),
            atomicRadiusPm = f[5].toDoubleOrNull(),
            ionizationEnergyKjMol = f[6].toDoubleOrNull(),
            densityGcm3 = f[7].toDoubleOrNull(),
        )
    }

    private companion object {
        fun category(z: Int): ElementCategory =
            when {
                z in intArrayOf(3, 11, 19, 37, 55, 87) -> ElementCategory.ALKALI_METAL
                z in intArrayOf(4, 12, 20, 38, 56, 88) -> ElementCategory.ALKALINE_EARTH
                z in 57..71 -> ElementCategory.LANTHANIDE
                z in 89..103 -> ElementCategory.ACTINIDE
                z in 21..30 || z in 39..48 || z in 72..80 || z in 104..112 ->
                    ElementCategory.TRANSITION_METAL
                z in intArrayOf(13, 31, 49, 50, 81, 82, 83, 84, 113, 114, 115, 116) ->
                    ElementCategory.POST_TRANSITION
                z in intArrayOf(5, 14, 32, 33, 51, 52) -> ElementCategory.METALLOID
                z in intArrayOf(1, 6, 7, 8, 15, 16, 34) -> ElementCategory.NONMETAL
                z in intArrayOf(9, 17, 35, 53, 85, 117) -> ElementCategory.HALOGEN
                else -> ElementCategory.NOBLE_GAS
            }

        /** Row 1..7 for main periods, 8 for lanthanides, 9 for actinides; column 1..18. */
        fun position(z: Int): Pair<Int, Int> =
            when {
                z == 1 -> 1 to 1
                z == 2 -> 1 to 18
                z <= 4 -> 2 to z - 2
                z <= 10 -> 2 to z + 8
                z <= 12 -> 3 to z - 10
                z <= 18 -> 3 to z
                z <= 36 -> 4 to z - 18
                z <= 54 -> 5 to z - 36
                z <= 56 -> 6 to z - 54
                z <= 71 -> 8 to z - 54
                z <= 86 -> 6 to z - 68
                z <= 88 -> 7 to z - 86
                z <= 103 -> 9 to z - 86
                else -> 7 to z - 100
            }

        private fun iso(a: Int, abundance: Double?, zh: String? = null, en: String? = null) =
            Isotope(a, abundance, zh, en)

        val ISOTOPES: Map<Int, List<Isotope>> =
            mapOf(
                1 to listOf(iso(1, 99.985), iso(2, 0.015), iso(3, null, "12.3 年", "12.3 y")),
                2 to listOf(iso(4, 99.9999), iso(3, 0.0001)),
                3 to listOf(iso(7, 92.4), iso(6, 7.6)),
                4 to listOf(iso(9, 100.0)),
                5 to listOf(iso(11, 80.1), iso(10, 19.9)),
                6 to listOf(iso(12, 98.9), iso(13, 1.1), iso(14, null, "5730 年", "5730 y")),
                7 to listOf(iso(14, 99.6), iso(15, 0.4)),
                8 to listOf(iso(16, 99.76), iso(17, 0.04), iso(18, 0.20)),
                9 to listOf(iso(19, 100.0)),
                10 to listOf(iso(20, 90.5), iso(21, 0.3), iso(22, 9.3)),
                11 to listOf(iso(23, 100.0)),
                12 to listOf(iso(24, 79.0), iso(25, 10.0), iso(26, 11.0)),
                13 to listOf(iso(27, 100.0)),
                14 to listOf(iso(28, 92.2), iso(29, 4.7), iso(30, 3.1)),
                15 to listOf(iso(31, 100.0)),
                16 to listOf(iso(32, 95.0), iso(33, 0.75), iso(34, 4.2)),
                17 to listOf(iso(35, 75.8), iso(37, 24.2)),
                18 to listOf(iso(40, 99.6), iso(36, 0.33), iso(38, 0.06)),
                19 to listOf(iso(39, 93.3), iso(41, 6.7), iso(40, 0.012, "12.5 亿年", "1.25 Gy")),
                20 to listOf(iso(40, 96.9), iso(44, 2.1)),
                26 to listOf(iso(56, 91.8), iso(54, 5.8), iso(57, 2.1), iso(58, 0.3)),
                27 to listOf(iso(59, 100.0), iso(60, null, "5.27 年", "5.27 y")),
                29 to listOf(iso(63, 69.2), iso(65, 30.8)),
                38 to listOf(iso(88, 82.6), iso(90, null, "28.8 年", "28.8 y")),
                43 to listOf(iso(99, null, "21.1 万年", "211 ky")),
                47 to listOf(iso(107, 51.8), iso(109, 48.2)),
                53 to listOf(iso(127, 100.0), iso(131, null, "8.02 天", "8.02 d")),
                55 to listOf(iso(133, 100.0), iso(137, null, "30.1 年", "30.1 y")),
                79 to listOf(iso(197, 100.0)),
                82 to listOf(iso(208, 52.4), iso(206, 24.1), iso(207, 22.1), iso(204, 1.4)),
                88 to listOf(iso(226, null, "1600 年", "1600 y")),
                92 to
                    listOf(
                        iso(238, 99.27, "44.7 亿年", "4.47 Gy"),
                        iso(235, 0.72, "7.04 亿年", "704 My"),
                        iso(234, 0.005, "24.6 万年", "246 ky"),
                    ),
                94 to listOf(iso(239, null, "2.41 万年", "24.1 ky"), iso(244, null, "8000 万年", "80 My")),
            )

        private fun crystal(structure: AllotropeStructure): Allotrope =
            when (structure) {
                AllotropeStructure.BCC ->
                    Allotrope("bcc", "体心立方", "Body-centred cubic", "BCC", "每个原子有 8 个近邻", "8 nearest neighbours", structure)
                AllotropeStructure.FCC ->
                    Allotrope("fcc", "面心立方", "Face-centred cubic", "FCC", "每个原子有 12 个近邻 · 密堆积", "12 nearest neighbours · close-packed", structure)
                else ->
                    Allotrope("hcp", "六方密堆积", "Hexagonal close-packed", "HCP", "每个原子有 12 个近邻 · 密堆积", "12 nearest neighbours · close-packed", structure)
            }

        private fun diatomic(formula: String) =
            Allotrope("x2", formula, formula, formula, "双原子分子", "Diatomic molecule", AllotropeStructure.DIATOMIC)

        val ALLOTROPES: Map<Int, List<Allotrope>> = buildMap {
            put(1, listOf(diatomic("H₂")))
            put(
                6,
                listOf(
                    Allotrope("diamond", "金刚石", "Diamond", "sp³", "四面体网状 · 极硬", "Tetrahedral network · very hard", AllotropeStructure.DIAMOND),
                    Allotrope("graphite", "石墨", "Graphite", "sp²", "层状 · 导电", "Layered · conductive", AllotropeStructure.GRAPHITE),
                    Allotrope("c60", "富勒烯", "Fullerene", "C₆₀", "笼状分子", "Cage molecule", AllotropeStructure.FULLERENE),
                ),
            )
            put(7, listOf(diatomic("N₂")))
            put(
                8,
                listOf(
                    Allotrope("o2", "氧气", "Dioxygen", "O₂", "双原子分子 · 支持呼吸", "Diatomic · the oxygen we breathe", AllotropeStructure.DIATOMIC),
                    Allotrope("o3", "臭氧", "Ozone", "O₃", "弯曲形分子 · 吸收紫外线", "Bent molecule · absorbs UV", AllotropeStructure.OZONE),
                ),
            )
            put(9, listOf(diatomic("F₂")))
            put(14, listOf(Allotrope("si", "晶体硅", "Crystalline silicon", "sp³", "金刚石型结构 · 半导体", "Diamond-type lattice · semiconductor", AllotropeStructure.DIAMOND)))
            put(15, listOf(Allotrope("p4", "白磷", "White phosphorus", "P₄", "四面体分子 · 易自燃", "Tetrahedral molecule · pyrophoric", AllotropeStructure.P4_TETRAHEDRON)))
            put(16, listOf(Allotrope("s8", "斜方硫", "Rhombic sulfur", "S₈", "皇冠形八元环", "Crown-shaped ring of eight", AllotropeStructure.S8_RING)))
            put(17, listOf(diatomic("Cl₂")))
            put(
                26,
                listOf(
                    Allotrope("alpha", "α-铁", "α-Iron", "BCC", "室温稳定 · 有磁性", "Stable at room temperature · magnetic", AllotropeStructure.BCC),
                    Allotrope("gamma", "γ-铁", "γ-Iron", "FCC", "912 °C 以上稳定", "Stable above 912 °C", AllotropeStructure.FCC),
                ),
            )
            put(32, listOf(Allotrope("ge", "晶体锗", "Crystalline germanium", "sp³", "金刚石型结构 · 半导体", "Diamond-type lattice · semiconductor", AllotropeStructure.DIAMOND)))
            put(35, listOf(diatomic("Br₂")))
            put(
                50,
                listOf(
                    Allotrope("gray", "灰锡", "Grey tin", "α-Sn", "金刚石型结构 · 13 °C 以下稳定", "Diamond-type lattice · stable below 13 °C", AllotropeStructure.DIAMOND),
                ),
            )
            put(53, listOf(diatomic("I₂")))
            for (z in intArrayOf(3, 11, 19, 23, 24, 37, 41, 42, 55, 56, 73, 74)) put(z, listOf(crystal(AllotropeStructure.BCC)))
            for (z in intArrayOf(13, 20, 28, 29, 45, 46, 47, 77, 78, 79, 82)) put(z, listOf(crystal(AllotropeStructure.FCC)))
            for (z in intArrayOf(4, 12, 21, 22, 27, 30, 39, 40, 48, 72, 75, 76)) put(z, listOf(crystal(AllotropeStructure.HCP)))
        }

        // symbol|中文名|English|mass|electronegativity|radius pm|1st ionisation kJ/mol|density g/cm³
        val ROWS: List<String> =
            listOf(
                "H|氢|Hydrogen|1.008|2.20|53|1312|0.00009",
                "He|氦|Helium|4.0026|-|31|2372|0.00018",
                "Li|锂|Lithium|6.94|0.98|167|520|0.534",
                "Be|铍|Beryllium|9.0122|1.57|112|900|1.85",
                "B|硼|Boron|10.81|2.04|87|801|2.34",
                "C|碳|Carbon|12.011|2.55|67|1086|2.27",
                "N|氮|Nitrogen|14.007|3.04|56|1402|0.00125",
                "O|氧|Oxygen|15.999|3.44|48|1314|0.00143",
                "F|氟|Fluorine|18.998|3.98|42|1681|0.0017",
                "Ne|氖|Neon|20.180|-|38|2081|0.0009",
                "Na|钠|Sodium|22.990|0.93|190|496|0.971",
                "Mg|镁|Magnesium|24.305|1.31|145|738|1.738",
                "Al|铝|Aluminium|26.982|1.61|118|578|2.70",
                "Si|硅|Silicon|28.085|1.90|111|787|2.33",
                "P|磷|Phosphorus|30.974|2.19|98|1012|1.82",
                "S|硫|Sulfur|32.06|2.58|88|1000|2.07",
                "Cl|氯|Chlorine|35.45|3.16|79|1251|0.0032",
                "Ar|氩|Argon|39.948|-|71|1521|0.0018",
                "K|钾|Potassium|39.098|0.82|243|419|0.862",
                "Ca|钙|Calcium|40.078|1.00|194|590|1.54",
                "Sc|钪|Scandium|44.956|1.36|184|633|2.99",
                "Ti|钛|Titanium|47.867|1.54|176|659|4.51",
                "V|钒|Vanadium|50.942|1.63|171|651|6.11",
                "Cr|铬|Chromium|51.996|1.66|166|653|7.19",
                "Mn|锰|Manganese|54.938|1.55|161|717|7.21",
                "Fe|铁|Iron|55.845|1.83|156|762|7.87",
                "Co|钴|Cobalt|58.933|1.88|152|760|8.90",
                "Ni|镍|Nickel|58.693|1.91|149|737|8.91",
                "Cu|铜|Copper|63.546|1.90|145|745|8.96",
                "Zn|锌|Zinc|65.38|1.65|142|906|7.14",
                "Ga|镓|Gallium|69.723|1.81|136|579|5.91",
                "Ge|锗|Germanium|72.630|2.01|125|762|5.32",
                "As|砷|Arsenic|74.922|2.18|114|947|5.73",
                "Se|硒|Selenium|78.971|2.55|103|941|4.81",
                "Br|溴|Bromine|79.904|2.96|94|1140|3.12",
                "Kr|氪|Krypton|83.798|3.00|88|1351|0.0037",
                "Rb|铷|Rubidium|85.468|0.82|265|403|1.53",
                "Sr|锶|Strontium|87.62|0.95|219|549|2.64",
                "Y|钇|Yttrium|88.906|1.22|212|600|4.47",
                "Zr|锆|Zirconium|91.224|1.33|206|640|6.52",
                "Nb|铌|Niobium|92.906|1.6|198|652|8.57",
                "Mo|钼|Molybdenum|95.95|2.16|190|684|10.28",
                "Tc|锝|Technetium|98|1.9|183|702|11.5",
                "Ru|钌|Ruthenium|101.07|2.2|178|710|12.45",
                "Rh|铑|Rhodium|102.91|2.28|173|720|12.41",
                "Pd|钯|Palladium|106.42|2.20|169|804|12.02",
                "Ag|银|Silver|107.87|1.93|165|731|10.49",
                "Cd|镉|Cadmium|112.41|1.69|161|868|8.65",
                "In|铟|Indium|114.82|1.78|156|558|7.31",
                "Sn|锡|Tin|118.71|1.96|145|709|7.29",
                "Sb|锑|Antimony|121.76|2.05|133|834|6.70",
                "Te|碲|Tellurium|127.60|2.1|123|869|6.24",
                "I|碘|Iodine|126.90|2.66|115|1008|4.93",
                "Xe|氙|Xenon|131.29|2.6|108|1170|0.0059",
                "Cs|铯|Caesium|132.91|0.79|298|376|1.93",
                "Ba|钡|Barium|137.33|0.89|253|503|3.51",
                "La|镧|Lanthanum|138.91|1.10|226|538|6.16",
                "Ce|铈|Cerium|140.12|1.12|210|534|6.77",
                "Pr|镨|Praseodymium|140.91|1.13|247|527|6.77",
                "Nd|钕|Neodymium|144.24|1.14|206|533|7.01",
                "Pm|钷|Promethium|145|1.13|205|540|7.26",
                "Sm|钐|Samarium|150.36|1.17|238|545|7.52",
                "Eu|铕|Europium|151.96|1.2|231|547|5.24",
                "Gd|钆|Gadolinium|157.25|1.20|233|593|7.90",
                "Tb|铽|Terbium|158.93|1.1|225|566|8.23",
                "Dy|镝|Dysprosium|162.50|1.22|228|573|8.54",
                "Ho|钬|Holmium|164.93|1.23|226|581|8.80",
                "Er|铒|Erbium|167.26|1.24|226|589|9.07",
                "Tm|铥|Thulium|168.93|1.25|222|597|9.32",
                "Yb|镱|Ytterbium|173.05|1.1|222|603|6.90",
                "Lu|镥|Lutetium|174.97|1.27|217|524|9.84",
                "Hf|铪|Hafnium|178.49|1.3|208|659|13.31",
                "Ta|钽|Tantalum|180.95|1.5|200|761|16.69",
                "W|钨|Tungsten|183.84|2.36|193|770|19.25",
                "Re|铼|Rhenium|186.21|1.9|188|760|21.02",
                "Os|锇|Osmium|190.23|2.2|185|840|22.59",
                "Ir|铱|Iridium|192.22|2.20|180|880|22.56",
                "Pt|铂|Platinum|195.08|2.28|177|870|21.45",
                "Au|金|Gold|196.97|2.54|174|890|19.30",
                "Hg|汞|Mercury|200.59|2.00|171|1007|13.53",
                "Tl|铊|Thallium|204.38|1.62|156|589|11.85",
                "Pb|铅|Lead|207.2|2.33|154|716|11.34",
                "Bi|铋|Bismuth|208.98|2.02|143|703|9.78",
                "Po|钋|Polonium|209|2.0|135|812|9.20",
                "At|砹|Astatine|210|2.2|127|899|-",
                "Rn|氡|Radon|222|-|120|1037|0.0097",
                "Fr|钫|Francium|223|0.7|-|380|-",
                "Ra|镭|Radium|226|0.9|-|509|5.5",
                "Ac|锕|Actinium|227|1.1|-|499|10.07",
                "Th|钍|Thorium|232.04|1.3|-|587|11.72",
                "Pa|镤|Protactinium|231.04|1.5|-|568|15.37",
                "U|铀|Uranium|238.03|1.38|-|598|19.05",
                "Np|镎|Neptunium|237|1.36|-|605|20.45",
                "Pu|钚|Plutonium|244|1.28|-|585|19.82",
                "Am|镅|Americium|243|1.3|-|578|13.67",
                "Cm|锔|Curium|247|1.3|-|581|13.51",
                "Bk|锫|Berkelium|247|1.3|-|601|14.78",
                "Cf|锎|Californium|251|1.3|-|608|15.1",
                "Es|锿|Einsteinium|252|1.3|-|619|-",
                "Fm|镄|Fermium|257|1.3|-|627|-",
                "Md|钔|Mendelevium|258|1.3|-|635|-",
                "No|锘|Nobelium|259|1.3|-|642|-",
                "Lr|铹|Lawrencium|266|1.3|-|470|-",
                "Rf|𬬻|Rutherfordium|267|-|-|-|-",
                "Db|𬭊|Dubnium|268|-|-|-|-",
                "Sg|𬭳|Seaborgium|269|-|-|-|-",
                "Bh|𬭛|Bohrium|270|-|-|-|-",
                "Hs|𬭶|Hassium|277|-|-|-|-",
                "Mt|鿏|Meitnerium|278|-|-|-|-",
                "Ds|𫟼|Darmstadtium|281|-|-|-|-",
                "Rg|𬬭|Roentgenium|282|-|-|-|-",
                "Cn|鿔|Copernicium|285|-|-|-|-",
                "Nh|鿭|Nihonium|286|-|-|-|-",
                "Fl|𫓧|Flerovium|289|-|-|-|-",
                "Mc|镆|Moscovium|290|-|-|-|-",
                "Lv|𫟷|Livermorium|293|-|-|-|-",
                "Ts|鿬|Tennessine|294|-|-|-|-",
                "Og|鿫|Oganesson|294|-|-|-|-",
            )
    }
}
