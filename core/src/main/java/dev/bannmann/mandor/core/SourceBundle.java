package dev.bannmann.mandor.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.mizool.core.exception.CodeInconsistencyException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

public class SourceBundle
{
    /**
     * All compilation units (classes, package-info) keyed to the path relative to their respective root directory (e.g.
     * {@code com/example/package-info.java} or {@code com/example/Foo.java}).
     */
    private final ListMultimap<Path, CompilationUnit> compilationUnits = MultimapBuilder.linkedHashKeys()
        .arrayListValues()
        .build();
    private final CombinedTypeSolver sourceBasedTypeSolver;
    private final ParserConfiguration parserConfiguration;
    private final JavaParserAdapter javaParserAdapter;
    private final RuleContext ruleContext;

    public SourceBundle()
    {
        sourceBasedTypeSolver = new CombinedTypeSolver();

        CombinedTypeSolver typeSolverWithFallback = new CombinedTypeSolver();
        typeSolverWithFallback.add(sourceBasedTypeSolver);
        typeSolverWithFallback.add(new ReflectionTypeSolver(false));

        parserConfiguration = new ParserConfiguration();
        parserConfiguration
            .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
            .setSymbolResolver(new JavaSymbolSolver(typeSolverWithFallback));

        var javaParser = new JavaParser(parserConfiguration);
        javaParserAdapter = new JavaParserAdapter(javaParser);

        ruleContext = new RuleContext(typeSolverWithFallback, this::lookupCompilationUnit);
    }

    public SourceBundle importSources(String directory)
    {
        return importSources(Paths.get(directory));
    }

    public SourceBundle importSources(Path start)
    {
        return importSources(start, this::isJavaSourceFile);
    }

    public SourceBundle importSources(Path start, Predicate<Path> filePredicate)
    {
        return importSources(start, combine(this::isJavaSourceFile, filePredicate));
    }

    private <T, U> BiPredicate<T, U> combine(BiPredicate<T, U> biPredicate, Predicate<T> predicate)
    {
        return biPredicate.and((t, u) -> predicate.test(t));
    }

    private SourceBundle importSources(Path path, BiPredicate<Path, BasicFileAttributes> biPredicate)
    {
        Path start = path.toAbsolutePath();
        sourceBasedTypeSolver.add(new JavaParserTypeSolver(start, parserConfiguration));

        try
        {
            try (Stream<Path> pathStream = Files.find(start, Integer.MAX_VALUE, biPredicate))
            {
                pathStream.map(this::parse)
                    .forEach(compilationUnit -> {
                        Path absolutePath = compilationUnit.getStorage()
                            .orElseThrow(CodeInconsistencyException::new)
                            .getPath();
                        Path relativePath = start.relativize(absolutePath);
                        compilationUnits.put(relativePath, compilationUnit);
                    });
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        return this;
    }

    private boolean isJavaSourceFile(Path filePath, BasicFileAttributes fileAttr)
    {
        return fileAttr.isRegularFile() &&
               filePath.getFileName()
                   .toString()
                   .endsWith(".java");
    }

    private CompilationUnit parse(Path path)
    {
        try
        {
            return parseFile(path);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private CompilationUnit parseFile(Path path) throws IOException
    {
        return javaParserAdapter.parse(path);
    }

    /**
     * @throws AssertionError if the rule discovered violations
     * @throws UnprocessableSourceCodeException if the rule encountered unexpected or unsupported source code constructs
     */
    public SourceBundle verify(SourceRule rule)
    {
        var violations = runScan(rule);
        if (!violations.isEmpty())
        {
            throw new AssertionError("Rule '%s' was violated (%d times):%n%s".formatted(rule.getDescription(),
                violations.size(),
                String.join("\n", violations)));
        }

        return this;
    }

    @VisibleForTesting
    List<String> runScan(SourceRule rule)
    {
        rule.init(ruleContext);

        compilationUnits.entries()
            .forEach(entry -> scanFile(rule, entry.getValue(), entry.getKey()));

        return rule.getViolations();
    }

    private void scanFile(SourceRule rule, CompilationUnit compilationUnit, Path relativePath)
    {
        // Tell the context about the new compilation unit so that its helper methods work.
        ruleContext.activate(compilationUnit, relativePath);

        // Initiate the scan itself
        try
        {
            rule.scan(compilationUnit);
        }
        catch (RuntimeException e)
        {
            throw new UnprocessableSourceCodeException("Rule failed to process " + ruleContext.getFilePath(), e);
        }
    }

    private Stream<CompilationUnit> lookupCompilationUnit(Path path)
    {
        return compilationUnits.get(path)
            .stream();
    }
}
