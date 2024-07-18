package io.github.potjerodekool.openapi.common.generate

enum class ContentType(
    val type: String,
    private val charset: String?
) {
    JSON("application/json", null),
    JSON_UTF_8("application/json", "UTF-8"),
    JSON_PATCH_JSON("application/json-patch+json", null),
    JSON_PATCH_JSON_UTF_8("application/json-patch+json", "UTF-8"),
    APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded", null),
    APPLICATION_X_WWW_FORM_URLENCODED_UTF_8("application/x-www-form-urlencoded", "UTF-8"),
    APPLICATION_OCTET_STREAM("application/octet-stream", null),
    MULTIPART_FORM_DATA("multipart/form-data", null);

    fun stringValue(): String {
        return if (charset == null) {
            type
        } else {
            "$type;charset=$charset"
        }
    }

    companion object {
        fun fromValue(value: String): ContentType {
            val contentTypes = values()
                .filter { it.stringValue() == value }

            if (contentTypes.size == 1) {
                return contentTypes[0]
            } else {
                throw IllegalArgumentException("Unsupported content type: $value")
            }
        }
    }
}
