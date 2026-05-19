package com.pc.fash_android_mobile.data.home

import com.pc.fash_android_mobile.data.editorial.EditorialGuideRepository

/**
 * Loads editorial carousel from common-service public API; keeps stub data for other discovery sections until APIs exist.
 */
class HttpHomeDiscoveryRepository(
    private val editorialGuideRepository: EditorialGuideRepository,
) : HomeDiscoveryRepository {

    override suspend fun loadDiscoveryBundle(): Result<HomeDiscoveryBundle> {
        val editorial = editorialGuideRepository.listCarousel().getOrElse { emptyList() }
        val stub = StubHomeDiscoveryRepository()
        return stub.loadDiscoveryBundle().map { base ->
            base.copy(editorialPosts = editorial)
        }
    }
}
