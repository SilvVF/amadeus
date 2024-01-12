package io.silv.domain.update

class GetUpdateCount(
    private val updatesRepository: UpdatesRepository
) {

    fun subscribe() = updatesRepository.observeUpdateCount()
}