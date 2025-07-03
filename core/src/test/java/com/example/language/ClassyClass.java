package com.example.language;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mizool.core.Identifier;

@RequiredArgsConstructor
public class ClassyClass
{
    // No violation of HardcodedClassReference due to static context
    private static final Class<ClassyClass> CLASS = ClassyClass.class;

    public static Identifier<ClassyClass> getRandomIdentifier()
    {
        return Identifier.forPojo(ClassyClass.class) // No violation of HardcodedClassReference due to static context
            .random();
    }

    private final Logger log = LoggerFactory.getLogger(ClassyClass.class); // Violation: HardcodedClassReference
    private final String name;

    public Identifier<ClassyClass> getNameAsIdentifier()
    {
        return Identifier.forPojo(ClassyClass.class) // Violation: HardcodedClassReference
            .of(name);
    }

    public enum GreekLetter
    {
        ALPHA,
        BETA;

        public static Set<Identifier<GreekLetter>> getAllIdentifiers()
        {
            return Arrays.stream(values())
                .map(value -> Identifier.forPojo(GreekLetter.class) // No violation of HardcodedClassReference due to static context
                    .of(value.name()))
                .collect(Collectors.toSet());
        }

        public Identifier<GreekLetter> getNameAsIdentifier()
        {
            return Identifier.forPojo(GreekLetter.class) // Violation: HardcodedClassReference
                .of(name());
        }
    }
}
