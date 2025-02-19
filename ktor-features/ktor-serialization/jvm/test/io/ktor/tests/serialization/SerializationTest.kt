/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.serialization

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.test.*

class SerializationTest {

    @Test
    @Ignore
    fun testMap() = withTestApplication {
        val uc = "\u0422"

        application.install(ContentNegotiation) {
            register(ContentType.Application.Json, SerializationConverter())
        }
        application.routing {
            val model = mapOf("id" to 1, "title" to "Hello, World!", "unicode" to uc)
            get("/") {
                call.respond(model)
            }
            post("/") {
                val map = call.receive<Map<*, *>>()
                val text = map.entries.joinToString { "${it.key}=${it.value}" }
                call.respond(text)
            }
        }

        handleRequest(HttpMethod.Get, "/") {
            addHeader("Accept", "application/json")
        }.response.let { response ->
            assertEquals(HttpStatusCode.OK, response.status())
            assertNotNull(response.content)
            assertEquals(listOf("""{"id":1,"title":"Hello, World!","unicode":"$uc"}"""), response.content!!.lines())
            val contentTypeText = assertNotNull(response.headers[HttpHeaders.ContentType])
            assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), ContentType.parse(contentTypeText))
        }

        handleRequest(HttpMethod.Post, "/") {
            addHeader("Accept", "text/plain")
            addHeader("Content-Type", "application/json")
            setBody("""{"id":1,"title":"Hello, World!","unicode":"$uc"}""")
        }.response.let { response ->
            assertEquals(HttpStatusCode.OK, response.status())
            assertNotNull(response.content)
            assertEquals(listOf("""id=1.0, title=Hello, World!, unicode=$uc"""), response.content!!.lines())
            val contentTypeText = assertNotNull(response.headers[HttpHeaders.ContentType])
            assertEquals(ContentType.Text.Plain.withCharset(Charsets.UTF_8), ContentType.parse(contentTypeText))
        }
    }

    @Test
    fun testEntity() = withTestApplication {
        val uc = "\u0422"
        application.install(ContentNegotiation) {
            register(ContentType.Application.Json, SerializationConverter())
        }

        application.routing {
            val model = MyEntity(
                777, "Cargo",
                listOf(
                    ChildEntity("Qube", 1),
                    ChildEntity("Sphere", 2),
                    ChildEntity(uc, 3)
                )
            )

            get("/") {
                call.respond(model)
            }
            post("/") {
                val entity = call.receive<MyEntity>()
                call.respond(entity.toString())
            }
        }

        handleRequest(HttpMethod.Get, "/") {
            addHeader("Accept", "application/json")
        }.response.let { response ->
            assertEquals(HttpStatusCode.OK, response.status())
            assertNotNull(response.content)
            assertEquals(
                listOf("""{"id":777,"name":"Cargo","children":[{"item":"Qube","quantity":1},{"item":"Sphere","quantity":2},{"item":"$uc","quantity":3}]}"""),
                response.content!!.lines()
            )
            val contentTypeText = assertNotNull(response.headers[HttpHeaders.ContentType])
            assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), ContentType.parse(contentTypeText))
        }

        handleRequest(HttpMethod.Post, "/") {
            addHeader("Content-Type", "application/json")
            setBody("""{"id":777,"name":"Cargo","children":[{"item":"Qube","quantity":1},{"item":"Sphere","quantity":2},{"item":"$uc", "quantity":3}]}""")
        }.response.let { response ->
            assertEquals(HttpStatusCode.OK, response.status())
            assertNotNull(response.content)
            assertEquals(
                listOf("""MyEntity(id=777, name=Cargo, children=[ChildEntity(item=Qube, quantity=1), ChildEntity(item=Sphere, quantity=2), ChildEntity(item=$uc, quantity=3)])"""),
                response.content!!.lines()
            )
            val contentTypeText = assertNotNull(response.headers[HttpHeaders.ContentType])
            assertEquals(ContentType.Text.Plain.withCharset(Charsets.UTF_8), ContentType.parse(contentTypeText))
        }

    }

    @Serializable
    private data class TextPlainData(val x: Int)

    @Test
    fun testGsonOnTextAny(): Unit = withTestApplication {
        application.install(ContentNegotiation) {
            serialization()
            register(contentType = ContentType.Text.Any, converter = SerializationConverter())
        }

        application.routing {
            post("/") {
                val instance = call.receive<TextPlainData>()
                assertEquals(TextPlainData(777), instance)
                call.respondText("OK")
            }
        }

        handleRequest(HttpMethod.Post, "/") {
            addHeader(HttpHeaders.ContentType, "text/plain")
            setBody("{\"x\": 777}")
        }.let {
            assertEquals(HttpStatusCode.OK, it.response.status())
            assertEquals("OK", it.response.content)
        }
        handleRequest(HttpMethod.Post, "/") {
            addHeader(HttpHeaders.ContentType, "application/json")
            setBody("{\"x\": 777}")
        }.let {
            assertEquals(HttpStatusCode.OK, it.response.status())
            assertEquals("OK", it.response.content)
        }
    }

    @Serializable
    private class NullValues

    @Test
    fun testReceiveNullValue(): Unit = withTestApplication {
        application.install(ContentNegotiation) {
            serialization()
            register(contentType = ContentType.Text.Any, converter = SerializationConverter())
        }

        application.routing {
            post("/") {
                val result = try {
                    call.receive<NullValues>().toString()
                } catch (expected: JsonParsingException) {
                    "OK"
                }
                call.respondText(result)
            }
        }

        handleRequest(HttpMethod.Post, "/") {
            addHeader(HttpHeaders.ContentType, "application/json")
            setBody("null")
        }.let {
            assertEquals(HttpStatusCode.OK, it.response.status())
            assertEquals("OK", it.response.content)
        }
    }

}

@Serializable
data class MyEntity(val id: Int, val name: String, val children: List<ChildEntity>)

@Serializable
data class ChildEntity(val item: String, val quantity: Int)

