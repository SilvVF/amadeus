package io.silv.data.update

class GetUpdateCount(
    private val updatesRepository: UpdatesRepository
) {

    fun subscribe() = updatesRepository.observeUpdateCount()
}