package dev.bannmann.mandor.core;

import static dev.bannmann.labs.core.Nullness.guaranteeNonNull;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.mizool.core.exception.CodeInconsistencyException;
import com.github.mizool.core.exception.ConfigurationException;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class Context
{
    @Getter
    private final CompilationUnit compilationUnit;

    @Getter
    private final Path filePath;

    private final Function<Path, Stream<CompilationUnit>> lookup;

    public String getEnclosingTypeName(Node startingNode)
    {
        String topmostTypeName = null;
        Node node = startingNode;
        while (node != null)
        {
            if (node instanceof TypeDeclaration<?> typeDeclaration)
            {
                topmostTypeName = typeDeclaration.getFullyQualifiedName()
                    .orElse(topmostTypeName);
            }
            node = node.getParentNode()
                .orElse(null);
        }

        if (topmostTypeName == null)
        {
            throw new IllegalArgumentException("%s doesn't seem to have any enclosing type".formatted(getFilePath()));
        }

        return topmostTypeName;
    }

    public String getFileLocation(Node startingNode)
    {
        return "(%s:%s)".formatted(getFilePath().getFileName(),
            startingNode.getRange()
                .map(range -> range.begin.line)
                .orElseThrow(CodeInconsistencyException::new));
    }

    public ResolvedAnnotationDeclaration resolve(AnnotationExpr annotation)
    {
        try
        {
            return annotation.resolve();
        }
        catch (UnsolvedSymbolException e)
        {
            throw new ConfigurationException("Cannot resolve annotation %s used by %s in %s".formatted(annotation.getNameAsString(),
                getEnclosingTypeName(annotation),
                getFileLocation(annotation)), e);
        }
    }

    /**
     * @return the compilation units residing at the given relative path. May be empty, but never {@code null}.
     */
    public Stream<CompilationUnit> lookup(Path path)
    {
        return lookup.apply(path);
    }

    /**
     * @return the package-info files for the package of the current compilation unit. There may be as many as one per directory imported into the {@link SourceBundle}.
     */
    public Stream<PackageDeclaration> getPackageInfoFiles()
    {
        Path directoryPath = guaranteeNonNull(getFilePath().getParent());
        Path packageInfoPath = directoryPath.resolve("package-info.java");

        return lookup(packageInfoPath).flatMap(packageInfoUnit -> packageInfoUnit.getPackageDeclaration()
            .stream());
    }
}
