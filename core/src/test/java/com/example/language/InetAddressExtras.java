package com.example.language;

import java.net.Inet4Address;
import java.net.InetAddress;

import lombok.experimental.UtilityClass;

import com.google.common.net.InetAddresses;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@UtilityClass
public class InetAddressExtras
{
    @SuppressWarnings("BitwiseOperatorUsage") // Violation: needless suppression
    public static boolean isRoutable(InetAddress inetAddress)
    {
        if (!(inetAddress instanceof Inet4Address ip))
        {
            throw new IllegalArgumentException();
        }
        return !ip.isLoopbackAddress() && !ip.isSiteLocalAddress() && !isCarrierGradeNat(ip);
    }

    /**
     * 100.64.0.0/10
     */
    @SuppressWarnings("BitwiseOperatorUsage")
    @SuppressWarningsRationale("Avoiding bitwise operators would make this code ridiculously more complex")
    private static boolean isCarrierGradeNat(Inet4Address ip)
    {
        int integer = InetAddresses.coerceToInteger(ip);
        return integer >>> 22 == 0b110010001; // No violation of BitwiseOperatorUsage due to @SuppressWarnings
    }
}
