package com.example.language;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

public class BooleanLogic
{
    private static final Set<IntPredicate> PREDICATES = Set.of(value -> value % 7 == 0,
        value -> value > 100 && value < 200,
        value -> value < 0);

    public static void main(String[] args)
    {
        boolean fooCommandActive = args[0].equals("fooCommand");
        fooCommandActive ^= args.length < 3;

        if (fooCommandActive)
        {
            System.out.println("Foo command activated");
            return;
        }

        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        if (a % 2 == 0 ^ b > 50)
        {
            System.out.println("Primary criteria met by exactly one number");
            return;
        }

        Map<Integer, Boolean> predicateResults = new HashMap<>();
        for (String arg : args)
        {
            int integer = Integer.parseInt(arg);
            Boolean result = predicateResults.get(integer);
            for (IntPredicate predicate : PREDICATES)
            {
                result ^= predicate.test(integer);
            }
            predicateResults.put(integer, result);
        }
        predicateResults.entrySet()
            .stream()
            .filter(Map.Entry::getValue)
            .forEach(entry -> System.out.printf("Number %d met exactly one secondary criterion%n", entry.getKey()));

        System.err.println("Unsupported combination of parameters");
    }
}
