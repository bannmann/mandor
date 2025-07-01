package dev.bannmann.mandor.core.rules;

import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;
import dev.bannmann.mandor.core.UnprocessableSourceCodeException;

public final class UndesiredNullabilityAnnotation extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        private static final Set<String> ANNOTATION_NAMES = Set.of("Nullable", "NotNull", "NonNull", "Nonnull");

        private static final Set<String> ALLOWED_ANNOTATIONS = Set.of("org.jspecify.annotations.NonNull",
            "org.jspecify.annotations.Nullable",
            "lombok.NonNull");

        @Override
        public void visit(MarkerAnnotationExpr markerAnnotation, Void arg)
        {
            process(markerAnnotation);
            super.visit(markerAnnotation, arg);
        }

        private void process(AnnotationExpr annotation)
        {
            if (!isRelatedToNullness(annotation))
            {
                return;
            }

            String qualifiedName = getQualifiedName(annotation);
            if (ALLOWED_ANNOTATIONS.contains(qualifiedName))
            {
                return;
            }

            if (isOutsideNullMarkedCode(annotation))
            {
                return;
            }

            addViolation("%s uses undesired annotation %s in %s",
                Nodes.getEnclosingTypeName(annotation),
                qualifiedName,
                getContext().getCodeLocation(annotation));
        }

        private boolean isRelatedToNullness(AnnotationExpr annotation)
        {
            return ANNOTATION_NAMES.contains(annotation.getNameAsString());
        }
        private String getQualifiedName(AnnotationExpr annotation)
        {
            try
            {
                return annotation.resolve()
                    .getQualifiedName();
            }
            catch (UnsolvedSymbolException e)
            {
                throw new UnprocessableSourceCodeException(
                    "Cannot resolve qualified name for annotation %s used by %s in %s".formatted(annotation.getNameAsString(),
                        Nodes.getEnclosingTypeName(annotation),
                        getContext().getCodeLocation(annotation)),
                    e);
            }
        }

        private boolean isOutsideNullMarkedCode(AnnotationExpr annotation)
        {
            return !isInsideNullMarkedCode(annotation);
        }

        private boolean isInsideNullMarkedCode(AnnotationExpr annotation)
        {
            return CodeNullness.isInNullMarkedClass(annotation) || CodeNullness.isInNullMarkedPackage(getContext());
        }

        @Override
        public void visit(SingleMemberAnnotationExpr singleMemberAnnotation, Void arg)
        {
            process(singleMemberAnnotation);
            super.visit(singleMemberAnnotation, arg);
        }

        @Override
        public void visit(NormalAnnotationExpr normalAnnotation, Void arg)
        {
            process(normalAnnotation);
            super.visit(normalAnnotation, arg);
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
        return "@NullMarked code may only use nullability annotations from jSpecify (and Lombok's @NonNull)";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
