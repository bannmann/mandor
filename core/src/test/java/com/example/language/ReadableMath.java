package com.example.language;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("BitwiseOperatorUsage") // Violation: needless suppression
public class ReadableMath
{
    private static final int EIGHT = 010; // Violation: OctalNumberUsage

    @SuppressWarnings("OctalNumberUsage")
    private static final int NINE = 011; // No violation of OctalNumberUsage due to @SuppressWarnings above

    private static final int TEN = 10;

    @SuppressWarnings("OctalNumberUsage") // Violation: needless suppression
    public int divideByFour(int input)
    {
        return input / 4;
    }

    public boolean isEven(int input)
    {
        return input % 2 == 0;
    }

    public boolean hasDivisorEight(int input)
    {
        return input % EIGHT == 0;
    }

    public boolean hasDivisorNine(int input)
    {
        return input % NINE == 0;
    }

    public boolean hasDivisorTen(int input)
    {
        return input % TEN == 0;
    }
}
