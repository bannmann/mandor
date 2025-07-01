package dev.bannmann.mandor.core.rules;

import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;

@MetaInfServices
public class BitwiseOperatorUsage extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        @Override
        public void visit(BinaryExpr expression, Void arg)
        {
            super.visit(expression, arg);

            switch (expression.getOperator())
            {
                case BINARY_AND, BINARY_OR, LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> process(expression);
                case XOR ->
                {
                    if (!(isBoolean(expression.getLeft()) && isBoolean(expression.getRight())))
                    {
                        process(expression);
                    }
                }
                default ->
                {
                    // Other operators are fine, nothing to do.
                }
            }
        }

        private void process(Expression expression)
        {
            var node = expression.getParentNode()
                .orElseThrow(() -> new IllegalArgumentException("%s doesn't seem to have a parent node".formatted(
                    expression)));

            addViolation("%s uses bitwise operator in %s",
                Nodes.getEnclosingTypeName(node),
                getContext().getCodeLocation(node));
        }

        private boolean isBoolean(Expression expression)
        {
            var resolvedType = expression.calculateResolvedType();
            return isPrimitiveBoolean(resolvedType) || isReferenceBoolean(resolvedType);
        }

        private boolean isReferenceBoolean(ResolvedType resolvedType)
        {
            return resolvedType instanceof ResolvedReferenceType resolvedReferenceType &&
                   resolvedReferenceType.hasName() &&
                   resolvedReferenceType.getQualifiedName()
                       .equals(Boolean.class.getCanonicalName());
        }

        private boolean isPrimitiveBoolean(ResolvedType resolvedType)
        {
            return resolvedType instanceof ResolvedPrimitiveType resolvedPrimitiveType &&
                   resolvedPrimitiveType.isBoolean();
        }

        @Override
        public void visit(AssignExpr expression, Void arg)
        {
            super.visit(expression, arg);

            switch (expression.getOperator())
            {
                case BINARY_AND, BINARY_OR, LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> process(expression);
                case XOR ->
                {
                    if (!isBooleanAssignment(expression))
                    {
                        process(expression);
                    }
                }
                default ->
                {
                    // Other operators are fine, nothing to do.
                }
            }
        }

        private boolean isBooleanAssignment(AssignExpr expression)
        {
            NameExpr targetNameExpression = (NameExpr) expression.getTarget();
            JavaParserVariableDeclaration resolve = (JavaParserVariableDeclaration) targetNameExpression.resolve();
            var type = resolve.getVariableDeclarator()
                .getType();
            if (type.equals(PrimitiveType.booleanType()))
            {
                return true;
            }

            if (!(type instanceof ClassOrInterfaceType classOrInterfaceType))
            {
                return false;
            }

            return classOrInterfaceType.getNameAsString()
                .equals("Boolean");
        }
    }

    private final Visitor visitor = new Visitor();

    @Override
    protected void scan(CompilationUnit compilationUnit)
    {
        compilationUnit.accept(visitor, null);
    }

    @Override
    public String getDescription()
    {
        return "Bitwise operators should only be used in very rare, exceptional circumstances";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }

    @Override
    public Status getStatus()
    {
        return Status.RECOMMENDED;
    }
}
