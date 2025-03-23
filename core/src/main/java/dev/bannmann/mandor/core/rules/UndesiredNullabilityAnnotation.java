package dev.bannmann.mandor.core.rules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.mizool.core.exception.ConfigurationException;
import dev.bannmann.mandor.core.AbstractSourceVisitor;
import dev.bannmann.mandor.core.SourceRule;

@SuppressWarnings("VoidUsed")
public class UndesiredNullabilityAnnotation extends SourceRule
{
    private static class Visitor extends AbstractSourceVisitor
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
            if (!ANNOTATION_NAMES.contains(annotation.getNameAsString()))
            {
                return;
            }

            String qualifiedName = getQualifiedName(annotation);
            if (ALLOWED_ANNOTATIONS.contains(qualifiedName))
            {
                return;
            }

            boolean isNullMarkedPackage = findAncestor(annotation,
                CompilationUnit.class).flatMap(CompilationUnit::getStorage)
                .map(CompilationUnit.Storage::getDirectory)
                .map(path -> path.resolve("package-info.java"))
                .map(path -> {
                    try
                    {
                        return StaticJavaParser.parse(path);
                    }
                    catch (IOException e)
                    {
                        throw new UncheckedIOException(e);
                    }
                })
                .flatMap(CompilationUnit::getPackageDeclaration)
                .flatMap(packageDeclaration -> packageDeclaration.getAnnotationByClass(NullMarked.class))
                .isPresent();

            if (!isNullMarkedPackage)
            {
                return;
            }

            addViolation("%s uses undesired annotation %s in %s",
                getEnclosingTypeName(annotation),
                qualifiedName,
                getFileLocation(annotation));
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
                throw new ConfigurationException("Cannot resolve qualified name for annotation %s used by %s in %s".formatted(
                    annotation.getNameAsString(),
                    getEnclosingTypeName(annotation),
                    getFileLocation(annotation)), e);
            }
        }

        @Override
        public void visit(SingleMemberAnnotationExpr singleMemberAnnotation, Void unused)
        {
            super.visit(singleMemberAnnotation, unused);
        }

        @Override
        public void visit(NormalAnnotationExpr normalAnnotation, Void arg)
        {
            super.visit(normalAnnotation, arg);
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
        return "@NullMarked packages may only use nullability annotations from jSpecify (and Lombok's @NonNull)";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
