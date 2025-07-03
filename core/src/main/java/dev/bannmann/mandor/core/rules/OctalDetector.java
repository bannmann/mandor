package dev.bannmann.mandor.core.rules;

import java.util.regex.Pattern;

class OctalDetector
{
    private static final Pattern OCTAL_PATTERN = Pattern.compile("0[1-9_][\\d_]*[lL]?");

    public boolean isOctal(String value)
    {
        return OCTAL_PATTERN.matcher(value)
            .matches();
    }
}
