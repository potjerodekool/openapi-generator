package io.github.potjerodekool.openapi;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import static io.github.potjerodekool.openapi.util.Utils.asGeneric;
import static io.github.potjerodekool.openapi.util.Utils.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 Organised the imports.
 Tries to reduce the use of qualified classname as much as possible to make the generated code more readable.
 */
public class ImportOrganiser extends GenericVisitorWithDefaults<Object, Object> {

    private static final Object DEFAULT_RETURN = new Object();

    private @Nullable CompilationUnit cu;

    @Override
    public Object visit(final CompilationUnit n, final Object arg) {
        this.cu = n;
        n.getTypes().accept(this, arg);
        this.cu = null;
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ClassOrInterfaceDeclaration n, final Object arg) {
        n.getMembers().forEach(member -> member.accept(this, arg));
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final FieldDeclaration n, final Object arg) {
        n.getAnnotations().accept(this, arg);

        n.getVariables().forEach(variable -> {
            final var type = variable.getType();
            Type newType = (Type) type.accept(this, arg);

            if (newType != type) {
                variable.setType(newType);
            }
        });

        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ConstructorDeclaration n, final Object arg) {
        n.getAnnotations().accept(this, arg);
        n.getParameters().accept(this, arg);
        n.getBody().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final MethodDeclaration n, final Object arg) {
        n.getAnnotations().accept(this, arg);
        n.getParameters().accept(this, arg);
        n.getBody().ifPresent(body -> body.accept(this, arg));
        final var returnType = n.getType();
        final var newReturnType = (Type) returnType.accept(this, arg);

        if (newReturnType != returnType) {
            n.setType(newReturnType);
        }

        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final Parameter n, final Object arg) {
        n.getAnnotations().accept(this, arg);

        final var type = n.getType();
        final var newType = (Type) type.accept(this, arg);

        if (newType != type) {
            n.setType(newType);
        }
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final BlockStmt n, final Object arg) {
        n.getStatements().forEach(statement -> statement.accept(this, arg));
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ReturnStmt n, final Object arg) {
        n.getExpression().ifPresent(expression -> expression.accept(this, arg));
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final AnnotationDeclaration n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final AnnotationMemberDeclaration n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ArrayAccessExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ArrayCreationExpr n, final Object arg) {
        n.getElementType().accept(this, arg);
        n.getInitializer().ifPresent(arrayInitializerExpr -> arrayInitializerExpr.getValues().accept(this, arg));
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ArrayInitializerExpr n, final Object arg) {
        n.getValues().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final AssertStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final AssignExpr n, final Object arg) {
        n.getValue().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final BinaryExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final BooleanLiteralExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final BreakStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final CastExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final CatchClause n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final CharLiteralExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ClassExpr n, final Object arg) {
        final var type = n.getType();
        final var newType = (Type) type.accept(this, arg);

        if (newType != type) {
            n.setType(newType);
        }

        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ConditionalExpr n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final ContinueStmt n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final DoStmt n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final DoubleLiteralExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final EmptyStmt n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final EnclosedExpr n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final EnumConstantDeclaration n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final EnumDeclaration n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final ExplicitConstructorInvocationStmt n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final ExpressionStmt n, final Object arg) {
        n.getExpression().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final FieldAccessExpr n, final Object arg) {
        n.getTypeArguments().ifPresent(typeArguments -> typeArguments.forEach(typeArgument -> typeArgument.accept(this, arg)));
        n.getScope().accept(this, arg);
        n.getName().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ForEachStmt n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final ForStmt n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final IfStmt n, final Object arg) {
        n.getCondition().accept(this, arg);
        final var thenStmt = n.getThenStmt();

        if (thenStmt != null) {
            thenStmt.accept(this, arg);
        }

        n.getElseStmt().ifPresent(elseStmt -> elseStmt.accept(this, arg));
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final InitializerDeclaration n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final InstanceOfExpr n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final IntegerLiteralExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final JavadocComment n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final LabeledStmt n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final LongLiteralExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final MarkerAnnotationExpr n, final Object arg) {
        final var name = n.getNameAsString();

        if (importClass(name)) {
            n.setName(resolveSimpleClassName(name));
        }

        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final MemberValuePair n, final Object arg) {
        n.getValue().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final MethodCallExpr n, final Object arg) {
        n.getScope().ifPresent(scope -> scope.accept(this, arg));
        n.getTypeArguments().ifPresent(typeArguments -> typeArguments.forEach(typeArgument -> typeArgument.accept(this, arg)));
        n.getArguments().forEach(argument -> argument.accept(this, arg));
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final NameExpr n, final Object arg) {
        final var name = n.getNameAsString();

        if (name.contains(".")) {
            importClass(name);
            n.setName(resolveSimpleClassName(name));
        }

        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final NormalAnnotationExpr n, final Object arg) {
        final var name = n.getNameAsString();

        if (importClass(name)) {
            n.setName(resolveSimpleClassName(name));
        }

        n.getPairs().accept(this, arg);

        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final NullLiteralExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ObjectCreationExpr n, final Object arg) {
        final var type = n.getType();
        final var newType = (ClassOrInterfaceType) type.accept(this, arg);

        if (newType != type) {
            n.setType(newType);
        }

        n.getArguments().accept(this, arg);

        return n;
    }

