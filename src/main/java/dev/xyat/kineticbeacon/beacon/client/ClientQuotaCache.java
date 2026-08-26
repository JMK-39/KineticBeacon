package dev.xyat.kineticbeacon.beacon.client;

public class ClientQuotaCache {
    public static int globalUsed = 0;
    public static int globalMax = 500;
    public static boolean perPlayerEnabled = false;
    public static int personalUsed = 0;
    public static int personalMax = 100;

    public static int offlineTimeout = 4320;
    public static boolean offlineDeact = true;
    public static boolean offlineCL = true;
    public static boolean offlineSP = true;
}