package com.example.language;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("BitwiseOperatorUsage") // Violation: needless suppression
public class ReadableMath
{
    public int divideByFour(int input)
    {
        return input / 4;
    }

    public boolean isEven(int input)
    {
        return input % 2 == 0;
    }
}
