package dev.bannmann.mandor.core.rules;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.mizool.core.exception.CodeInconsistencyException;
import com.google.common.collect.Sets;
import dev.bannmann.labs.core.StreamExtras;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;
import dev.bannmann.mandor.core.UnprocessableSourceCodeException;

@MetaInfServices
public final class MissingSuppressionRationale extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        @Override
        public void visit(SingleMemberAnnotationExpr annotation, Void unused)
        {
            /*
             * Technically, a custom @SuppressWarnings might exist in another package, but we ignore that for now.
             *
             * The fully qualified name is unlikely to be used for types from java.lang, but as it's valid and easy
             * to check for, we do that.
             */
            if (annotation.getNameAsString()
                    .equals("SuppressWarnings") ||
                annotation.getNameAsString()
                    .equals("java.lang.SuppressWarnings"))
            {
                Expression memberValue = annotation.getMemberValue();
                if (memberValue instanceof StringLiteralExpr string)
                {
                    verifyRationalePresent(annotation, string);
                }
                else if (memberValue instanceof ArrayInitializerExpr array)
                {
                    verifyRationalePresent(annotation, array);
                }
                else
                {
                    throw new UnprocessableSourceCodeException(createExceptionMessage(memberValue));
                }
            }

            super.visit(annotation, unused);
        }

        private void verifyRationalePresent(SingleMemberAnnotationExpr annotation, StringLiteralExpr stringExpression)
        {
            verifyRationalePresent(annotation, stringExpression.asString());
        }

        private void verifyRationalePresent(SingleMemberAnnotationExpr suppressionAnnotation, String... names)
        {
            List<AnnotationExpr> rationaleAnnotations = suppressionAnnotation.getParentNode()
                .orElseThrow(CodeInconsistencyException::new)
                .getChildNodes()
                .stream()
                .filter(AnnotationExpr.class::isInstance)
                .map(AnnotationExpr.class::cast)
                .filter(annotationExpr -> annotationExpr.getNameAsString()
                    .equals("SuppressWarningsRationale"))
                .toList();

            Set<String> suppressedNames = Set.of(names);
            if (suppressedNames.size() == 1 &&
                rationaleAnnotations.size() == 1 &&
                getName(rationaleAnnotations.get(0)).isEmpty())
            {
                // Suppressing a single warning and having one SuppressWarningsRationale without an explicit name is fine.
                return;
            }

            Set<String> rationaleNames = rationaleAnnotations.stream()
                .map(this::getName)
                .flatMap(Optional::stream)
                .map(this::readStringValue)
                .collect(Collectors.toSet());

            Sets.SetView<String> suppressedWithoutRationale = Sets.difference(suppressedNames, rationaleNames);
            if (suppressedWithoutRationale.isEmpty())
            {
                return;
            }

            String what = suppressedWithoutRationale.size() > 1
                ? "warnings"
                : "warning";

            addViolation("%s suppresses %s %s without giving rationale in %s",
                Nodes.obtainEnclosingTopLevelTypeName(suppressionAnnotation),
                what,
                suppressedWithoutRationale.stream()
                    .sorted()
                    .collect(Collectors.joining("', '", "'", "'")),
                getContext().getCodeLocation(suppressionAnnotation));
        }

        private Optional<Expression> getName(AnnotationExpr annotationExpression)
        {
            if (annotationExpression instanceof SingleMemberAnnotationExpr)
            {
                return Optional.empty();
            }

            if (annotationExpression instanceof NormalAnnotationExpr normalAnnotationExpression)
            {
                return normalAnnotationExpression.getPairs()
                    .stream()
                    .filter(memberValuePair -> memberValuePair.getName()
                        .asString()
                        .equals("name"))
                    .reduce(StreamExtras.atMostOne())
                    .stream()
                    .findFirst()
                    .map(MemberValuePair::getValue);
            }

            throw new UnprocessableSourceCodeException("Unexpected type of annotation expression (%s): %s".formatted(
                annotationExpression.getClass()
                    .getSimpleName(),
                annotationExpression));
        }

        private String readStringValue(Expression expression)
        {
            if (!(expression instanceof StringLiteralExpr stringLiteralExpr))
            {
                throw new UnprocessableSourceCodeException(createExceptionMessage(expression));
            }

            return stringLiteralExpr.asString();
        }

        private void verifyRationalePresent(SingleMemberAnnotationExpr annotation, ArrayInitializerExpr array)
        {
            verifyRationalePresent(annotation,
                array.getValues()
                    .stream()
                    .map(arrayValueExpression -> {
                        if (!(arrayValueExpression instanceof StringLiteralExpr stringExpression))
                        {
                            throw new IllegalArgumentException(createExceptionMessage(arrayValueExpression));
                        }

                        return stringExpression.asString();
                    })
                    .toArray(String[]::new));
        }

        private String createExceptionMessage(Expression expression)
        {
            return "Unsupported expression type for @SuppressWarnings: %s in %s".formatted(expression,
                getContext().getCodeLocation(expression));
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg)
        {
            trackSuppressibleScope(n, () -> super.visit(n, arg));
        }

        @Override
        public void visit(FieldDeclaration n, Void arg)
        {
            trackSuppressibleScope(n, () -> super.visit(n, arg));
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg)
        {
            trackSuppressibleScope(n, () -> super.visit(n, arg));
        }

        @Override
        public void visit(MethodDeclaration n, Void arg)
        {
            trackSuppressibleScope(n, () -> super.visit(n, arg));
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
        return "Each @SuppressWarnings must have a corresponding @SuppressWarningsRationale";
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
