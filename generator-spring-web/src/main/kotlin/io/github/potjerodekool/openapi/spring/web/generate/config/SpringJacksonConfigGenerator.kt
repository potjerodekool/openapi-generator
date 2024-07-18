package io.github.potjerodekool.openapi.spring.web.generate.config

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.codegen.Language
import io.github.potjerodekool.codegen.model.CompilationUnit
import io.github.potjerodekool.codegen.model.element.ElementKind
import io.github.potjerodekool.codegen.model.element.Modifier
import io.github.potjerodekool.codegen.model.element.Name
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration
import io.github.potjerodekool.codegen.model.tree.expression.*
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression
import io.github.potjerodekool.codegen.model.type.TypeKind
import io.github.potjerodekool.codegen.model.util.StringUtils
import io.github.potjerodekool.openapi.common.GeneratorConfig
import io.github.potjerodekool.openapi.common.dependency.Artifact
import io.github.potjerodekool.openapi.common.dependency.DependencyChecker
import io.github.potjerodekool.openapi.common.generate.config.ConfigGenerator
import io.github.potjerodekool.openapi.common.log.Logger.Companion.getLogger
import java.io.IOException
import java.util.*
import java.util.List
import java.util.function.Consumer
import java.util.jar.JarFile
import java.util.stream.Collectors
import kotlin.collections.HashSet
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.dropLastWhile
import kotlin.collections.toTypedArray

class SpringJacksonConfigGenerator(
    private val generatorConfig: GeneratorConfig,
    private val environment: Environment,
    dependencyChecker: DependencyChecker
) : ConfigGenerator {
    private val resolvedJaxsonModuleClasses: Set<String>

    init {
        this.resolvedJaxsonModuleClasses = resolveDependencies(dependencyChecker)
    }

    override fun generate() {
        if (skipGeneration()) {
            return
        }

        val cu = CompilationUnit(Language.JAVA)

        val configPackageName = generatorConfig.configPackageName()
        val packageDeclaration = PackageDeclaration(IdentifierExpression(configPackageName))
        cu.packageDeclaration(packageDeclaration)

        val classDeclaration = ClassDeclaration()
            .simpleName(Name.of(configClassName))
            .kind(ElementKind.CLASS)
            .modifier(Modifier.PUBLIC)
            .annotation(AnnotationExpression("org.springframework.context.annotation.Configuration"))
            .annotation(
                AnnotationExpression(
                    "javax.annotation.processing.Generated",
                    LiteralExpression.createStringLiteralExpression(javaClass.name)
                )
            )
        classDeclaration.enclosing = packageDeclaration

        cu.classDeclaration(classDeclaration)

        fillClass(classDeclaration)

        environment.compilationUnits.add(cu)
    }

    private val configClassName: String
        get() = "JacksonConfiguration"

    private fun fillClass(classDeclaration: ClassDeclaration) {
        resolvedJaxsonModuleClasses.forEach(Consumer { jaxsonModuleClassName: String ->
            if ("com.fasterxml.jackson.module.kotlin.KotlinModule" == jaxsonModuleClassName) {
                addKotlinModuleBeanMethod(classDeclaration, jaxsonModuleClassName)
            } else {
                addBeanMethod(classDeclaration, jaxsonModuleClassName)
            }
        })
    }

    private fun skipGeneration(): Boolean {
        return resolvedJaxsonModuleClasses.isEmpty()
    }

    private fun addBeanMethod(
        classDeclaration: ClassDeclaration,
        moduleClassName: String
    ) {
        val sepIndex = moduleClassName.lastIndexOf(".")
        val simpleName = if (sepIndex < 0) moduleClassName else moduleClassName.substring(sepIndex + 1)
        val methodName = StringUtils.firstLower(simpleName)

        classDeclaration.method { method: MethodDeclaration ->
            method.simpleName(Name.of(methodName))
            method.returnType(NoTypeExpression(TypeKind.VOID))
            method.modifier(Modifier.PUBLIC)

            method.annotation(AnnotationExpression("org.springframework.context.annotation.Bean"))
                .annotation(
                    AnnotationExpression(
                        "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean",
                        ArrayInitializerExpression(
                            LiteralExpression.createClassLiteralExpression(
                                ClassOrInterfaceTypeExpression(
                                    moduleClassName
                                )
                            )
                        )
                    )
                )

            method.body(
                BlockStatement(
                    ReturnStatement(
                        NewClassExpression(ClassOrInterfaceTypeExpression(moduleClassName))
                    )
                )
            )
            method.returnType(ClassOrInterfaceTypeExpression(moduleClassName))
        }
    }

    private fun addKotlinModuleBeanMethod(
        classDeclaration: ClassDeclaration,
        moduleClassName: String
    ) {
        val parameterNamesModuleType = ClassOrInterfaceTypeExpression(moduleClassName)

        val sepIndex = moduleClassName.lastIndexOf(".")
        val simpleName = if (sepIndex < 0) moduleClassName else moduleClassName.substring(sepIndex + 1)
        val methodName = StringUtils.firstLower(simpleName)

        classDeclaration.method { method: MethodDeclaration ->
            method.simpleName(Name.of(methodName))
            method.returnType(parameterNamesModuleType)
            method.modifier(Modifier.PUBLIC)
            method.annotation(AnnotationExpression("org.springframework.context.annotation.Bean"))
                .annotation(
                    AnnotationExpression(
                        "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean",
                        ArrayInitializerExpression(
                            LiteralExpression.createClassLiteralExpression(
                                ClassOrInterfaceTypeExpression(moduleClassName)
                            )
                        )
                    )
                )


            val buildCall = MethodCallExpression(
                MethodCallExpression(
                    NewClassExpression(ClassOrInterfaceTypeExpression("$moduleClassName.Builder")),
                    "configure",
                    List.of(
                        FieldAccessExpression(
                            ClassOrInterfaceTypeExpression("com.fasterxml.jackson.module.kotlin.KotlinFeature"),
                            "StrictNullChecks"
                        ),
                        LiteralExpression.createBooleanLiteralExpression(true)
                    )
                ),
                "build"
            )
            method.body(BlockStatement(ReturnStatement(buildCall)))
        }
    }

    companion object {
        private val LOGGER = getLogger(
            SpringJacksonConfigGenerator::class.java.name
        )

        private fun resolveDependencies(dependencyChecker: DependencyChecker): Set<String> {
            return dependencyChecker.projectArtifacts
                .flatMap { artifact -> processArtifact(artifact).stream() }
                .collect(Collectors.toSet())
        }

        private fun processArtifact(artifact: Artifact): Set<String> {
            val modules: MutableSet<String> = HashSet()
            val file = artifact.file

            if (file != null && file.isFile && file.name.lowercase(Locale.getDefault()).endsWith(".jar")) {
                try {
                    JarFile(file).use { jarFile ->
                        val serviceFile = jarFile.getJarEntry("META-INF/services/com.fasterxml.jackson.databind.Module")
                        if (serviceFile != null) {
                            LOGGER.info(String.format("Found jaxson databind modules in %s", file.absolutePath))
                            val inputStream = jarFile.getInputStream(serviceFile)
                            val moduleClassNames =
                                String(inputStream.readAllBytes()).split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            modules.addAll(Arrays.asList(*moduleClassNames))
                        }
                    }
                } catch (e: IOException) {
                    //Ignore exception
                }
            }

            return modules
        }
    }
}