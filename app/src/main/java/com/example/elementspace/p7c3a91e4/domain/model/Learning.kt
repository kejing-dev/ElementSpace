package com.example.elementspace.p7c3a91e4.domain.model

/** How much the cards show (v1.7.0). Beginner is the default: reviewers found the full content too dense. */
enum class LearningLevel(val zh: String, val en: String) {
    BASIC("入门", "Beginner"),
    SCHOOL("中学", "School"),
    UNIVERSITY("大学", "University"),
}

/** Something a guided path asks an open atom Volume to do. */
sealed interface AtomAction {
    data class Charge(val delta: Int) : AtomAction

    data class View(val view: AtomViewMode) : AtomAction

    /** Shows the allotrope view on allotrope [index]. */
    data class Allotrope(val index: Int) : AtomAction
}

data class AtomCommand(val atomicNumber: Int, val action: AtomAction)

/** What pressing a guided step's button does. One tap each; nothing to drag. */
sealed interface GuideAction {
    data class OpenAtom(val atomicNumber: Int) : GuideAction

    data class Atom(val atomicNumber: Int, val action: AtomAction) : GuideAction

    data class Reaction(val id: String) : GuideAction

    data class Molecule(val id: String) : GuideAction

    data class Collide(val a: Int, val b: Int) : GuideAction
}

/** One step: what to notice ([text]), and the single button that makes it happen. */
data class GuideStep(val text: L10n, val button: L10n, val action: GuideAction)

data class GuidePath(val id: String, val title: L10n, val steps: List<GuideStep>, val done: L10n)
