/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal val client = HttpClient(CIO) {
  install(ContentNegotiation) {
    json()
  }
}

fun httpClient(): HttpClient {
  return HttpClient(CIO) {
    expectSuccess = true
    defaultRequest {
      url("https://youtrack.jetbrains.com/api/")
    }
    install(Auth) {
      bearer {
        loadTokens {
          val accessToken = System.getenv("YOUTRACK_TOKEN") ?: error("Missing YOUTRACK_TOKEN")
          BearerTokens(accessToken, "")
        }
      }
    }
    install(ContentNegotiation) {
      json(
        Json {
          prettyPrint = true
          isLenient = true
        },
      )
    }
  }
}
