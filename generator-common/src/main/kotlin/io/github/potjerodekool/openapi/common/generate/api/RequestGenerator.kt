package io.github.potjerodekool.openapi.common.generate.api

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.codegen.io.Filer
import io.github.potjerodekool.codegen.io.Location
import io.github.potjerodekool.openapi.common.GeneratorConfig
import io.github.potjerodekool.openapi.common.generate.Templates
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RequestGenerator(
    generatorConfig: GeneratorConfig,
    environment: Environment,
    private val templates: Templates
) {
    private val basePackageName = generatorConfig.basePackageName
    private val filer: Filer = environment.filer

    fun generate() {
        val st = templates.getInstanceOf("/request/request")

        st.add("packageName", this.basePackageName)
        st.add("generatorName", javaClass.name)
        st.add("date", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()))

        val code = st.render()

        val resource = filer.createResource(
            Location.SOURCE_OUTPUT,
            this.basePackageName,
            "Request.java"
        )

        resource.writeToOutputStream(code.toByteArray())
    }
}
