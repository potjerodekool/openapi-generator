package io.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface GenerateUtils {
    boolean isListType(Type type);

    @Nullable Expression getDefaultValue(Type type);
}
