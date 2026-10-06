package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.Allotrope
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.Isotope

/** Read-only access to the bundled element dataset. */
interface ElementRepository {
    fun elements(): List<Element>

    fun element(atomicNumber: Int): Element?

    /** Notable isotopes, most abundant first. Never empty for a known element. */
    fun isotopes(atomicNumber: Int): List<Isotope>

    /** Allotropes or the room-temperature crystal structure; empty when none is bundled. */
    fun allotropes(atomicNumber: Int): List<Allotrope>
}
