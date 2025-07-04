package com.example.libraries;

@javax.annotation.concurrent.NotThreadSafe
public final class SimpleCounter2
{
    private int value;

    public int increase()
    {
        return ++value;
    }
}
