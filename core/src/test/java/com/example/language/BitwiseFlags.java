package com.example.language;

public class BitwiseFlags
{
    private static final int VISIBLE = 0b0001;
    private static final int ACTIVE = 0b0010;
    private static final int PUBLISHED = 0b0100;
    private static final int DEPRECATED = 0b1000;

    public static void main(String[] args)
    {
        int example1 = VISIBLE | ACTIVE;
        int example2 = DEPRECATED;

        example1 |= PUBLISHED;
        example2 |= PUBLISHED;

        System.out.println("example1: " + example1);
        System.out.println("example2: " + example2);

        for (String arg : args)
        {
            System.out.println();

            int input = Integer.parseInt(arg);
            System.out.printf("flags active in %d:%n", input);
            printFlagIfSet(input, VISIBLE, "visible");
            printFlagIfSet(input, ACTIVE, "active");
            printFlagIfSet(input, PUBLISHED, "published");
            printFlagIfSet(input, DEPRECATED, "deprecated");
        }
    }

    private static void printFlagIfSet(int input, int flagValue, String flagName)
    {
        if ((input & flagValue) == flagValue)
        {
            System.out.println("    " + flagName);
        }
    }
}
