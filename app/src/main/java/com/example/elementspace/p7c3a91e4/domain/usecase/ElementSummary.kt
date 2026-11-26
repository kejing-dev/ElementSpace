package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.L10n

/**
 * "一句话看懂" (v1.7.0): one plain sentence per element at the top of its card. Common elements have
 * their own line; the rest get their family's line; elements with no chemistry to show reuse the
 * note that explains why. Facts: PEP 九年级 / 必修第一册, and Greenwood & Earnshaw for the metals.
 */
object ElementSummary {
    private val OWN: Map<String, L10n> =
        mapOf(
            "H" to L10n("最轻的元素，只有 1 个质子和 1 个电子。氢气能燃烧，烧完生成水。", "The lightest element: one proton and one electron. Hydrogen gas burns, and what it makes is water."),
            "He" to L10n("只有 2 个电子，正好把第一层排满，所以几乎不和任何东西反应。比空气轻，用来充气球。", "Its two electrons exactly fill the first shell, so it reacts with almost nothing. Lighter than air: it fills balloons."),
            "Li" to L10n("最轻的金属。最外层 1 个电子容易失去，锂电池靠的就是这一点。", "The lightest metal. Its single outer electron is easily lost, which is what lithium batteries use."),
            "C" to L10n("最外层 4 个电子，能和别的原子连成长链和环，是生命和所有有机物的骨架。", "Four outer electrons let it link into long chains and rings: the backbone of life and all organic matter."),
            "N" to L10n("空气里大约 78% 是氮气。两个氮原子之间连得非常牢，所以氮气很不活泼。", "About 78% of air is nitrogen. Its two atoms hold each other very tightly, so nitrogen gas is unreactive."),
            "O" to L10n("最外层 6 个电子，再得 2 个就排满，很容易夺别人的电子。燃烧和呼吸都离不开它。", "Six outer electrons, two short of full, so it readily takes electrons from others. Burning and breathing both need it."),
            "F" to L10n("得电子能力最强的元素，几乎能和所有元素反应。", "The strongest electron taker of all: it reacts with almost every element."),
            "Ne" to L10n("最外层电子排满，不反应。通电时发出红光，就是霓虹灯。", "A full outer shell: it does not react. With electricity it glows red: the neon light."),
            "Na" to L10n("最外层只有 1 个电子，很容易丢掉，所以特别活泼，遇水就剧烈反应。", "It has a single outer electron and loses it very easily, so it is extremely reactive and reacts violently with water."),
            "Mg" to L10n("最外层 2 个电子，容易失去。点燃时发出耀眼的白光。", "Two outer electrons, easily lost. It burns with a dazzling white light."),
            "Al" to L10n("其实很活泼，但表面会生成一层致密的氧化膜保护自己，所以不容易锈穿。", "Actually quite reactive, but a dense oxide film forms on its surface and protects it from corroding through."),
            "Si" to L10n("导电能力介于金属和非金属之间（半导体），是芯片和玻璃的主角。", "It conducts somewhere between a metal and a non-metal (a semiconductor): the heart of chips and of glass."),
            "P" to L10n("白磷在空气里会自己烧起来。骨骼和 DNA 里也有磷。", "White phosphorus catches fire by itself in air. Phosphorus is also in bones and DNA."),
            "S" to L10n("黄色的固体。燃烧时生成有刺激性气味的二氧化硫。", "A yellow solid. It burns to sulfur dioxide, a gas with a sharp smell."),
            "Cl" to L10n("最外层 7 个电子，再得 1 个就排满，很容易夺电子。黄绿色的有毒气体，能消毒。", "Seven outer electrons, one short of full, so it readily takes one. A yellow-green toxic gas that disinfects."),
            "Ar" to L10n("最外层电子排满，不反应。空气里约 0.93% 是氩，常用作保护气。", "A full outer shell: it does not react. About 0.93% of air is argon, used as a protective gas."),
            "K" to L10n("和钠同族，最外层电子离原子核更远，比钠还活泼。", "In sodium's family, with its outer electron even further from the nucleus: more reactive than sodium."),
            "Ca" to L10n("最外层 2 个电子，容易失去。骨头、贝壳和石灰石里都有钙。", "Two outer electrons, easily lost. Bones, shells and limestone all contain calcium."),
            "Ti" to L10n("又轻又结实，还耐腐蚀，用在飞机和人工关节上。", "Light, strong and corrosion-resistant: used in aircraft and artificial joints."),
            "Fe" to L10n("用得最多的金属。在潮湿的空气里会和氧气、水一起作用而生锈。", "The most widely used metal. In damp air it rusts through the joint action of oxygen and water."),
            "Cu" to L10n("不活泼的红色金属，导电性很好，所以电线用铜。", "An unreactive red metal and an excellent conductor, which is why wires are copper."),
            "Zn" to L10n("比铁活泼，镀在铁的外面能保护铁；和酸反应放出氢气。", "More reactive than iron: plated on iron it protects it. It gives off hydrogen with acids."),
            "Br" to L10n("常温下唯一的液态非金属，红棕色，容易挥发。", "The only non-metal that is liquid at room temperature: red-brown and volatile."),
            "Ag" to L10n("导电性最好的金属。不活泼，但遇到含硫的气体表面会变黑。", "The best electrical conductor of all metals. Unreactive, but sulfur-containing gases tarnish it black."),
            "I" to L10n("紫黑色的固体，加热直接变成紫色蒸气。遇到淀粉变蓝。", "A violet-black solid that turns straight into purple vapour when heated. It turns starch blue."),
            "Xe" to L10n("稀有气体里比较「好说话」的一个：能和氟化合。", "The most approachable of the noble gases: it does combine with fluorine."),
            "W" to L10n("熔点最高的金属，约 3422 ℃，白炽灯的灯丝就是钨丝。", "The metal with the highest melting point, about 3422 °C: the filament of an incandescent lamp."),
            "Au" to L10n("非常不活泼，加热也不和氧气反应，所以金首饰不会变色。", "Very unreactive: even heated it ignores oxygen, so gold jewellery never tarnishes."),
            "Hg" to L10n("常温下唯一的液态金属。蒸气有毒。", "The only metal that is liquid at room temperature. Its vapour is toxic."),
            "Pb" to L10n("又重又软的金属，有毒。", "A heavy, soft metal; toxic."),
            "U" to L10n("有放射性的重金属，可以做核燃料。", "A radioactive heavy metal used as nuclear fuel."),
        )

