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
 * The lab bench (v1.9.0, twelve experiments since v1.9.1), chosen for what you see. Every observation is worded
 * after the PEP textbooks (九年级 units 2, 5, 8, 10; 必修第一册 ch. 2; 必修第二册 ch. 5, 8) and each
 * experiment links to its reaction in the library for the atom-level view. Positions are scene
 * units above the bench top (y = 0), x to the right, z toward the viewer.
 */
object Experiments {
    private fun c(r: Float, g: Float, b: Float, a: Float = 1f) = LabColor(r, g, b, a)

    private fun v(x: Float, y: Float, z: Float = 0f) = Vec3(x, y, z)

    private val J2 = L10n("人教版九年级《化学》第二单元「我们周围的空气」", "PEP Chemistry Grade 9, Unit 2 (The air around us)")
    private val J5 = L10n("人教版九年级《化学》第五单元「化学方程式」", "PEP Chemistry Grade 9, Unit 5 (Chemical equations)")
    private val J6 = L10n("人教版九年级《化学》第六单元「碳和碳的氧化物」", "PEP Chemistry Grade 9, Unit 6 (Carbon and its oxides)")
    private val J8 = L10n("人教版九年级《化学》第八单元「金属和金属材料」", "PEP Chemistry Grade 9, Unit 8 (Metals)")
    private val J10 = L10n("人教版九年级《化学》第十单元「酸和碱」", "PEP Chemistry Grade 9, Unit 10 (Acids and bases)")
    private val S1_21 = L10n("人教版高中《化学》必修第一册 第二章第一节「钠及其化合物」", "PEP High-school Chemistry Book 1, Ch. 2 §1 (Sodium and its compounds)")
    private val S2_52 = L10n("人教版高中《化学》必修第二册 第五章第二节「氮及其化合物」", "PEP High-school Chemistry Book 2, Ch. 5 §2 (Nitrogen and its compounds)")
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
                                LabEffect.Solid(v(0f, 0.2f), Vec3(0.008f, 0.38f, 0.008f), COPPER, metal = true, drops = true),
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
            Experiment(
                id = "fe_cuso4",
                name = L10n("铁钉和硫酸铜：湿法炼铜", "Iron nail in copper sulfate"),
                setup = L10n("烧杯里是蓝色的硫酸铜溶液，旁边有一根打磨光亮的铁钉。", "A beaker of blue copper sulfate solution, and a polished iron nail."),
                props = listOf(beaker()),
                start = listOf(liquid(c(0.16f, 0.5f, 1f, 0.5f))),
                steps =
                    listOf(
                        LabStep(
                            L10n("放入铁钉", "Put in the iron nail"),
                            L10n("铁钉表面覆盖一层红色的物质，溶液由蓝色逐渐变成浅绿色。", "A red coating covers the nail, and the blue solution gradually turns pale green."),
                            listOf(
                                liquid(c(0.62f, 0.9f, 0.68f, 0.42f), seconds = 14f),
                                LabEffect.Solid(v(0.02f, 0.2f), Vec3(0.014f, 0.36f, 0.014f), c(0.45f, 0.46f, 0.5f), metal = true, drops = true),
                                LabEffect.Crystals(v(0.02f, 0.03f), 0.19f, COPPER, 14f),
                            ),
                        ),
                        LabStep(
                            L10n("为什么会这样", "Why this happens"),
                            L10n("铁比铜活泼，把铜从硫酸铜溶液里置换出来；红色的是铜，浅绿色是硫酸亚铁溶液。Fe + CuSO₄ = FeSO₄ + Cu。我国古代就用这个办法炼铜。", "Iron is more reactive than copper and displaces it: the red coating is copper, the pale green is iron(II) sulfate. Fe + CuSO₄ = FeSO₄ + Cu. Ancient China smelted copper this way."),
                            listOf(
                                liquid(c(0.62f, 0.9f, 0.68f, 0.42f)),
                                LabEffect.Solid(v(0.02f, 0.2f), Vec3(0.014f, 0.36f, 0.014f), c(0.45f, 0.46f, 0.5f), metal = true),
                                LabEffect.Crystals(v(0.02f, 0.03f), 0.19f, COPPER, 0f),
                            ),
                        ),
                    ),
                reactionId = "fe_cuso4",
                source = J8,
            ),
            Experiment(
                id = "zn_h2so4",
                name = L10n("锌和稀硫酸：制氢气", "Zinc and dilute sulfuric acid: making hydrogen"),
                setup = L10n("烧杯里是稀硫酸，旁边有几颗锌粒。", "A beaker of dilute sulfuric acid, and a few zinc granules."),
                props = listOf(beaker()),
                start = listOf(liquid(WATER)),
                steps =
                    listOf(
                        LabStep(
                            L10n("放入锌粒", "Drop in the zinc"),
                            L10n("锌粒表面产生大量气泡，锌粒逐渐变小。", "Bubbles stream off the zinc, and the granules slowly shrink."),
                            listOf(
                                liquid(WATER),
                                LabEffect.Solid(v(-0.05f, 0.035f, 0.02f), Vec3(0.05f, 0.04f, 0.05f), c(0.6f, 0.62f, 0.66f), round = true, metal = true, drops = true),
                                LabEffect.Solid(v(0.06f, 0.035f, -0.03f), Vec3(0.045f, 0.035f, 0.045f), c(0.6f, 0.62f, 0.66f), round = true, metal = true, drops = true),
                                LabEffect.Solid(v(0.01f, 0.035f, 0.07f), Vec3(0.04f, 0.035f, 0.04f), c(0.6f, 0.62f, 0.66f), round = true, metal = true, drops = true),
                                LabEffect.Bubbles(v(0f, 0.06f), LEVEL, 45f, 0.09f),
                            ),
                        ),
                        LabStep(
                            L10n("放出的是什么气体", "What gas is it"),
                            L10n("是氢气：锌把酸里的氢置换了出来。Zn + H₂SO₄ = ZnSO₄ + H₂↑。实验室常用这个反应制氢气。", "Hydrogen: zinc displaces it from the acid. Zn + H₂SO₄ = ZnSO₄ + H₂↑. This is the usual laboratory way to make hydrogen."),
                            listOf(
                                liquid(WATER),
                                LabEffect.Solid(v(-0.05f, 0.03f, 0.02f), Vec3(0.03f, 0.025f, 0.03f), c(0.6f, 0.62f, 0.66f), round = true, metal = true),
                                LabEffect.Solid(v(0.06f, 0.03f, -0.03f), Vec3(0.028f, 0.022f, 0.028f), c(0.6f, 0.62f, 0.66f), round = true, metal = true),
                                LabEffect.Bubbles(v(0f, 0.05f), LEVEL, 22f, 0.09f),
                            ),
                        ),
                    ),
                reactionId = "zn_1",
                source = J8,
            ),
            Experiment(
                id = "caco3_hcl",
                name = L10n("大理石和稀盐酸：制二氧化碳", "Marble and dilute hydrochloric acid: making carbon dioxide"),
                setup = L10n("烧杯里是稀盐酸，旁边有几块大理石（主要成分是碳酸钙）。", "A beaker of dilute hydrochloric acid, and a few lumps of marble (mostly calcium carbonate)."),
                props = listOf(beaker()),
                start = listOf(liquid(WATER)),
                steps =
                    listOf(
                        LabStep(
                            L10n("放入大理石", "Drop in the marble"),
                            L10n("大理石表面产生大量气泡，固体逐渐减少。", "The marble fizzes with bubbles and slowly dissolves away."),
                            listOf(
                                liquid(WATER),
                                LabEffect.Solid(v(-0.04f, 0.045f, 0.03f), Vec3(0.08f, 0.06f, 0.07f), c(0.93f, 0.93f, 0.9f), round = true, drops = true),
                                LabEffect.Solid(v(0.06f, 0.04f, -0.03f), Vec3(0.06f, 0.05f, 0.06f), c(0.93f, 0.93f, 0.9f), round = true, drops = true),
                                LabEffect.Bubbles(v(0f, 0.07f), LEVEL, 50f, 0.1f),
                            ),
                        ),
                        LabStep(
                            L10n("放出的是什么气体", "What gas is it"),
                            L10n("是二氧化碳。CaCO₃ + 2HCl = CaCl₂ + H₂O + CO₂↑。实验室用这个反应制二氧化碳；鸡蛋壳、水垢遇到酸冒泡也是这个道理。", "Carbon dioxide. CaCO₃ + 2HCl = CaCl₂ + H₂O + CO₂↑. This is how the gas is made in the lab; eggshell and limescale fizz in acid for the same reason."),
                            listOf(
                                liquid(WATER),
                                LabEffect.Solid(v(-0.04f, 0.035f, 0.03f), Vec3(0.05f, 0.04f, 0.045f), c(0.93f, 0.93f, 0.9f), round = true),
                                LabEffect.Bubbles(v(0f, 0.06f), LEVEL, 24f, 0.1f),
                            ),
                        ),
                    ),
                reactionId = "ca_1",
                source = J6,
            ),
            Experiment(
                id = "limewater_co2",
                name = L10n("二氧化碳通入澄清石灰水", "Carbon dioxide into limewater"),
                setup = L10n("烧杯里是澄清的石灰水（氢氧化钙溶液），一根玻璃导管伸到液面下。", "A beaker of clear limewater (calcium hydroxide solution), with a glass tube dipping below the surface."),
                props = listOf(beaker()),
                start = listOf(liquid(WATER), LabEffect.Solid(v(0.05f, 0.32f), Vec3(0.012f, 0.5f, 0.012f), c(0.85f, 0.93f, 1f))),
                steps =
                    listOf(
                        LabStep(
                            L10n("通入二氧化碳", "Bubble in carbon dioxide"),
                            L10n("澄清的石灰水变浑浊。", "The clear limewater turns cloudy."),
                            listOf(
                                liquid(c(0.96f, 0.96f, 0.93f, 0.8f), seconds = 9f),
                                LabEffect.Solid(v(0.05f, 0.32f), Vec3(0.012f, 0.5f, 0.012f), c(0.85f, 0.93f, 1f)),
                                LabEffect.Bubbles(v(0.05f, 0.08f), LEVEL, 26f, 0.03f),
                            ),
                        ),
                        LabStep(
                            L10n("为什么变浑浊", "Why it turns cloudy"),
                            L10n("生成了不溶于水的碳酸钙小颗粒。Ca(OH)₂ + CO₂ = CaCO₃↓ + H₂O。这个现象常用来检验二氧化碳。", "Tiny particles of insoluble calcium carbonate form. Ca(OH)₂ + CO₂ = CaCO₃↓ + H₂O. This is the usual test for carbon dioxide."),
                            listOf(
                                liquid(c(0.96f, 0.96f, 0.93f, 0.8f)),
                                LabEffect.Solid(v(0.05f, 0.32f), Vec3(0.012f, 0.5f, 0.012f), c(0.85f, 0.93f, 1f)),
                            ),
                        ),
                    ),
                reactionId = "ca_4",
                source = J6,
            ),
            // v1.11.0
            Experiment(
                id = "s_burn",
                name = L10n("硫在氧气中燃烧", "Sulfur burning in oxygen"),
                setup = L10n("集气瓶里装满氧气，瓶底留了少量水。燃烧匙里放着淡黄色的硫粉。", "A gas jar full of oxygen with a little water at the bottom. A deflagrating spoon holds pale-yellow sulfur powder."),
                props = listOf(LabProp("gasjar", 0f, 0f, 0.5f, glass = true)),
                start =
                    listOf(
                        LabEffect.Liquid(v(0f, 0.012f), 0.118f, 0.03f, WATER),
                        LabEffect.Solid(v(0f, 0.7f), Vec3(0.008f, 0.2f, 0.008f), c(0.3f, 0.3f, 0.32f), metal = true),
                        LabEffect.Solid(v(0f, 0.59f), Vec3(0.04f, 0.02f, 0.04f), c(0.95f, 0.88f, 0.3f), round = true),
                    ),
                steps =
                    listOf(
                        LabStep(
                            L10n("点燃硫，伸进瓶里", "Light the sulfur and lower it in"),
                            L10n("硫在氧气里燃烧得更旺，发出明亮的蓝紫色火焰，放出热量，生成有刺激性气味的气体。", "In oxygen the sulfur burns more strongly, with a bright blue-violet flame, giving out heat and a gas with a pungent smell."),
                            listOf(
                                LabEffect.Liquid(v(0f, 0.012f), 0.118f, 0.03f, WATER),
                                LabEffect.Solid(v(0f, 0.42f), Vec3(0.008f, 0.3f, 0.008f), c(0.3f, 0.3f, 0.32f), metal = true),
                                LabEffect.Solid(v(0f, 0.26f), Vec3(0.04f, 0.02f, 0.04f), c(0.95f, 0.88f, 0.3f), round = true),
                                LabEffect.Flame(v(0f, 0.27f), c(0.5f, 0.42f, 1f), 0.75f, 1.2f),
                            ),
                        ),
                        LabStep(
                            L10n("生成了什么", "What formed"),
                            L10n("生成的气体是二氧化硫 SO₂，有毒，会污染空气。瓶底的水用来吸收它。在空气里硫的火焰只是微弱的淡蓝色。S + O₂ = SO₂（点燃）。", "The gas is sulfur dioxide, SO₂: toxic and an air pollutant. The water at the bottom absorbs it. In air, sulfur burns with only a faint pale-blue flame. S + O₂ = SO₂ (ignite)."),
                            listOf(
                                LabEffect.Liquid(v(0f, 0.012f), 0.118f, 0.03f, WATER),
                                LabEffect.Solid(v(0f, 0.42f), Vec3(0.008f, 0.3f, 0.008f), c(0.3f, 0.3f, 0.32f), metal = true),
                                LabEffect.Flame(v(0f, 0.27f), c(0.5f, 0.42f, 1f), 0.35f, 0.5f),
                            ),
                        ),
                    ),
                reactionId = "s_1",
                source = J2,
            ),
            Experiment(
                id = "o2_splint",
                name = L10n("带火星的木条复燃：检验氧气", "A glowing splint relights: the test for oxygen"),
                setup = L10n("集气瓶里收集了一瓶无色的气体。手里是一根吹灭后还带着火星的木条。", "A gas jar holds a colourless gas. In hand is a wooden splint, blown out but still glowing at the tip."),
                props = listOf(LabProp("gasjar", 0f, 0f, 0.5f, glass = true)),
                start =
                    listOf(
                        LabEffect.Solid(v(0f, 0.7f), Vec3(0.012f, 0.2f, 0.012f), c(0.78f, 0.62f, 0.4f)),
                        LabEffect.Solid(v(0f, 0.595f), Vec3(0.014f, 0.014f, 0.014f), c(1f, 0.35f, 0.1f), round = true, glowing = true),
                    ),
                steps =
                    listOf(
                        LabStep(
                            L10n("把木条伸进瓶里", "Lower the splint into the jar"),
                            L10n("带火星的木条复燃了，烧得很旺。", "The glowing splint bursts back into flame and burns brightly."),
                            listOf(
                                LabEffect.Solid(v(0f, 0.45f), Vec3(0.012f, 0.24f, 0.012f), c(0.78f, 0.62f, 0.4f)),
                                LabEffect.Flame(v(0f, 0.33f), LAMP_FLAME, 0.7f, 1.2f),
                            ),
                        ),
                        LabStep(
                            L10n("这说明什么", "What this shows"),
                            L10n("瓶里的气体是氧气。氧气能支持燃烧，所以带火星的木条在氧气里会复燃。这就是检验氧气的方法。", "The gas is oxygen. Oxygen supports burning, so a glowing splint relights in it. This is the test for oxygen."),
                            listOf(
                                LabEffect.Solid(v(0f, 0.47f), Vec3(0.012f, 0.2f, 0.012f), c(0.78f, 0.62f, 0.4f)),
                                LabEffect.Flame(v(0f, 0.37f), LAMP_FLAME, 0.5f, 0.8f),
                            ),
                        ),
                    ),
                source = J2,
            ),
            Experiment(
                id = "candle_co2",
                name = L10n("二氧化碳熄灭阶梯蜡烛", "Carbon dioxide puts out stepped candles"),
                setup = L10n("烧杯里有一高一矮两支点燃的蜡烛。旁边有一瓶收集好的二氧化碳。", "Two lit candles, one short and one tall, stand in a beaker. Beside it is a jar of collected carbon dioxide."),
                props = listOf(beaker()),
                start =
                    listOf(
                        LabEffect.Solid(v(-0.06f, 0.05f), Vec3(0.03f, 0.08f, 0.03f), c(0.96f, 0.94f, 0.86f)),
                        LabEffect.Solid(v(0.06f, 0.1f), Vec3(0.03f, 0.18f, 0.03f), c(0.96f, 0.94f, 0.86f)),
                        LabEffect.Flame(v(-0.06f, 0.095f), LAMP_FLAME, 0.3f, 0.5f),
                        LabEffect.Flame(v(0.06f, 0.195f), LAMP_FLAME, 0.3f, 0.5f),
                    ),
                steps =
                    listOf(
                        LabStep(
                            L10n("沿杯壁慢慢倒入二氧化碳", "Slowly pour carbon dioxide down the side"),
                            L10n("矮的蜡烛先熄灭了。", "The short candle goes out first."),
                            listOf(
                                LabEffect.Solid(v(-0.06f, 0.05f), Vec3(0.03f, 0.08f, 0.03f), c(0.96f, 0.94f, 0.86f)),
                                LabEffect.Solid(v(0.06f, 0.1f), Vec3(0.03f, 0.18f, 0.03f), c(0.96f, 0.94f, 0.86f)),
                                LabEffect.Smoke(v(-0.06f, 0.1f), c(0.9f, 0.9f, 0.9f, 0.4f), 3f),
                                LabEffect.Flame(v(0.06f, 0.195f), LAMP_FLAME, 0.3f, 0.5f),
                            ),
                        ),
                        LabStep(
                            L10n("继续倒", "Keep pouring"),
                            L10n("高的蜡烛随后也熄灭了。", "Then the tall candle goes out too."),
                            listOf(
                                LabEffect.Solid(v(-0.06f, 0.05f), Vec3(0.03f, 0.08f, 0.03f), c(0.96f, 0.94f, 0.86f)),
                                LabEffect.Solid(v(0.06f, 0.1f), Vec3(0.03f, 0.18f, 0.03f), c(0.96f, 0.94f, 0.86f)),
                                LabEffect.Smoke(v(0.06f, 0.2f), c(0.9f, 0.9f, 0.9f, 0.4f), 3f),
                            ),
                        ),
                        LabStep(
                            L10n("为什么矮的先灭", "Why the short one first"),
                            L10n("二氧化碳的密度比空气大，倒进去先沉在杯底，再慢慢升高；它不能燃烧，也不支持燃烧。所以二氧化碳可以用来灭火。", "Carbon dioxide is denser than air: it settles at the bottom and rises slowly. It neither burns nor supports burning, which is why it is used to put out fires."),
                            listOf(
                                LabEffect.Solid(v(-0.06f, 0.05f), Vec3(0.03f, 0.08f, 0.03f), c(0.96f, 0.94f, 0.86f)),
                                LabEffect.Solid(v(0.06f, 0.1f), Vec3(0.03f, 0.18f, 0.03f), c(0.96f, 0.94f, 0.86f)),
                            ),
                        ),
                    ),
                source = J6,
            ),
            Experiment(
                id = "neutral",
                name = L10n("酸碱中和", "Neutralising an alkali with an acid"),
                setup = L10n("烧杯里是无色的氢氧化钠溶液。旁边有酚酞溶液和稀盐酸。", "A beaker of colourless sodium hydroxide solution. Phenolphthalein and dilute hydrochloric acid stand ready."),
                props = listOf(beaker()),
                start = listOf(liquid(WATER)),
                steps =
                    listOf(
                        LabStep(
                            L10n("滴入几滴酚酞溶液", "Add a few drops of phenolphthalein"),
                            L10n("溶液变成红色：酚酞遇到碱性溶液变红。", "The solution turns red: phenolphthalein is red in an alkaline solution."),
                            listOf(liquid(c(0.93f, 0.22f, 0.5f, 0.55f), seconds = 2.5f)),
                        ),
                        LabStep(
                            L10n("慢慢滴入稀盐酸，边滴边搅", "Add dilute hydrochloric acid slowly, stirring"),
                            L10n("红色逐渐变浅，最后恰好变成无色。", "The red fades and, at the end point, just disappears."),
                            listOf(liquid(WATER, seconds = 7f), LabEffect.Solid(v(0.07f, 0.3f), Vec3(0.01f, 0.46f, 0.01f), c(0.85f, 0.93f, 1f))),
                        ),
                        LabStep(
                            L10n("为什么颜色没了", "Why the colour went"),
                            L10n("盐酸和氢氧化钠反应，生成了氯化钠和水，溶液不再显碱性。NaOH + HCl = NaCl + H₂O。酸和碱作用生成盐和水的反应，叫做中和反应。", "The acid reacted with the sodium hydroxide to give sodium chloride and water, so the solution is no longer alkaline. NaOH + HCl = NaCl + H₂O. An acid and an alkali giving a salt and water is called neutralisation."),
                            listOf(liquid(WATER)),
                        ),
                    ),
                source = J10,
            ),
            Experiment(
                id = "cu_hno3",
                name = L10n("铜和浓硝酸", "Copper and concentrated nitric acid"),
                setup = L10n("烧杯里是浓硝酸，旁边有一小片铜。这个实验要在通风橱里做。", "A beaker of concentrated nitric acid, and a small piece of copper. This is done in a fume cupboard."),
                props = listOf(beaker()),
                start = listOf(liquid(c(0.98f, 0.95f, 0.72f, 0.3f))),
                steps =
                    listOf(
                        LabStep(
                            L10n("放入铜片", "Drop in the copper"),
                            L10n("反应剧烈，铜片逐渐溶解，产生红棕色气体，溶液变成绿色。", "The reaction is vigorous: the copper dissolves, a red-brown gas comes off, and the solution turns green."),
                            listOf(
                                liquid(c(0.25f, 0.68f, 0.42f, 0.55f), seconds = 7f),
                                LabEffect.Solid(v(0f, 0.03f), Vec3(0.09f, 0.008f, 0.06f), COPPER, metal = true, drops = true),
                                LabEffect.Bubbles(v(0f, 0.04f), LEVEL, 45f, 0.06f),
                                LabEffect.Smoke(v(0f, LEVEL + 0.04f), c(0.72f, 0.36f, 0.14f, 0.6f), 9f),
                            ),
                        ),
                        LabStep(
                            L10n("红棕色的气体是什么", "What the red-brown gas is"),
                            L10n("是二氧化氮 NO₂，有毒。Cu + 4HNO₃（浓）= Cu(NO₃)₂ + 2NO₂↑ + 2H₂O。铜排在氢后面，不和稀盐酸反应，却能被硝酸氧化：硝酸有很强的氧化性。", "Nitrogen dioxide, NO₂, which is toxic. Cu + 4HNO₃ (conc.) = Cu(NO₃)₂ + 2NO₂↑ + 2H₂O. Copper sits below hydrogen and ignores dilute hydrochloric acid, yet nitric acid oxidises it: nitric acid is a strong oxidising agent."),
                            listOf(liquid(c(0.25f, 0.68f, 0.42f, 0.55f)), LabEffect.Smoke(v(0f, LEVEL + 0.04f), c(0.72f, 0.36f, 0.14f, 0.5f), 3f)),
                        ),
                    ),
                reactionId = "n_5",
                source = S2_52,
            ),
        )

    fun experiment(id: String): Experiment? = ALL.firstOrNull { it.id == id }

    /** Which element cards offer which experiment (v1.12.0): the elements the experiment is really about. */
    val ELEMENTS: Map<String, List<String>> =
        mapOf(
            "flame_test" to listOf("Na", "K", "Li", "Ca", "Sr", "Ba", "Cu"),
            "mg_burn" to listOf("Mg"),
            "fe_o2" to listOf("Fe", "O"),
            "na_water" to listOf("Na"),
            "cu_agno3" to listOf("Cu", "Ag"),
            "cuso4_naoh" to listOf("Cu"),
            "h2o2" to listOf("O"),
            "thermite" to listOf("Al", "Fe"),
            "fe_cuso4" to listOf("Fe", "Cu"),
            "zn_h2so4" to listOf("Zn", "H"),
            "caco3_hcl" to listOf("Ca", "C"),
            "limewater_co2" to listOf("Ca", "C"),
            "s_burn" to listOf("S", "O"),
            "o2_splint" to listOf("O"),
            "candle_co2" to listOf("C"),
            "neutral" to listOf("Na", "Cl"),
            "cu_hno3" to listOf("Cu", "N"),
        )

    fun forSymbol(symbol: String): List<Experiment> = ALL.filter { symbol in ELEMENTS[it.id].orEmpty() }

    /** The bench experiment that shows this library reaction, if there is one. */
    fun forReaction(reactionId: String): Experiment? = ALL.firstOrNull { it.reactionId == reactionId }
}
