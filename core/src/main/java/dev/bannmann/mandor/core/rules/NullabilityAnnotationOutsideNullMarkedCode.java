package dev.bannmann.mandor.core.rules;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;

public class NullabilityAnnotationOutsideNullMarkedCode extends SourceRule
{
    private static final Set<Class<? extends Annotation>> NULLABILITY_ANNOTATIONS = Set.of(NonNull.class,
        Nullable.class);

    private class Visitor extends VoidVisitorAdapter<Void>
    {
        @Override
        public void visit(MarkerAnnotationExpr markerAnnotation, Void arg)
        {
            process(markerAnnotation);
            super.visit(markerAnnotation, arg);
        }

        private void process(AnnotationExpr annotation)
        {
            if (annotationHasMismatchingSimpleName(annotation))
            {
                return;
            }

            var resolvedAnnotationDeclaration = annotation.resolve();
            if (annotationHasMismatchingType(resolvedAnnotationDeclaration))
            {
                return;
            }

            if (CodeNullness.isInNullMarkedClass(annotation) || CodeNullness.isInNullMarkedPackage(getContext()))
            {
                return;
            }

            addViolation("%s is not NullMarked but uses a jSpecify nullability annotation in %s",
                Nodes.getEnclosingTypeName(annotation),
                getContext().getCodeLocation(annotation));
        }

        private boolean annotationHasMismatchingSimpleName(AnnotationExpr annotation)
        {
            return NULLABILITY_ANNOTATIONS.stream()
                .map(Class::getSimpleName)
                .noneMatch(s -> s.equals(annotation.getNameAsString()));
        }

        private boolean annotationHasMismatchingType(ResolvedAnnotationDeclaration resolvedAnnotationDeclaration)
        {
            return NULLABILITY_ANNOTATIONS.stream()
                .map(Class::getName)
                .noneMatch(s -> s.equals(resolvedAnnotationDeclaration.getQualifiedName()));
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
        return "Classes using jSpecify nullability annotations need to be @NullMarked on package or class level";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
