package io.github.potjerodekool.openapi.common.generate

import io.github.potjerodekool.codegen.template.adapter.EnumModelAdapter
import org.stringtemplate.v4.ST
import org.stringtemplate.v4.STGroupDir

class Templates {
    private val group = STGroupDir("codegen-templates")

    init {
        group.registerModelAdaptor(Enum::class.java, EnumModelAdapter())
    }

    fun getInstanceOf(name: String?): ST {
        return group.getInstanceOf(name)
    }
}
