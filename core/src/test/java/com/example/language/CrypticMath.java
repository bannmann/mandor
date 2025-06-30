package com.example.language;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CrypticMath
{
    public int divideByFour(int input)
    {
        return input >> 2;
    }

    public boolean isEven(int input)
    {
        return (input & 1) == 0;
    }
}
