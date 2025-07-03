package dev.bannmann.mandor.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestOctalDetector
{
    @DataProvider
    protected Object[][] cases()
    {
        return new Object[][]{
            new Object[]{ "01", true },
            new Object[]{ "07", true },
            new Object[]{ "017", true },
            new Object[]{ "0", false },
            new Object[]{ "00", false },
            new Object[]{ "00000", false },
            new Object[]{ "00_000", false },
            new Object[]{ "45", false },
            new Object[]{ "1337", false },
            new Object[]{ "37_272_384_929", false },
            new Object[]{ "27l", false },
            new Object[]{ "27L", false }
        };
    }

    @Test(dataProvider = "cases")
    public void test(String value, boolean expectedResult)
    {
        boolean actual = new OctalDetector().isOctal(value);
        assertThat(actual).isEqualTo(expectedResult);
    }
}
