package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.Experiment
import com.example.elementspace.p7c3a91e4.domain.model.L10n
import com.example.elementspace.p7c3a91e4.domain.model.LabChoice
import com.example.elementspace.p7c3a91e4.domain.model.LabColor
import com.example.elementspace.p7c3a91e4.domain.model.LabEffect
import com.example.elementspace.p7c3a91e4.domain.model.LabProp
import com.example.elementspace.p7c3a91e4.domain.model.LabStep
import com.example.elementspace.p7c3a91e4.domain.model.Vec3

/**
 * The lab bench (v1.9.0): eight experiments chosen for what you see. Every observation is worded
 * after the PEP textbooks (九年级 units 2, 5, 8, 10; 必修第一册 ch. 2; 必修第二册 ch. 8) and each
 * experiment links to its reaction in the library for the atom-level view. Positions are scene
 * units above the bench top (y = 0), x to the right, z toward the viewer.
 */
object Experiments {
    private fun c(r: Float, g: Float, b: Float, a: Float = 1f) = LabColor(r, g, b, a)

    private fun v(x: Float, y: Float, z: Float = 0f) = Vec3(x, y, z)

    private val J2 = L10n("人教版九年级《化学》第二单元「我们周围的空气」", "PEP Chemistry Grade 9, Unit 2 (The air around us)")
    private val J5 = L10n("人教版九年级《化学》第五单元「化学方程式」", "PEP Chemistry Grade 9, Unit 5 (Chemical equations)")
    private val J8 = L10n("人教版九年级《化学》第八单元「金属和金属材料」", "PEP Chemistry Grade 9, Unit 8 (Metals)")
    private val J10 = L10n("人教版九年级《化学》第十单元「酸和碱」", "PEP Chemistry Grade 9, Unit 10 (Acids and bases)")
    private val S1_21 = L10n("人教版高中《化学》必修第一册 第二章第一节「钠及其化合物」", "PEP High-school Chemistry Book 1, Ch. 2 §1 (Sodium and its compounds)")
    private val S2_81 = L10n("人教版高中《化学》必修第二册 第八章第一节「自然资源的开发利用」", "PEP High-school Chemistry Book 2, Ch. 8 §1 (Using natural resources)")

    private val LAMP_FLAME = c(1f, 0.72f, 0.28f)
    private val WATER = c(0.72f, 0.87f, 1f, 0.32f)
    private val SILVER = c(0.86f, 0.88f, 0.9f)
    private val COPPER = c(0.82f, 0.45f, 0.25f)
    private val BLACK = c(0.08f, 0.08f, 0.09f)
    private val WHITE = c(1f, 1f, 1f)

    private const val WICK = 0.37f
    private const val BEAKER = 0.42f
    private const val BEAKER_R = 0.165f
    private const val LEVEL = 0.24f

    private fun lamp() = LabProp("lamp", 0f, 0f, 0.34f)

    private fun beaker() = LabProp("beaker", 0f, 0f, BEAKER, glass = true)

    private fun liquid(color: LabColor, seconds: Float = 0f) = LabEffect.Liquid(v(0f, 0.012f), BEAKER_R, LEVEL, color, seconds)

    private fun lampFlame(size: Float = 1f) = LabEffect.Flame(v(0f, WICK), LAMP_FLAME, size)