    @Override
    public Object visit(final PackageDeclaration n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final PrimitiveType n, final Object arg) {
        return n;
    }

    @Override
    public Object visit(final Name n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final SimpleName n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ArrayType n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final ArrayCreationLevel n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final IntersectionType n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final UnionType n, final Object arg) {
        return super.visit(n, arg);
    }

    @Override
    public Object visit(final SingleMemberAnnotationExpr n, final Object arg) {
        final var name = n.getNameAsString();

        if (importClass(name)) {
            n.setName(resolveSimpleClassName(name));
        }

        n.getMemberValue().accept(this, arg);

        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final StringLiteralExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final SuperExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final SwitchEntry n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final SwitchStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final SynchronizedStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ThisExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ThrowStmt n, final Object arg) {
        n.getExpression().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final TryStmt n, final Object arg) {
        n.getTryBlock().accept(this, arg);
        n.getCatchClauses().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final LocalClassDeclarationStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final LocalRecordDeclarationStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final TypeParameter n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final UnaryExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final UnknownType n, final Object arg) {
        return n;
    }

    @Override
    public Object visit(final VariableDeclarationExpr n, final Object arg) {
        n.getVariables().forEach(variableDeclarator -> variableDeclarator.accept(this, arg));
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final VariableDeclarator n, final Object arg) {
        final var type = n.getType();
        final var newType = (Type) type.accept(this, arg);
        if (newType != type) {
            n.setType(newType);
        }

        n.getInitializer().ifPresent(initializer -> initializer.accept(this, arg));

        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final VoidType n, final Object arg) {
        return n;
    }

    @Override
    public Object visit(final WhileStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final WildcardType n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final LambdaExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final MethodReferenceExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final TypeExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ImportDeclaration n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final BlockComment n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final LineComment n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final NodeList n, final Object arg) {
        asGeneric(n).forEach(node -> node.accept(this, arg));
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ModuleDeclaration n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ModuleRequiresDirective n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ModuleExportsDirective n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ModuleProvidesDirective n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ModuleUsesDirective n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ModuleOpensDirective n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final UnparsableStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ReceiverParameter n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final VarType n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final Modifier n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final SwitchExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final YieldStmt n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final TextBlockLiteralExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final PatternExpr n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final RecordDeclaration n, final Object arg) {
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final CompactConstructorDeclaration n, final Object arg) {
        n.getAnnotations().accept(this, arg);
        n.getTypeParameters().accept(this, arg);
        return DEFAULT_RETURN;
    }

    @Override
    public Object visit(final ClassOrInterfaceType n, final Object arg) {
        var type = n;

        if (n.getScope().isPresent()) {
            final var qualifiedName = n.getNameWithScope();

            if (importClass(qualifiedName)) {
                final var simpleName = resolveSimpleClassName(qualifiedName);
                type = new ClassOrInterfaceType().setName(simpleName);
            }
        }

        final var typeArgumentOptional = n.getTypeArguments();

        if (typeArgumentOptional.isPresent()) {
            final var newTypeArguments = new NodeList<Type>();

            final var typeArguments = typeArgumentOptional.get();
            for (final Type typeArgument : typeArguments) {
                final var newTypeArgument = (Type) typeArgument.accept(this, arg);
                newTypeArguments.add(newTypeArgument);
            }
            type.setTypeArguments(newTypeArguments);
        }

        return type;
    }

    private String resolveSimpleClassName(final String className) {
        final var nameSep = className.lastIndexOf('.');
        return nameSep > -1 ? className.substring(nameSep + 1) : className;
    }

    private boolean importClass(final String className) {
        if (className.startsWith("java.lang.")) {
            return true;
        }

        final var simpleClassName = resolveSimpleClassName(className);

        final CompilationUnit currentCompilationUnit = cu;

        if (currentCompilationUnit != null) {
            final var imports = currentCompilationUnit.getImports();

            for (final var importDeclaration : imports) {
                if (importDeclaration.isAsterisk()) {
                    throw new UnsupportedOperationException("Process asterisk import");
                } else if (!importDeclaration.isStatic()) {
                    final var qualifiedImportName = importDeclaration.getNameAsString();

                    if (qualifiedImportName.equals(className)) {
                        return true;
                    } else if (importDeclaration.getNameAsString().equals(simpleClassName)) {
                        return false;
                    }
                }
            }

            currentCompilationUnit.addImport(className);
            return true;
        }
        return false;
    }

    @Override
    public Object defaultAction(final Node n, final Object arg) {
        throw new UnsupportedOperationException("" + n.getClass().getName());
    }

    @Override
    public Object defaultAction(final NodeList n, final Object arg) {
        asGeneric(n).forEach(e -> e.accept(this, arg));
        return DEFAULT_RETURN;
    }

}
