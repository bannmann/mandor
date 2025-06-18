package dev.bannmann.mandor.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

public class SourceBundle
{
    /**
     * All compilation units (classes, package-info) keyed to the path relative to the root directory (e.g. {@code com/example/package-info.java} or {@code com/example/Foo.java}).
     */
    private final ListMultimap<Path, CompilationUnit> compilationUnits = MultimapBuilder.linkedHashKeys()
        .arrayListValues()
        .build();

    public SourceBundle()
    {
        StaticJavaParser.getParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
            .setSymbolResolver(new JavaSymbolSolver(new NameOnlySolver()));
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

    private SourceBundle importSources(Path start, BiPredicate<Path, BasicFileAttributes> biPredicate)
    {
        try
        {
            try (Stream<Path> pathStream = Files.find(start, Integer.MAX_VALUE, biPredicate))
            {
                pathStream.forEach(path -> {
                    var compilationUnit = parse(path);
                    Path pathRelativeToStart = start.relativize(path);
                    compilationUnits.put(pathRelativeToStart, compilationUnit);
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
            return StaticJavaParser.parse(path);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

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
        for (Map.Entry<Path, CompilationUnit> entry : compilationUnits.entries())
        {
            var compilationUnit = entry.getValue();
            var path = entry.getKey();
            Context context = new Context(compilationUnit, path, this::lookupCompilationUnit);

            rule.scan(compilationUnit, context);
        }

        return rule.getViolations();
    }

    private Stream<CompilationUnit> lookupCompilationUnit(Path path)
    {
        return compilationUnits.get(path)
            .stream();
    }
}