    fun of(element: Element, note: L10n?): L10n {
        OWN[element.symbol]?.let { return it }
        // Noble gases with their own line are handled above; any other noted element is one we cannot experiment on.
        if (note != null) return L10n("${element.nameZh}：${note.zh}", "${element.nameEn}: ${note.en}")
        val zh = element.nameZh
        val en = element.nameEn
        return when (element.category) {
            ElementCategory.ALKALI_METAL -> L10n("${zh}和钠同族，最外层只有 1 个电子，很容易失去，非常活泼。", "$en is in sodium's family: a single outer electron, lost very easily, so it is very reactive.")
            ElementCategory.ALKALINE_EARTH -> L10n("${zh}最外层 2 个电子，容易失去，是比较活泼的金属。", "$en has two outer electrons, easily lost: a fairly reactive metal.")
            ElementCategory.TRANSITION_METAL -> L10n("${zh}是过渡金属：有金属光泽，能导电、导热。", "$en is a transition metal: shiny, and a conductor of electricity and heat.")
            ElementCategory.POST_TRANSITION -> L10n("${zh}是金属，比较软，熔点不高。", "$en is a metal, fairly soft and low-melting.")
            ElementCategory.METALLOID -> L10n("${zh}的性质介于金属和非金属之间。", "${en}'s properties lie between those of metals and non-metals.")
            ElementCategory.NONMETAL -> L10n("${zh}是非金属，反应时容易得到电子，或者和别的原子共用电子。", "$en is a non-metal: in reactions it tends to gain electrons or share them with other atoms.")
            ElementCategory.HALOGEN -> L10n("${zh}和氯同族，最外层 7 个电子，再得 1 个就排满，容易得电子。", "$en is in chlorine's family: seven outer electrons, one short of full, so it gains one easily.")
            ElementCategory.NOBLE_GAS -> L10n("${zh}是稀有气体，最外层电子已经排满，很难发生反应。", "$en is a noble gas: its outer shell is full, so it hardly reacts.")
            ElementCategory.LANTHANIDE -> L10n("${zh}是稀土金属，化学性质和其他稀土元素很像，在化合物里通常失去 3 个电子。", "$en is a rare-earth metal, chemically much like the others; in compounds it usually has lost three electrons.")
            ElementCategory.ACTINIDE -> L10n("${zh}是放射性金属。", "$en is a radioactive metal.")
        }
    }
}
