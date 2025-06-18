package com.example.exhaustive_switch;

import dev.bannmann.mandor.annotations.ExhaustiveSwitch;

public class GoodSwitch
{
    enum SomeChoice
    {
        FOO,
        BAR
    }

    public static void main(String[] args)
    {
        var someChoice = SomeChoice.valueOf(args[0]);

        @ExhaustiveSwitch Runnable handler = switch (someChoice)
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
}
