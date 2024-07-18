package io.github.potjerodekool.openapi.common.util

import io.github.potjerodekool.openapi.common.StatusCodeMatcher.is2XX
import io.github.potjerodekool.openapi.common.generate.ContentType
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import java.util.*

object OpenApiUtils {
    fun resolveResponseTypes(operation: Operation): List<SchemaWithInfo> {
        if (operation.responses == null) {
            return listOf()
        }

        val responses = operation.responses

        return responses.entries.stream()
            .filter { entry: Map.Entry<String, ApiResponse?> -> "default" != entry.key }
            .map { entry: Map.Entry<String?, ApiResponse> ->
                val response = entry.value
                val contentMediaType = resolveResponseMediaType(response.content)
                Optional.ofNullable(contentMediaType)
            }
            .filter { obj: Optional<SchemaWithInfo> -> obj.isPresent }
            .map { obj: Optional<SchemaWithInfo> -> obj.get() }
            .toList()
    }

    fun resolveResponseMediaType(contentMediaType: Content?): SchemaWithInfo? {
        val jsonSchemaAndExtensions = findJsonMediaType(contentMediaType)

        if (jsonSchemaAndExtensions != null) {
            return jsonSchemaAndExtensions
        } else {
            //Not returning json, maybe image/jpg or */*
            if (contentMediaType != null && contentMediaType.size == 1) {
                val entries: Set<Map.Entry<String, MediaType>> = contentMediaType.entries

                val entry = entries.iterator().next()
                val mediaType = entry.value

                return if (mediaType.schema != null
                ) SchemaWithInfo(
                    mediaType.schema,
                    mediaType.extensions,
                    ContentType.fromValue(entry.key)
                )
                else null
            }
            return null
        }
    }

    fun findJsonMediaType(contentMediaType: Content?): SchemaWithInfo? {
        if (contentMediaType == null) {
            return null
        } else {
            val content = getMediaType(contentMediaType)
            val mediaType = content!!.value

            if (mediaType.schema == null) {
                return null
            }

            return SchemaWithInfo(
                mediaType.schema,
                mediaType.extensions ?: emptyMap(),
                content.key
            )
        }
    }

    fun getMediaType(contentMediaType: Content): Pair<ContentType, MediaType>? {
        return Arrays.stream(ContentType.values())
            .map { contentType: ContentType ->
                val mediaType = contentMediaType[contentType.stringValue()]
                if (mediaType != null
                ) ImmutablePair(contentType, mediaType)
                else null
            }
            .filter { obj: ImmutablePair<ContentType, MediaType>? -> Objects.nonNull(obj) }
            .findFirst()
            .orElse(null)
    }

    fun isMultiPart(contentMediaType: Content): Boolean {
        return contentMediaType.keys.stream()
            .anyMatch { it: String -> it.startsWith("multipart/") }
    }

    fun isImageOrVideo(contentMediaType: Content): Boolean {
        return contentMediaType.keys.stream()
            .anyMatch { it: String ->
                (it.startsWith("image/")
                        || it.startsWith("video/"))
            }
    }

    fun findOkResponse(responses: ApiResponses?): Optional<MutableMap.MutableEntry<String, ApiResponse>> {
        return if (responses != null) responses.entries.stream()
            .filter { entry: Map.Entry<String?, ApiResponse?> -> is2XX(entry.key) }
            .findFirst()
        else Optional.empty()
    }
}
