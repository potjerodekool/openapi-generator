package io.github.potjerodekool.openapi.common

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.openapi.common.dependency.ApplicationContext

class OpenApiEnvironment(
    val project: Project,
    val environment: Environment,
    val generatorConfig: GeneratorConfig,
    val applicationContext: ApplicationContext
)