    val ALL: List<Experiment> =
        listOf(
            Experiment(
                id = "flame_test",
                name = L10n("焰色试验", "Flame test"),
                setup = L10n("台上有一盏酒精灯、一根铂丝和几种金属的盐。", "On the bench: an alcohol lamp, a platinum wire and salts of several metals."),
                props = listOf(lamp()),
                start = listOf(LabEffect.Solid(v(0.2f, 0.42f), Vec3(0.22f, 0.006f, 0.006f), SILVER, metal = true)),
                steps =
                    listOf(
                        LabStep(
                            L10n("点燃酒精灯", "Light the lamp"),
                            L10n("选一种盐：用铂丝蘸一点，伸进火焰里，看火焰的颜色。", "Pick a salt: dip the wire in it, hold it in the flame and watch the colour."),
                            listOf(lampFlame(), LabEffect.Solid(v(0.13f, WICK + 0.07f), Vec3(0.22f, 0.006f, 0.006f), SILVER, metal = true)),
                        )
                    ),
                choices =
                    listOf(
                        LabChoice(L10n("钠 Na", "Sodium Na"), L10n("钠：黄色。", "Sodium: yellow."), c(1f, 0.78f, 0.08f)),
                        LabChoice(L10n("钾 K", "Potassium K"), L10n("钾：紫色（要透过蓝色钴玻璃看，滤掉钠的黄光）。", "Potassium: lilac (viewed through blue cobalt glass to filter out sodium's yellow)."), c(0.7f, 0.48f, 1f)),
                        LabChoice(L10n("锂 Li", "Lithium Li"), L10n("锂：紫红色。", "Lithium: crimson."), c(0.95f, 0.1f, 0.32f)),
                        LabChoice(L10n("钙 Ca", "Calcium Ca"), L10n("钙：砖红色。", "Calcium: brick red."), c(1f, 0.36f, 0.14f)),
                        LabChoice(L10n("锶 Sr", "Strontium Sr"), L10n("锶：洋红色（焰火里的红色）。", "Strontium: scarlet (the red of fireworks)."), c(1f, 0.1f, 0.18f)),
                        LabChoice(L10n("钡 Ba", "Barium Ba"), L10n("钡：黄绿色。", "Barium: yellow-green."), c(0.62f, 0.95f, 0.28f)),
                        LabChoice(L10n("铜 Cu", "Copper Cu"), L10n("铜：绿色。", "Copper: green."), c(0.12f, 0.92f, 0.5f)),
                    ),
                adjust = L10n("火力", "Flame"),
                source = S1_21,
            ),
            Experiment(
                id = "mg_burn",
                name = L10n("镁条燃烧", "Burning magnesium"),
                setup = L10n("坩埚钳夹着一段打磨过的镁条，旁边是酒精灯。", "A strip of polished magnesium ribbon, held beside an alcohol lamp."),
                props = listOf(lamp()),
                start = listOf(LabEffect.Solid(v(0.3f, 0.52f), Vec3(0.2f, 0.008f, 0.03f), SILVER, metal = true)),
                steps =
                    listOf(
                        LabStep(
                            L10n("点燃酒精灯", "Light the lamp"),
                            L10n("镁条是银白色的金属。", "Magnesium ribbon is a silvery metal."),
                            listOf(lampFlame(), LabEffect.Solid(v(0.3f, 0.52f), Vec3(0.2f, 0.008f, 0.03f), SILVER, metal = true)),
                        ),
                        LabStep(
                            L10n("把镁条伸进火焰", "Hold the ribbon in the flame"),
                            L10n("镁条剧烈燃烧，发出耀眼的白光，放出大量的热，生成白色固体。", "The ribbon burns fiercely with a dazzling white light and much heat, forming a white solid."),
                            listOf(
                                lampFlame(),
                                LabEffect.Solid(v(0.13f, 0.5f), Vec3(0.2f, 0.008f, 0.03f), WHITE, glowing = true),
                                LabEffect.Glow(v(0.04f, 0.5f), c(1f, 1f, 0.97f), 0.55f, 3f),
                                LabEffect.Sparks(v(0.04f, 0.5f), c(1f, 1f, 0.9f), 26f, 0.5f),
                                LabEffect.Smoke(v(0.04f, 0.6f), c(1f, 1f, 1f, 0.5f), 7f),
                            ),
                        ),
                        LabStep(
                            L10n("看剩下了什么", "See what is left"),
                            L10n("燃烧后剩下白色粉末：氧化镁 MgO。2Mg + O₂ = 2MgO（点燃）。", "A white powder is left: magnesium oxide, MgO. 2Mg + O₂ = 2MgO (ignite)."),
                            listOf(lampFlame(), LabEffect.Solid(v(0.24f, 0.02f, 0.1f), Vec3(0.12f, 0.03f, 0.09f), WHITE, round = true)),
                        ),
                    ),
                reactionId = "mg_1",
                source = J5,
            ),
            Experiment(
                id = "fe_o2",
                name = L10n("铁丝在氧气中燃烧", "Iron wire burning in oxygen"),
                setup = L10n("集气瓶里装满氧气，瓶底留了少量水。铁丝下端系着一根火柴。", "A gas jar full of oxygen with a little water at the bottom. The iron wire has a match tied to its end."),
                props = listOf(LabProp("gasjar", 0f, 0f, 0.5f, glass = true)),
                start =
                    listOf(
                        LabEffect.Liquid(v(0f, 0.012f), 0.118f, 0.03f, WATER),
                        LabEffect.Solid(v(0f, 0.68f), Vec3(0.008f, 0.3f, 0.008f), c(0.3f, 0.3f, 0.32f), metal = true),
                    ),
                steps =
                    listOf(
                        LabStep(
                            L10n("把烧红的铁丝伸进瓶里", "Lower the red-hot wire into the jar"),
                            L10n("铁丝剧烈燃烧，火星四射，放出大量的热，生成黑色固体。", "The wire burns fiercely, throwing sparks in all directions and giving out much heat; a black solid forms."),
                            listOf(
                                LabEffect.Liquid(v(0f, 0.012f), 0.118f, 0.03f, WATER),
                                LabEffect.Solid(v(0f, 0.42f), Vec3(0.008f, 0.3f, 0.008f), c(0.3f, 0.3f, 0.32f), metal = true),
                                LabEffect.Glow(v(0f, 0.26f), c(1f, 0.75f, 0.35f), 0.2f, 1.2f),
                                LabEffect.Sparks(v(0f, 0.26f), c(1f, 0.8f, 0.4f), 40f, 0.42f),
                            ),
                        ),
                        LabStep(
                            L10n("看瓶底", "Look at the bottom"),
                            L10n("瓶底落下黑色固体：四氧化三铁 Fe₃O₄。瓶底预先放水，是为了防止高温的熔化物炸裂瓶底。3Fe + 2O₂ = Fe₃O₄（点燃）。", "A black solid lies at the bottom: Fe₃O₄. The water is there so the hot melt does not crack the jar. 3Fe + 2O₂ = Fe₃O₄ (ignite)."),
                            listOf(
                                LabEffect.Liquid(v(0f, 0.012f), 0.118f, 0.03f, WATER),
                                LabEffect.Solid(v(0f, 0.48f), Vec3(0.008f, 0.18f, 0.008f), c(0.3f, 0.3f, 0.32f), metal = true),
                                LabEffect.Solid(v(0.02f, 0.03f, 0.02f), Vec3(0.05f, 0.03f, 0.04f), BLACK, round = true),
                            ),
                        ),
                    ),
                reactionId = "fe_1",
                source = J2,
            ),
            Experiment(
                id = "na_water",
                name = L10n("钠和水反应", "Sodium and water"),
                setup = L10n("烧杯里是水。钠保存在煤油里，用镊子取出，切下绿豆大的一小块。", "Water in a beaker. Sodium is kept under kerosene; a piece the size of a small pea is cut off."),
                props = listOf(beaker()),
                start = listOf(liquid(WATER)),
                steps =
                    listOf(
                        LabStep(
                            L10n("滴入几滴酚酞", "Add a few drops of phenolphthalein"),
                            L10n("溶液还是无色。酚酞遇到碱才变红。", "Still colourless. Phenolphthalein only turns red with a base."),
                            listOf(liquid(WATER)),
                        ),
                        LabStep(
                            L10n("放入一小块钠", "Drop in a small piece of sodium"),
                            L10n("钠浮在水面上，熔成闪亮的小球，四处游动，发出嘶嘶的响声；溶液变红。", "The sodium floats, melts into a shiny ball, darts about and hisses; the solution turns red."),
                            listOf(
                                liquid(c(1f, 0.28f, 0.55f, 0.5f), seconds = 7f),
                                LabEffect.Skater(v(0f, LEVEL + 0.02f), 0.11f, 0.03f, SILVER),
                                LabEffect.Smoke(v(0f, LEVEL + 0.04f), c(1f, 1f, 1f, 0.35f), 5f),
                                LabEffect.Bubbles(v(0f, LEVEL - 0.03f), LEVEL + 0.01f, 14f, 0.12f),
                            ),
                        ),
                        LabStep(
                            L10n("等反应结束", "Wait for it to finish"),
                            L10n("钠消失了，溶液是红色的：生成了氢氧化钠（碱）和氢气。2Na + 2H₂O = 2NaOH + H₂↑。浮：钠比水轻；熔：反应放热、钠的熔点低；游、响：生成了气体。", "The sodium is gone and the solution is red: sodium hydroxide (a base) and hydrogen formed. 2Na + 2H₂O = 2NaOH + H₂↑. It floats (lighter than water), melts (heat, low melting point), moves and hisses (gas)."),
                            listOf(liquid(c(1f, 0.28f, 0.55f, 0.5f))),
                        ),
                    ),
                reactionId = "na_3",
                source = S1_21,
            ),
            Experiment(
                id = "cu_agno3",
                name = L10n("铜丝和硝酸银：银树", "Copper wire in silver nitrate: a silver tree"),
                setup = L10n("烧杯里是无色的硝酸银溶液。", "A beaker of colourless silver nitrate solution."),
                props = listOf(beaker()),
                start = listOf(liquid(WATER)),
                steps =
                    listOf(
                        LabStep(
                            L10n("放入铜丝", "Put in the copper wire"),
                            L10n("铜丝表面慢慢长出银白色的固体，溶液由无色逐渐变成蓝色。", "A silvery solid slowly grows on the copper, and the colourless solution gradually turns blue."),
                            listOf(
                                liquid(c(0.22f, 0.52f, 1f, 0.5f), seconds = 14f),
                                LabEffect.Solid(v(0f, 0.2f), Vec3(0.008f, 0.38f, 0.008f), COPPER, metal = true),
                                LabEffect.Crystals(v(0f, 0.03f), 0.19f, SILVER, 14f),
                            ),
                        ),
                        LabStep(
                            L10n("为什么会这样", "Why this happens"),
                            L10n("铜比银活泼，把银从它的盐溶液里置换出来；铜变成铜离子进入溶液，所以溶液变蓝。Cu + 2AgNO₃ = Cu(NO₃)₂ + 2Ag。", "Copper is more reactive than silver and displaces it from its salt; the copper goes into solution as ions, which is why it turns blue. Cu + 2AgNO₃ = Cu(NO₃)₂ + 2Ag."),
                            listOf(
                                liquid(c(0.22f, 0.52f, 1f, 0.5f)),
                                LabEffect.Solid(v(0f, 0.2f), Vec3(0.008f, 0.38f, 0.008f), COPPER, metal = true),
                                LabEffect.Crystals(v(0f, 0.03f), 0.19f, SILVER, 0f),
                            ),
                        ),
                    ),
                reactionId = "cu_2",
                source = J8,
            ),
            Experiment(
                id = "cuso4_naoh",
                name = L10n("硫酸铜和氢氧化钠：蓝色沉淀", "Copper sulfate and sodium hydroxide: a blue precipitate"),
                setup = L10n("烧杯里是蓝色的硫酸铜溶液。", "A beaker of blue copper sulfate solution."),
                props = listOf(beaker()),
                start = listOf(liquid(c(0.16f, 0.5f, 1f, 0.5f))),
                steps =
                    listOf(
                        LabStep(
                            L10n("滴加氢氧化钠溶液", "Add sodium hydroxide solution"),
                            L10n("生成蓝色絮状沉淀。", "A blue, fluffy precipitate forms."),
                            listOf(
                                liquid(c(0.45f, 0.68f, 1f, 0.42f), seconds = 10f),
                                LabEffect.Flakes(v(0f, LEVEL - 0.02f), 0.03f, c(0.16f, 0.42f, 1f, 0.9f), 22f, 0.13f),
                            ),
                        ),
                        LabStep(
                            L10n("静置一会儿", "Let it stand"),
                            L10n("沉淀沉到杯底，是氢氧化铜 Cu(OH)₂；上层溶液颜色变浅。CuSO₄ + 2NaOH = Cu(OH)₂↓ + Na₂SO₄。", "The precipitate, copper hydroxide Cu(OH)₂, settles; the liquid above is paler. CuSO₄ + 2NaOH = Cu(OH)₂↓ + Na₂SO₄."),
                            listOf(
                                liquid(c(0.7f, 0.85f, 1f, 0.34f), seconds = 4f),
                                LabEffect.Solid(v(0f, 0.045f), Vec3(0.3f, 0.06f, 0.3f), c(0.16f, 0.42f, 1f), round = true),
                            ),
                        ),
                    ),
                reactionId = "cu_3",
                source = J10,
            ),
            Experiment(
                id = "h2o2",
                name = L10n("过氧化氢分解", "Decomposing hydrogen peroxide"),
                setup = L10n("烧杯里是过氧化氢溶液（双氧水），几乎看不到气泡。", "A beaker of hydrogen peroxide solution; hardly a bubble to be seen."),
                props = listOf(beaker(), LabProp("tuberack", -0.45f, -0.05f, 0.36f)),
                start = listOf(liquid(WATER), LabEffect.Bubbles(v(0f, 0.03f), LEVEL, 0.6f, 0.12f)),
                steps =
                    listOf(
                        LabStep(
                            L10n("加入少量二氧化锰", "Add a little manganese dioxide"),
                            L10n("迅速产生大量气泡。二氧化锰是催化剂：加快了反应，自己的质量和化学性质不变。", "Bubbles pour out at once. Manganese dioxide is a catalyst: it speeds the reaction while its own mass and chemistry stay the same."),
                            listOf(
                                liquid(WATER),
                                LabEffect.Solid(v(0f, 0.022f), Vec3(0.24f, 0.018f, 0.24f), BLACK, round = true),
                                LabEffect.Bubbles(v(0f, 0.03f), LEVEL, 60f, 0.14f),
                            ),
                        ),
                        LabStep(
                            L10n("伸入带火星的木条", "Hold in a glowing splint"),
                            L10n("带火星的木条复燃：放出的气体是氧气。2H₂O₂ = 2H₂O + O₂↑（MnO₂ 催化）。", "The glowing splint relights: the gas is oxygen. 2H₂O₂ = 2H₂O + O₂↑ (MnO₂ catalyst)."),
                            listOf(
                                liquid(WATER),
                                LabEffect.Solid(v(0f, 0.022f), Vec3(0.24f, 0.018f, 0.24f), BLACK, round = true),
                                LabEffect.Bubbles(v(0f, 0.03f), LEVEL, 60f, 0.14f),
                                LabEffect.Solid(v(0.04f, 0.52f), Vec3(0.012f, 0.3f, 0.012f), c(0.62f, 0.42f, 0.24f)),
                                LabEffect.Flame(v(0.04f, 0.36f), c(1f, 0.7f, 0.25f), 0.55f, 0.6f),
                            ),
                        ),
                    ),
                reactionId = "o_1",
                source = J2,
            ),
            Experiment(
                id = "thermite",
                name = L10n("铝热反应", "Thermite reaction"),
                setup = L10n("坩埚里是铝粉和氧化铁粉末的混合物，上面插着一段镁条作引燃剂。", "The crucible holds a mixture of aluminium powder and iron(III) oxide, with a magnesium strip stuck in as the fuse."),
                props = listOf(LabProp("crucible", 0f, 0f, 0.6f)),
                start = listOf(LabEffect.Solid(v(0f, 0.66f), Vec3(0.008f, 0.1f, 0.02f), SILVER, metal = true)),
                steps =
                    listOf(
                        LabStep(
                            L10n("点燃镁条", "Light the magnesium fuse"),
                            L10n("镁条燃烧，放出的热引发了反应。", "The magnesium burns, and its heat sets the reaction off."),
                            listOf(
                                LabEffect.Solid(v(0f, 0.66f), Vec3(0.008f, 0.1f, 0.02f), WHITE, glowing = true),
                                LabEffect.Glow(v(0f, 0.72f), c(1f, 1f, 0.95f), 0.22f, 1.2f),
                                LabEffect.Sparks(v(0f, 0.72f), c(1f, 1f, 0.9f), 8f, 0.3f),
                            ),
                        ),
                        LabStep(
                            L10n("反应开始", "The reaction takes off"),
                            L10n("反应非常剧烈，放出大量的热，发出耀眼的光，火星四射；有熔融的铁落下来。", "The reaction is violent: great heat, dazzling light, flying sparks, and molten iron running out."),
                            listOf(
                                LabEffect.Glow(v(0f, 0.62f), c(1f, 0.85f, 0.55f), 0.7f, 4f),
                                LabEffect.Flame(v(0f, 0.6f), c(1f, 0.7f, 0.3f), 2.2f, 0f),
                                LabEffect.Sparks(v(0f, 0.62f), c(1f, 0.78f, 0.35f), 80f, 0.85f),
                                LabEffect.Smoke(v(0f, 0.75f), c(0.9f, 0.9f, 0.9f, 0.5f), 9f),
                                LabEffect.Solid(v(0f, 0.035f, 0.02f), Vec3(0.07f, 0.045f, 0.06f), c(1f, 0.55f, 0.15f), round = true, glowing = true),
                            ),
                        ),
                        LabStep(
                            L10n("冷却以后", "After it cools"),
                            L10n("落下的是铁，冷却后变黑变硬，能被磁铁吸引。铝把铁从氧化铁里置换出来：2Al + Fe₂O₃ = Al₂O₃ + 2Fe（高温）。这个反应用来焊接钢轨。", "What fell is iron: black and hard when cool, and attracted by a magnet. Aluminium displaces iron from its oxide: 2Al + Fe₂O₃ = Al₂O₃ + 2Fe (high temperature). The reaction is used to weld rails."),
                            listOf(LabEffect.Solid(v(0f, 0.035f, 0.02f), Vec3(0.07f, 0.045f, 0.06f), c(0.16f, 0.16f, 0.18f), round = true, metal = true)),
                        ),
                    ),
                reactionId = "al_4",
                source = S2_81,
            ),
        )

    fun experiment(id: String): Experiment? = ALL.firstOrNull { it.id == id }
}
