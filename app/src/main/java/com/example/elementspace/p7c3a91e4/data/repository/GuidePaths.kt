package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.AtomAction
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.model.GuideAction
import com.example.elementspace.p7c3a91e4.domain.model.GuidePath
import com.example.elementspace.p7c3a91e4.domain.model.GuideStep
import com.example.elementspace.p7c3a91e4.domain.model.L10n

/**
 * "跟我学" (v1.7.0): short paths, one sentence and one tap per step. Facts follow the PEP textbooks
 * (九年级 units 3, 4, 6, 8; 必修第一册 ch. 2, 4; 必修第二册 ch. 5 §3). Reaction and molecule ids must exist in the libraries
 * (GuidePathsTest checks).
 */
object GuidePaths {
    private fun step(zh: String, en: String, buttonZh: String, buttonEn: String, action: GuideAction) = GuideStep(L10n(zh, en), L10n(buttonZh, buttonEn), action)

    val ALL: List<GuidePath> =
        listOf(
            GuidePath(
                "sodium_water",
                L10n("为什么钠遇水会炸", "Why sodium explodes in water"),
                listOf(
                    step("先认识钠原子：11 个电子分三层，2、8、1。", "Meet the sodium atom: 11 electrons in three shells, 2, 8, 1.", "打开钠", "Open sodium", GuideAction.OpenAtom(11)),
                    step("最外层只有 1 个电子，离原子核远，抓不牢。拿走它试试。", "The outer shell holds a single electron, far from the nucleus and loosely held. Take it away.", "拿走这个电子", "Remove that electron", GuideAction.Atom(11, AtomAction.Charge(1))),
                    step("钠变成了 Na⁺，剩下的电子和氖一样排满，很稳定。所以钠总想把这个电子送出去。遇到水会怎样？", "Sodium is now Na⁺, with full shells like neon: stable. So sodium is always ready to give that electron away. What happens in water?", "看钠和水反应", "Watch sodium and water", GuideAction.Reaction("na_3")),
                    step("钠把电子给了水，生成氢氧化钠和氢气，还放出大量的热，氢气可能被点着。钾和钠同族，再看看钾。", "Sodium hands electrons to water, giving sodium hydroxide and hydrogen plus a lot of heat, which can ignite the hydrogen. Potassium is in the same group: look at it.", "对比钾", "Compare potassium", GuideAction.OpenAtom(19)),
                ),
                L10n("钾的最外层电子在第 4 层，离核更远，更容易失去，所以钾遇水比钠更剧烈。", "Potassium's outer electron sits in the fourth shell, even further out and easier to lose, so it reacts with water more violently than sodium."),
            ),
            GuidePath(
                "table_salt",
                L10n("食盐是怎么来的", "Where table salt comes from"),
                listOf(
                    step("食盐是氯化钠。先看钠：最外层 1 个电子，容易失去。", "Table salt is sodium chloride. First sodium: one outer electron, easily lost.", "打开钠", "Open sodium", GuideAction.OpenAtom(11)),
                    step("再看氯：最外层 7 个电子，再得 1 个就排满 8 个。", "Now chlorine: seven outer electrons, one short of a full eight.", "打开氯", "Open chlorine", GuideAction.OpenAtom(17)),
                    step("一个想给，一个想要。让它们碰一碰。", "One wants to give, the other wants to take. Bring them together.", "钠和氯碰一碰", "Collide sodium and chlorine", GuideAction.Collide(11, 17)),
                    step("看电子是怎么从钠跑到氯那边去的。", "Watch the electron move from sodium to chlorine.", "看 3D 反应", "Watch in 3D", GuideAction.Reaction("na_cl2")),
                ),
                L10n("钠失去电子变成 Na⁺，氯得到电子变成 Cl⁻，一正一负紧紧吸在一起，就是食盐 NaCl。", "Sodium becomes Na⁺, chlorine becomes Cl⁻, and the opposite charges hold tightly together: that is table salt, NaCl."),
            ),
            GuidePath(
                "carbon_forms",
                L10n("钻石和铅笔芯为什么都是碳", "Why diamond and pencil lead are both carbon"),
                listOf(
                    step("碳原子最外层有 4 个电子，可以和 4 个别的原子相连。", "A carbon atom has four outer electrons and can bond to four other atoms.", "打开碳", "Open carbon", GuideAction.OpenAtom(6)),
                    step("金刚石（钻石）：每个碳都和 4 个碳相连，搭成立体的网。", "Diamond: every carbon bonds to four others, building a three-dimensional network.", "看金刚石", "See diamond", GuideAction.Atom(6, AtomAction.Allotrope(0))),
                    step("这张网每个方向都很牢，所以金刚石是自然界最硬的物质。再看石墨。", "The network is strong in every direction, which makes diamond the hardest natural substance. Now graphite.", "看石墨", "See graphite", GuideAction.Atom(6, AtomAction.Allotrope(1))),
                ),
                L10n("石墨里的碳连成一层一层的平面，层和层之间容易滑开，所以铅笔芯很软、能在纸上留下痕迹。同一种原子，排法不同，性质就不同。", "In graphite the carbons form flat layers that slide over each other easily, so pencil lead is soft and leaves a mark. Same atoms, different arrangement, different properties."),
            ),
            GuidePath(
                "water",
                L10n("水是怎么组成的", "What water is made of"),
                listOf(
                    step("氢原子只有 1 个电子，再有 1 个就排满第一层。", "A hydrogen atom has one electron; one more would fill its first shell.", "打开氢", "Open hydrogen", GuideAction.OpenAtom(1)),
                    step("氧原子最外层 6 个电子，还差 2 个排满。", "An oxygen atom has six outer electrons, two short of full.", "打开氧", "Open oxygen", GuideAction.OpenAtom(8)),
                    step("它们都不肯把电子送人，于是共用。看氢气在氧气里燃烧。", "Neither gives its electrons away, so they share. Watch hydrogen burn in oxygen.", "看氢气燃烧", "Watch hydrogen burn", GuideAction.Reaction("h2_o2")),
                    step("每个氧和两个氢各共用一对电子，得到水分子。", "Each oxygen shares one pair of electrons with each of two hydrogens: a water molecule.", "看水分子", "See the water molecule", GuideAction.Molecule("water")),
                ),
                L10n("水分子不是直线，是弯的，两个 O—H 键的夹角约 104.5°。", "The water molecule is bent, not straight: the two O–H bonds are about 104.5° apart."),
            ),
            GuidePath(
                "balloon",
                L10n("气球为什么充氦气，不充氢气", "Why balloons use helium, not hydrogen"),
                listOf(
                    step("氢气最轻，但是氢原子只有 1 个电子，很想再要 1 个。", "Hydrogen is the lightest gas, but its atom has one electron and wants one more.", "打开氢", "Open hydrogen", GuideAction.OpenAtom(1)),
                    step("所以氢气遇到氧气，一点就着，甚至爆炸。", "So hydrogen meeting oxygen burns at a spark, or explodes.", "氢和氧碰一碰", "Collide hydrogen and oxygen", GuideAction.Collide(1, 8)),
                    step("氦原子有 2 个电子，第一层正好排满。", "A helium atom has two electrons: its first shell is exactly full.", "打开氦", "Open helium", GuideAction.OpenAtom(2)),
                    step("排满了就既不想给、也不想要。让氦和氧碰一碰。", "A full shell neither gives nor takes. Collide helium and oxygen.", "氦和氧碰一碰", "Collide helium and oxygen", GuideAction.Collide(2, 8)),
                ),
                L10n("氦不反应、不会燃烧，也比空气轻得多，所以气球和飞艇用氦气更安全。", "Helium does not react or burn and is much lighter than air, so it is the safe choice for balloons and airships."),
            ),
            GuidePath(
                "firework_colours",
                L10n("焰火的颜色从哪来", "Where firework colours come from"),
                listOf(
                    step("先做个实验：点燃酒精灯，再换几种盐，看火焰的颜色。", "Start with an experiment: light the lamp, then try several salts and watch the flame.", "打开焰色试验", "Open the flame test", GuideAction.Lab("flame_test")),
                    step("红色的焰火里有锶。看看锶原子：38 个电子，分五层。", "Red fireworks contain strontium. Look at its atom: 38 electrons in five shells.", "打开锶", "Open strontium", GuideAction.OpenAtom(38)),
                    step("绿色的焰火里常有钡或铜。再看看铜。", "Green fireworks usually contain barium or copper. Now look at copper.", "打开铜", "Open copper", GuideAction.OpenAtom(29)),
                ),
                L10n("金属原子受热时，电子吸收能量跳到离核更远的地方，再跳回来时把能量变成光放出来。每种元素的电子能量不同，放出的光颜色就不同。", "When a metal atom is heated its electrons absorb energy and jump further from the nucleus; falling back, they give the energy out as light. Each element's electrons have their own energies, so each gives its own colour."),
            ),
            // v1.10.0
            GuidePath(
                "iron_rust",
                L10n("铁为什么会生锈", "Why iron rusts"),
                listOf(
                    step("铁原子有 26 个电子，最外层只有 2 个，容易失去。", "An iron atom has 26 electrons with only two in its outer shell, easily lost.", "打开铁", "Open iron", GuideAction.OpenAtom(26)),
                    step("铁和空气里的氧气、水一起作用，就慢慢生锈。铁锈是红棕色的，很疏松。", "With oxygen and water from the air acting together, iron slowly rusts. Rust is red-brown and crumbly.", "看生锈的铁钉", "See a rusty nail", GuideAction.Atom(26, AtomAction.Thing("nail"))),
                    step("生锈很慢。让铁和氧直接碰一碰，看它们会不会反应。", "Rusting is slow. Bring iron and oxygen together directly and see whether they react.", "铁和氧碰一碰", "Collide iron and oxygen", GuideAction.Collide(26, 8)),
                    step("在纯氧里，烧红的铁丝会剧烈燃烧，火星四射。到实验台上看。", "In pure oxygen a red-hot iron wire burns fiercely, throwing sparks. See it on the bench.", "做铁丝燃烧实验", "Burn iron wire", GuideAction.Lab("fe_o2")),
                ),
                L10n("生锈和燃烧都是铁和氧气反应，一个很慢，一个很剧烈。刷油漆、涂油、镀上别的金属，把氧气和水隔开，铁就不容易生锈。", "Rusting and burning are both iron reacting with oxygen: one slow, one violent. Paint, oil or a coating of another metal keeps oxygen and water away, so the iron does not rust."),
            ),
            GuidePath(
                "chip_sand",
                L10n("芯片为什么说是沙子做的", "Why chips are said to be made of sand"),
                listOf(
                    step("硅原子最外层有 4 个电子，和碳在同一族。", "A silicon atom has four outer electrons and sits in the same group as carbon.", "打开硅", "Open silicon", GuideAction.OpenAtom(14)),
                    step("高纯度的硅是半导体，导电能力在导体和绝缘体之间，芯片就是用它做的。", "Very pure silicon is a semiconductor, conducting between a conductor and an insulator. Chips are made from it.", "看电脑芯片", "See a computer chip", GuideAction.Atom(14, AtomAction.Thing("chip"))),
                    step("硅从哪来？沙子的主要成分是二氧化硅，在高温下用碳把氧夺走，就得到硅。", "Where does it come from? Sand is mostly silicon dioxide; at high temperature carbon takes the oxygen away, leaving silicon.", "看沙子变成硅", "See sand become silicon", GuideAction.Reaction("si_2")),
                    step("玻璃的主要成分也是二氧化硅。", "Glass is mainly silicon dioxide too.", "看玻璃瓶", "See a glass jar", GuideAction.Atom(14, AtomAction.Thing("jar"))),
                ),
                L10n("硅在地壳里的含量仅次于氧，排第二。沙子、玻璃、芯片里都有它。", "Silicon is the second most abundant element in the Earth's crust, after oxygen. It is in sand, glass and chips."),
            ),
        )

    fun path(id: String): GuidePath? = ALL.firstOrNull { it.id == id }
}
