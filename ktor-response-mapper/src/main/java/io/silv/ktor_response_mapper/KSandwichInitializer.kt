/*
 * Designed and developed by 2020 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("RedundantVisibilityModifier", "unused")
package io.silv.ktor_response_mapper

import io.silv.ktor_response_mapper.operators.SandwichOperator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * @author skydoves (Jaewoong Eum)
 *
 * SandwichInitializer is a rules and strategies initializer of the network response.
 */
public object KSandwichInitializer {

  /**
   * @author skydoves (Jaewoong Eum)
   *
   * determines the success code range of network responses.
   *
   * if a network request is successful and the response code is in the [successCodeRange],
   * its response will be a [ApiResponse.Success].
   *
   * if a network request is successful but out of the [successCodeRange] or failure,
   * the response will be a [ApiResponse.Failure.Error].
   * */
  @JvmStatic
  public var successCodeRange: IntRange = 200..299


  /**
   * @author skydoves (Jaewoong Eum)
   *
   * A list of global operators that is executed by [ApiResponse]s globally on each response.
   *
   * [io.silv.ktor_response_mapper.operators.ApiResponseOperator] which allows you to handle success and error response instead of
   * the [ApiResponse.onSuccess], [ApiResponse.onError], [ApiResponse.onException] transformers.
   * [io.silv.ktor_response_mapper.operators.ApiResponseSuspendOperator] can be used for suspension scope.
   *
   * Via setting [sandwichOperators], you don't need to set operator for every [ApiResponse].
   */
  @JvmStatic
  public var sandwichOperators: MutableList<SandwichOperator> = mutableListOf()

  /**
   * @author skydoves (Jaewoong Eum)
   *
   * A [CoroutineScope] for executing and operating the overall Retrofit network requests.
   */

  @JvmSynthetic
  public var sandwichScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
}
