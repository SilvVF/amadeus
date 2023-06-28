package io.silv.ktor_response_mapper.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.silv.ktor_response_mapper.operators.SandwichOperator
import io.silv.ktor_response_mapper.KSandwichInitializer


interface KSandwichClient {

    val client: HttpClient

    fun addGlobalOperator(operator: SandwichOperator)

    companion object {
        fun <T : HttpClientEngineConfig> createClient(
            engineFactory: HttpClientEngineFactory<T>,
            block: HttpClientConfig<T>.() -> Unit = {}
        ): KSandwichClient {
            val config: HttpClientConfig<T> = HttpClientConfig<T>().apply(block)
            val engine = engineFactory.create {
                apply {
                    block(config)
                }
            }


            return KSandwichClientImpl(
                client = HttpClient(engine, config)
            )
        }
    }
}

internal class KSandwichClientImpl(
    override val client: HttpClient
): KSandwichClient {

    override fun addGlobalOperator(operator: SandwichOperator) {
        KSandwichInitializer.sandwichOperators += operator
    }
}





