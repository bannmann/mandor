package dev.bannmann.mandor.core;

import static dev.bannmann.mandor.core.SourceRule.Status.OPTIONAL;
import static dev.bannmann.mandor.core.SourceRule.Status.RECOMMENDED;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import com.github.mizool.core.MetaInfServices;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CheckReturnValue;
import dev.bannmann.mandor.core.SourceRule.Status;

@UtilityClass
public class SourceRuleProvider
{
    @CheckReturnValue
    public interface LoaderInitial
    {
        Loader recommendedFrom(String packageName);

        Loader recommendedAndOptionalFrom(String packageName);

        Loader customFrom(String packageName, Status first, Status... more);
    }

    public interface Loader extends LoaderInitial
    {
        /**
         * Gets the matching source rules as a stream.
         *
         * @return a stream of source rule instances. The instances are not shared across multiple calls.
         *
         * @throws NoSuchElementException if the classpath contains no rules matching the given criteria
         */
        Stream<SourceRule> asStream();

        /**
         * Gets the matching source rules as a list.
         *
         * @return an immutable list of source rule instances, not shared across multiple calls
         *
         * @throws NoSuchElementException if the classpath contains no rules matching the given criteria
         */
        List<SourceRule> asList();

        /**
         * Gets the matching source rules as an object array suitable for a TestNG data provider method.
         *
         * @return a two-dimensional array of source rule instances, not shared across multiple calls.
         *
         * @throws NoSuchElementException if the classpath contains no rules matching the given criteria
         */
        Object[][] asDataProvider();
    }

    @NoArgsConstructor
    private static final class LoaderImpl implements Loader
    {
        private record Selection(String packageName, ImmutableSet<Status> statusFilter)
        {
            public boolean match(SourceRule rule)
            {
                return statusFilter.contains(rule.getStatus()) &&
                       rule.getClass()
                           .getPackageName()
                           .equals(packageName);
            }
        }

        private static final Comparator<SourceRule>
            COMPARATOR
            = Comparator.comparing(sourceRule -> sourceRule.getClass()
            .getName());

        private final Set<Selection> selections = new HashSet<>();

        @Override
        public Loader recommendedFrom(String packageName)
        {
            return loadInternal(packageName, ImmutableSet.of(RECOMMENDED));
        }

        private Loader loadInternal(@NonNull String packageName, ImmutableSet<Status> statusFilter)
        {
            selections.add(new Selection(packageName, statusFilter));
            return this;
        }

        @Override
        public Loader recommendedAndOptionalFrom(String packageName)
        {
            return loadInternal(packageName, ImmutableSet.copyOf(EnumSet.of(RECOMMENDED, OPTIONAL)));
        }

        @Override
        public Loader customFrom(String packageName, Status first, Status... rest)
        {
            return loadInternal(packageName, ImmutableSet.copyOf(EnumSet.of(first, rest)));
        }

        @Override
        public Stream<SourceRule> asStream()
        {
            return asList().stream();
        }

        private boolean applySelections(SourceRule sourceRule)
        {
            return selections.stream()
                .anyMatch(selection -> selection.match(sourceRule));
        }

        @Override
        public List<SourceRule> asList()
        {
            List<SourceRule> result = StreamSupport.stream(MetaInfServices.instances(SourceRule.class)
                    .spliterator(), false)
                .filter(this::applySelections)
                .sorted(COMPARATOR)
                .toList();
            if (result.isEmpty())
            {
                throw new NoSuchElementException("No matching rules found");
            }
            return result;
        }

        @Override
        public Object[][] asDataProvider()
        {
            List<SourceRule> list = asList();
            Object[][] result = new Object[list.size()][];
            for (int i = 0; i < list.size(); i++)
            {
                result[i] = new Object[]{ list.get(i) };
            }
            return result;
        }
    }

    public static LoaderInitial load()
    {
        return new LoaderImpl();
    }
}
