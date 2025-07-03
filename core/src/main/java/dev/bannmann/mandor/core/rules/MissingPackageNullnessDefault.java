package dev.bannmann.mandor.core.rules;

import java.util.HashSet;
import java.util.Set;

import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.google.errorprone.annotations.Keep;
import dev.bannmann.labs.annotations.ImplementationNote;
import dev.bannmann.mandor.core.SourceRule;

@MetaInfServices
public class MissingPackageNullnessDefault extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        private final Set<String> knownPackages = new HashSet<>();

        @Override
        public void visit(PackageDeclaration node, Void arg)
        {
            super.visit(node, arg);

            String packageName = node.getNameAsString();

            if (knownPackages.contains(packageName))
            {
                return;
            }
            knownPackages.add(packageName);

            if (getContext().getPackageInfoFiles()
                .flatMap(packageDeclaration -> packageDeclaration.getAnnotations()
                    .stream())
                .filter(annotationExpr -> {
                    String name = annotationExpr.getNameAsString();
                    return name.equals("NullMarked") || name.equals("NullUnmarked");
                })
                .map(AnnotationExpr::resolve)
                .map(ResolvedTypeDeclaration::getPackageName)
                .noneMatch(annotationPackage -> annotationPackage.equals("org.jspecify.annotations")))
            {
                addViolation("Package %s is not annotated with @NullMarked or @NullUnmarked", packageName);
            }
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
        return "Packages should explicitly declare nullness defaults";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }

    @Override
    @Keep
    @ImplementationNote("Removing this method could cause the reader to not notice that this rule is optional")
    public Status getStatus()
    {
        return Status.OPTIONAL;
    }
}
