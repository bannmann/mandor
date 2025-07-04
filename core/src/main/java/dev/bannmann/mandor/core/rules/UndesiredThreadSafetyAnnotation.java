package dev.bannmann.mandor.core.rules;

import static com.github.javaparser.StaticJavaParser.parseName;

import java.util.Set;

import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithIdentifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.mizool.core.exception.CodeInconsistencyException;
import com.google.errorprone.annotations.Keep;
import dev.bannmann.labs.annotations.ImplementationNote;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;

@MetaInfServices
public final class UndesiredThreadSafetyAnnotation extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        /**
         * Alternatives:
         * <ul>
         *     <li>{@code javax.annotation.concurrent.NotThreadSafe} - contained in both findbugs:jsr305 and checkerframework. Stems from JSR 305 which is dormant.</li>
         * </ul>
         */
        private static final Name NCIP_NOT_THREADSAFE = parseName("net.jcip.annotations.NotThreadSafe");

        /**
         * Alternatives:
         * <ul>
         *     <li>{@code net.jcip.annotations.ThreadSafe} - documentation only, not supported by Error Prone check</li>
         * </ul>
         */
        private static final Name ERROR_PRONE_THREADSAFE = parseName("com.google.errorprone.annotations.ThreadSafe");

        private static final Set<Name> ALLOWED_ANNOTATIONS = Set.of(ERROR_PRONE_THREADSAFE, NCIP_NOT_THREADSAFE);

        @Override
        public void visit(MarkerAnnotationExpr markerAnnotation, Void arg)
        {
            process(markerAnnotation);
            super.visit(markerAnnotation, arg);
        }

        private void process(AnnotationExpr annotation)
        {
            if (!isThreadSafetyRelated(annotation))
            {
                return;
            }

            Name usedAnnotationName = parseName(Nodes.getQualifiedName(annotation, getContext()));
            if (ALLOWED_ANNOTATIONS.contains(usedAnnotationName))
            {
                return;
            }

            Name desiredAnnotationName = ALLOWED_ANNOTATIONS.stream()
                .filter(name -> name.getIdentifier()
                    .equals(usedAnnotationName.getIdentifier()))
                .findAny()
                .orElseThrow(() -> new CodeInconsistencyException("Could not identify desired annotation for " +
                                                                  usedAnnotationName.getIdentifier()));

            addViolation("%s should use annotation %s instead of %s in %s",
                Nodes.obtainEnclosingTopLevelTypeName(annotation),
                desiredAnnotationName,
                usedAnnotationName,
                getContext().getCodeLocation(annotation));
        }

        /**
         * Determines whether the unqualified name of the annotation matches one of the thread safety annotations.
         * <p>
         * We need to handle both qualified and unqualified (imported) annotation names.
         */
        private boolean isThreadSafetyRelated(AnnotationExpr annotation)
        {
            String annotationIdentifier = annotation.getName()
                .getIdentifier();
            return ALLOWED_ANNOTATIONS.stream()
                .map(NodeWithIdentifier::getId)
                .anyMatch(identifier -> identifier.equals(annotationIdentifier));
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
        return "Code should use one set of thread safety annotations (@ThreadSafe from Error Prone and @NotThreadSafe from net.jcip)";
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
