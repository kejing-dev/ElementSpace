package com.example.elementspace.p7c3a91e4.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.domain.usecase.FilterElementsUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.TrendScaleUseCase

class PeriodicTableViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PeriodicTableViewModel(
            elementRepository = BundledElementRepository(),
            sessionRepository = InMemorySessionRepository.shared,
            filterElements = FilterElementsUseCase(),
            trendScale = TrendScaleUseCase(),
        )
            as T
}
