package org.nem.core.model;

public class TransactionTypes {
    private static int TRANSFER_TYPE = 0x0100;
    private static int ASSET_TYPE = 0x0200;
    private static int SNAPSHOT_TYPE = 0x0400;

    public static int TRANSFER = TRANSFER_TYPE | 0x01;
    public static int ASSET_NEW = ASSET_TYPE | 0x01;
    public static int ASSET_ASK = ASSET_TYPE | 0x02;
    public static int ASSET_BID = ASSET_TYPE | 0x03;
    public static int SNAPSHOT = SNAPSHOT_TYPE | 0x00;
}