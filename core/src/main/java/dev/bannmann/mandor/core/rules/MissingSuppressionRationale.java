package dev.bannmann.mandor.core.rules;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.mizool.core.exception.CodeInconsistencyException;
import com.google.common.collect.Sets;
import dev.bannmann.labs.core.StreamExtras;
import dev.bannmann.mandor.core.AbstractSourceVisitor;
import dev.bannmann.mandor.core.Context;
import dev.bannmann.mandor.core.SourceRule;

public class MissingSuppressionRationale extends SourceRule
{
    private static class Visitor extends AbstractSourceVisitor
    {
        @Override
        public void visit(SingleMemberAnnotationExpr annotation, Context context)
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
                    throw new IllegalArgumentException(createExceptionMessage(memberValue));
                }
            }

            super.visit(annotation, context);
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

            TreeSet<String> suppressedWithoutRationale = new TreeSet<>(Sets.difference(suppressedNames,
                rationaleNames));
            if (suppressedWithoutRationale.isEmpty())
            {
                return;
            }

            String what = suppressedWithoutRationale.size() > 1
                ? "warnings"
                : "warning";

            addViolation("%s suppresses %s %s without giving rationale in %s",
                getContext().getEnclosingTypeName(suppressionAnnotation),
                what,
                suppressedWithoutRationale.stream()
                    .collect(Collectors.joining("', '", "'", "'")),
                getContext().getFileLocation(suppressionAnnotation));
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

            throw new CodeInconsistencyException("Unexpected type of annotation expression (%s): %s".formatted(
                annotationExpression.getClass()
                    .getSimpleName(),
                annotationExpression));
        }

        private String readStringValue(Expression expression)
        {
            if (expression instanceof StringLiteralExpr stringLiteralExpr)
            {
                return stringLiteralExpr.asString();
            }

            throw new IllegalArgumentException(createExceptionMessage(expression));
        }

        private void verifyRationalePresent(SingleMemberAnnotationExpr annotation, ArrayInitializerExpr array)
        {
            verifyRationalePresent(annotation,
                array.getValues()
                    .stream()
                    .map(arrayValueExpression -> {
                        if (arrayValueExpression instanceof StringLiteralExpr stringExpression)
                        {
                            return stringExpression.asString();
                        }

                        throw new IllegalArgumentException(createExceptionMessage(arrayValueExpression));
                    })
                    .toArray(String[]::new));
        }

        private String createExceptionMessage(Expression expression)
        {
            return "Unsupported expression type for @SuppressWarnings: %s in %s".formatted(expression,
                getContext().getFileLocation(expression));
        }
    }

    private final Visitor visitor = new Visitor();

    @Override
    protected AbstractSourceVisitor getVisitor()
    {
        return visitor;
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
}
