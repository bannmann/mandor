package com.example.exhaustive_switch;

import dev.bannmann.mandor.annotations.ExhaustiveSwitch;

public class BadSwitch
{
    enum SomeChoice
    {
        FOO,
        BAR
    }

    public static void main(String[] args)
    {
        var someChoice = SomeChoice.valueOf(args[0]);

        new BadSwitch(someChoice);
        case1(someChoice);
        case2(someChoice);
    }

    private static void case1(SomeChoice someChoice)
    {
        // Violation: MalformedExhaustiveSwitch - next statement must call method on the variable
        @ExhaustiveSwitch Runnable handler = switch (someChoice)
        {
            case FOO -> () -> {
                System.out.println("yay!");
            };
            case BAR -> () -> {
                // nah, that's not good.
            };
        };

        System.out.println(handler);
    }

    private static void case2(SomeChoice someChoice)
    {
        // Violation: MalformedExhaustiveSwitch - declaration & definition must be combined
        @ExhaustiveSwitch Runnable handler;

        handler = switch (someChoice)
        {
            case FOO -> () -> {
                System.out.println("yay!");
            };
            case BAR -> () -> {
                // nah, that's not good.
            };
        };

        handler.run();
    }

    @SuppressWarnings("MalformedExhaustiveSwitch")
    private BadSwitch(SomeChoice someChoice)
    {
        // No violation of MalformedExhaustiveSwitch due to @SuppressWarnings above
        @ExhaustiveSwitch Runnable handler;
    }
}
