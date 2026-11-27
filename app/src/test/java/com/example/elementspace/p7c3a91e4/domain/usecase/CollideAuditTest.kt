package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.ElementReactions
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Not an assertion: writes what 碰一碰 answers for every element to build/collide-audit.txt. */
class CollideAuditTest {
    @Test
    fun writeAudit() {
        val elements = BundledElementRepository().elements()
        val collide = CollideElementsUseCase(BundledReactionRepository().all(), ElementReactions.NOTES)
        val out = StringBuilder()
        elements.forEach { a ->
            val reacts = ArrayList<String>(); val unknown = ArrayList<String>()
            var no = 0; var none = 0
            elements.filter { it != a }.forEach { b ->
                when (val r = collide(a, b)) {
                    is CollideResult.Reacts -> reacts += b.symbol + "(" + r.reactions.joinToString("/") { it.products[0].formula } + ")"
                    is CollideResult.NoReaction -> no++
                    is CollideResult.NoExperiment -> none++
                    CollideResult.NoCommonReaction -> unknown += b.symbol
                }
            }
            out.append("${a.atomicNumber} ${a.symbol} ${a.category} | reacts ${reacts.size}: ${reacts.joinToString(" ")} | noReaction $no | noExperiment $none | unknown ${unknown.size}: ${unknown.joinToString(",")}\n")
        }
        File("build").mkdirs(); File("build/collide-audit.txt").writeText(out.toString())
    }

    /** v1.17.0 audit: pairs every student tries must not answer "no common reaction". */
    @Test
    fun commonPairsHaveAnAnswer() {
        val elements = BundledElementRepository().elements().associateBy { it.symbol }
        val collide = CollideElementsUseCase(BundledReactionRepository().all(), ElementReactions.NOTES)
        fun hit(a: String, b: String) = collide(elements.getValue(a), elements.getValue(b))
        val metals = listOf("Li", "Na", "K", "Rb", "Cs", "Mg", "Ca", "Sr", "Ba")
        for (m in metals) for (x in listOf("F", "Cl", "Br", "I", "S", "O")) assertTrue("$m + $x", hit(m, x) is CollideResult.Reacts)
        for (m in listOf("Li", "Na", "K", "Rb", "Cs", "Mg", "Ca", "Sr", "Ba")) assertTrue("$m + H", hit(m, "H") is CollideResult.Reacts)
        for ((a, b) in listOf("Mn" to "O", "Mn" to "Cl", "Ni" to "Cl", "Co" to "S", "Si" to "F", "B" to "Cl", "P" to "Br", "Ag" to "I", "Pt" to "F", "Al" to "N")) assertTrue("$a + $b", hit(a, b) is CollideResult.Reacts)
        for (m in listOf("Au", "Pt", "Ag")) assertTrue("$m + O", hit(m, "O") is CollideResult.NoReaction)
        assertTrue(hit("Ra", "Cl") is CollideResult.NoExperiment)
        // One reaction per pair of elements unless the library really has two products (CO / CO2, Na2O / Na2O2, XeF2 / XeF4).
        val elementList = elements.values.toList()
        for (i in elementList.indices) for (j in i + 1 until elementList.size) {
            val r = collide(elementList[i], elementList[j])
            if (r is CollideResult.Reacts) assertTrue("${elementList[i].symbol} + ${elementList[j].symbol}", r.reactions.map { it.products[0].formula }.distinct().size == r.reactions.size)
        }
    }
}
