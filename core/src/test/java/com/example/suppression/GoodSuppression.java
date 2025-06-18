package com.example.suppression;

import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@SuppressWarnings("X:Omega")
@SuppressWarningsRationale("The Omega directive does not apply here.")
public class GoodSuppression
{
    @SuppressWarnings({ "X:Alpha", "X:Beta" })
    @SuppressWarningsRationale(name = "X:Alpha", value = "<insert justification here>")
    @SuppressWarningsRationale(name = "X:Beta", value = "Joe claims this is fine.")
    private static final String FOO = "Stuff";
}
