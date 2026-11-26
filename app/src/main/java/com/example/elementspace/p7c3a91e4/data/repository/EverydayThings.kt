package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.EverydayThing
import com.example.elementspace.p7c3a91e4.domain.model.L10n
import com.example.elementspace.p7c3a91e4.domain.model.ThingInside

/**
 * Everyday things, each tied to the elements it shows. Facts: PEP 九年级 (units 6, 8, 10, 11) and
 * 必修第一册; tungsten's melting point and the alkaline cell from Greenwood & Earnshaw (ch. 22, 28);
 * rusting from 九年级 unit 8, silicon from 必修第二册 ch. 5 §3, the safety match from Greenwood & Earnshaw ch. 12.
 * Models are Tripo meshes (see AGENTS.md for the asset list). EverydayThingsTest checks every id has
 * a model and every "inside" target exists.
 */
object EverydayThings {
    val ALL: List<EverydayThing> =
        listOf(
            EverydayThing(
                "balloon", listOf("He"), L10n("氦气球", "Helium balloon"),
                L10n("氦气比空气轻得多，所以气球往上飘；氦不会燃烧，比氢气安全。", "Helium is far lighter than air, so the balloon rises; it cannot burn, which makes it safer than hydrogen."),
                floats = true,
            ),
            EverydayThing(
                "pencil", listOf("C"), L10n("铅笔", "Pencil"),
                L10n("铅笔芯不是铅，是石墨和黏土。石墨里的碳原子连成一层一层，层间容易滑开，蹭在纸上就留下痕迹。", "Pencil lead is not lead but graphite and clay. In graphite the carbon atoms form layers that slide apart easily and rub off onto paper."),
                ThingInside.Allotrope(6, 1), L10n("看石墨的结构", "See graphite's structure"),
            ),
            EverydayThing(
                "diamond", listOf("C"), L10n("钻石", "Diamond"),
                L10n("钻石就是金刚石：每个碳原子都和 4 个碳原子相连，搭成立体的网，是自然界最硬的物质。", "A diamond is carbon in which every atom bonds to four others in a three-dimensional network: the hardest natural substance."),
                ThingInside.Allotrope(6, 0), L10n("看金刚石的结构", "See diamond's structure"),
            ),
            EverydayThing(
                "cola", listOf("C", "O"), L10n("汽水", "Fizzy drink"),
                L10n("汽水里的气泡是二氧化碳：加压溶在水里，一打开瓶盖压强变小，就跑出来了。", "The bubbles in a fizzy drink are carbon dioxide, dissolved under pressure and released when the cap comes off."),
                ThingInside.Molecule("co2"), L10n("看二氧化碳分子", "See the CO₂ molecule"),
            ),
            EverydayThing(
                "ice", listOf("H", "O"), L10n("冰块", "Ice"),
                L10n("冰是固态的水。每个水分子由 2 个氢原子和 1 个氧原子构成。", "Ice is solid water. Every water molecule is two hydrogen atoms and one oxygen atom."),
                ThingInside.Molecule("water"), L10n("看水分子", "See the water molecule"),
            ),
            EverydayThing(
                "salt", listOf("Na", "Cl"), L10n("食盐", "Table salt"),
                L10n("食盐的主要成分是氯化钠：钠离子和氯离子一个隔一个，整齐地排在一起。", "Table salt is mostly sodium chloride: sodium ions and chloride ions packed alternately in a regular pattern."),
                ThingInside.Reaction("na_cl2"), L10n("看钠和氯怎样变成盐", "See sodium and chlorine make salt"),
            ),
            EverydayThing(
                "can", listOf("Al"), L10n("易拉罐", "Drink can"),
                L10n("易拉罐是铝做的。铝很轻，表面会生成一层致密的氧化膜，所以不容易被腐蚀。", "Drink cans are aluminium: light, and protected from corrosion by the dense oxide film that forms on its surface."),
                ThingInside.Reaction("al_1"), L10n("看铝和氧气的反应", "See aluminium react with oxygen"),
            ),
            EverydayThing(
                "jar", listOf("Si", "O"), L10n("玻璃瓶", "Glass jar"),
                L10n("玻璃的主要成分是二氧化硅，和沙子一样。氢氟酸能和它反应，所以用来在玻璃上刻花。", "Glass is mainly silicon dioxide, like sand. Hydrofluoric acid reacts with it, which is how glass is etched."),
                ThingInside.Reaction("si_3"), L10n("看氢氟酸刻蚀玻璃", "See hydrofluoric acid etch glass"),
                glass = true,
            ),
            EverydayThing(
                "egg", listOf("Ca"), L10n("鸡蛋壳", "Eggshell"),
                L10n("蛋壳的主要成分是碳酸钙，和石灰石、贝壳一样，遇到酸会冒出二氧化碳气泡。", "Eggshell is mainly calcium carbonate, like limestone and seashells; acids make it fizz with carbon dioxide."),
                ThingInside.Reaction("ca_1"), L10n("看碳酸钙和酸的反应", "See calcium carbonate meet acid"),
            ),
            EverydayThing(
                "battery", listOf("Zn", "Mn"), L10n("碱性电池", "Alkaline battery"),
                L10n("电池里锌失去电子，二氧化锰得到电子。电子不能直接过去，只能从外面的电路绕，这就有了电流。", "Inside, zinc gives up electrons and manganese dioxide takes them. The electrons must travel round the outside circuit: that is the current."),
            ),
            EverydayThing(
                "wire", listOf("Cu"), L10n("铜电线", "Copper wire"),
                L10n("电线里是铜：导电性很好，又容易拉成细丝。", "Wires are copper: an excellent conductor that is easily drawn into thin strands."),
            ),
            EverydayThing(
                "bulb", listOf("W", "Ar"), L10n("白炽灯泡", "Light bulb"),
                L10n("灯丝是钨：熔点约 3422 ℃，是金属里最高的，烧到发白光也不熔化。灯泡里充了氩气保护灯丝。", "The filament is tungsten: it melts near 3422 °C, the highest of any metal, so it can glow white-hot. The bulb is filled with argon to protect it."),
                glows = true,
            ),
            EverydayThing(
                "ring", listOf("Au"), L10n("金戒指", "Gold ring"),
                L10n("金非常不活泼，不和氧气、水反应，所以金首饰戴很多年还是亮的。", "Gold is very unreactive: it ignores oxygen and water, so gold jewellery stays bright for years."),
            ),
            EverydayThing(
                "cinnabar", listOf("Hg", "S"), L10n("朱砂", "Cinnabar"),
                L10n("朱砂是硫化汞，红色颜料和印泥的原料。它含汞，有毒。", "Cinnabar is mercury sulfide, the red of traditional pigment and seal paste. It contains mercury and is toxic."),
                ThingInside.Reaction("hg_2"), L10n("看汞和硫的反应", "See mercury react with sulfur"),
            ),
            // v1.9.2
            EverydayThing(
                "nail", listOf("Fe"), L10n("生锈的铁钉", "Rusty nail"),
                L10n("铁和空气里的氧气、水一起作用就会生锈。铁锈的主要成分是氧化铁，红棕色、疏松，挡不住里面的铁继续生锈。", "Iron rusts when oxygen and water act on it together. Rust is mainly iron(III) oxide: red-brown and crumbly, so it cannot protect the iron underneath."),
                ThingInside.Reaction("fe_1"), L10n("看铁在氧气里燃烧", "See iron burn in oxygen"),
            ),
            EverydayThing(
                "chip", listOf("Si"), L10n("电脑芯片", "Computer chip"),
                L10n("芯片是用高纯度的硅做的。硅是半导体：导电能力在导体和绝缘体之间，可以做成微小的开关。硅来自沙子里的二氧化硅。", "Chips are made of very pure silicon, a semiconductor: it conducts between a conductor and an insulator, so it can be built into tiny switches. The silicon comes from the silicon dioxide in sand."),
                ThingInside.Reaction("si_2"), L10n("看怎样从沙子得到硅", "See silicon made from sand"),
            ),
            EverydayThing(
                "match", listOf("P", "S"), L10n("火柴", "Match"),
                L10n("安全火柴的盒子侧面涂着红磷，火柴头里有氯酸钾和硫。一划，摩擦生热，红磷先着火，再把火柴头点燃。", "A safety match box has red phosphorus on its striking strip; the match head holds potassium chlorate and sulfur. Striking heats the phosphorus until it ignites and lights the head."),
                ThingInside.Reaction("p_1"), L10n("看磷的燃烧", "See phosphorus burn"),
            ),
        )

    fun forSymbol(symbol: String): List<EverydayThing> = ALL.filter { symbol in it.symbols }

    fun thing(id: String): EverydayThing? = ALL.firstOrNull { it.id == id }
}
