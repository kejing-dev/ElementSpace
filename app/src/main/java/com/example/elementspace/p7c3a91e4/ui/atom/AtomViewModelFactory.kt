package com.example.elementspace.p7c3a91e4.ui.atom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.domain.usecase.BuildAllotropeLatticeUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.BuildNucleusLayoutUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.ElectronConfigurationUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.SampleOrbitalCloudUseCase

class AtomViewModelFactory(private val atomicNumber: Int) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AtomViewModel(
            atomicNumber = atomicNumber,
            elementRepository = BundledElementRepository(),
            sessionRepository = InMemorySessionRepository.shared,
            electronConfiguration = ElectronConfigurationUseCase(),
            buildNucleus = BuildNucleusLayoutUseCase(),
            sampleCloud = SampleOrbitalCloudUseCase(),
            buildLattice = BuildAllotropeLatticeUseCase(),
        )
            as T
}
