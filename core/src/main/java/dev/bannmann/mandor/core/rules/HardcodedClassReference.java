package dev.bannmann.mandor.core.rules;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;
import dev.bannmann.mandor.core.UnprocessableSourceCodeException;

@MetaInfServices
public class HardcodedClassReference extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        @Override
        public void visit(ClassExpr node, Void arg)
        {
            super.visit(node, arg);

            Optional<NodeWithStaticModifier<?>> potentiallyStaticAncestor = getPotentiallyStaticAncestor(node);
            if (potentiallyStaticAncestor.isEmpty() ||
                potentiallyStaticAncestor.get()
                    .isStatic())
            {
                return;
            }

            Optional<TypeDeclaration<?>> enclosingTypeOptional = Nodes.findAncestor(node, TypeDeclaration.class)
                .map(typeDeclaration -> (TypeDeclaration<?>) typeDeclaration);
            if (enclosingTypeOptional.isEmpty())
            {
                return;
            }

            TypeDeclaration<?> enclosingType = enclosingTypeOptional.get();
            String enclosingTypeName = enclosingType.getFullyQualifiedName()
                .orElseThrow(() -> new UnprocessableSourceCodeException("Unsupported type declaration at %s".formatted(
                    getContext().getCodeLocation(enclosingType))));

            // We first tried `node.getType().resolve()`, but that broke on a `Bar.Quux.class` literal in class `Foo`.
            // In that case, `targtTypeShortName` will be "Bar.Quux". So we use a simpler check for now.
            String targetTypeShortName = node.getTypeAsString();
            if (enclosingTypeName.endsWith("." + targetTypeShortName))
            {
                addViolation("%s refers to itself using a class literal in %s",
                    enclosingTypeName,
                    getContext().getCodeLocation(node));
            }
        }

        private @NonNull Optional<NodeWithStaticModifier<?>> getPotentiallyStaticAncestor(ClassExpr node)
        {
            return Nodes.findAncestor(node, NodeWithStaticModifier.class)
                .map(ancestor -> (NodeWithStaticModifier<?>) ancestor);
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
        return "Classes should use getClass() to refer to themselves instead of hardcoded class literals";
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
