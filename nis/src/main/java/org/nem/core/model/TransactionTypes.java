package org.nem.core.model;

public class TransactionTypes {
    private static final int TRANSFER_TYPE = 0x0100;
    private static final int ASSET_TYPE = 0x0200;
    private static final int SNAPSHOT_TYPE = 0x0400;

    public static final int TRANSFER = TRANSFER_TYPE | 0x01;
    public static final int ASSET_NEW = ASSET_TYPE | 0x01;
    public static final int ASSET_ASK = ASSET_TYPE | 0x02;
    public static final int ASSET_BID = ASSET_TYPE | 0x03;
    public static final int SNAPSHOT = SNAPSHOT_TYPE | 0x00;
}