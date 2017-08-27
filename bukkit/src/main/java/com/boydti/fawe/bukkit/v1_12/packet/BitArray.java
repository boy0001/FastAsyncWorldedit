package com.boydti.fawe.bukkit.v1_12.packet;

public class BitArray {
    private final long[] longArray;
    private final int bitsPerEntry;
    private final long maxEntryValue;
    private final int arraySize;

    public BitArray(int num, int length) {
        this.arraySize = length;
        this.bitsPerEntry = num;
        this.maxEntryValue = (1L << num) - 1L;
        this.longArray = new long[roundUp(length * num, 64) / 64];
    }

    private int roundUp(int value, int interval) {
        if(interval == 0) {
            return 0;
        } else if(value == 0) {
            return interval;
        } else {
            if(value < 0) {
                interval *= -1;
            }

            int lvt_2_1_ = value % interval;
            return lvt_2_1_ == 0?value:value + interval - lvt_2_1_;
        }
    }

    public void setAt(int index, int value) {
        int lvt_3_1_ = index * this.bitsPerEntry;
        int lvt_4_1_ = lvt_3_1_ / 64;
        int lvt_5_1_ = ((index + 1) * this.bitsPerEntry - 1) / 64;
        int lvt_6_1_ = lvt_3_1_ % 64;
        this.longArray[lvt_4_1_] = this.longArray[lvt_4_1_] & ~(this.maxEntryValue << lvt_6_1_) | ((long)value & this.maxEntryValue) << lvt_6_1_;
        if(lvt_4_1_ != lvt_5_1_) {
            int lvt_7_1_ = 64 - lvt_6_1_;
            int lvt_8_1_ = this.bitsPerEntry - lvt_7_1_;
            this.longArray[lvt_5_1_] = this.longArray[lvt_5_1_] >>> lvt_8_1_ << lvt_8_1_ | ((long)value & this.maxEntryValue) >> lvt_7_1_;
        }

    }

    public int getAt(int index) {
        int localBitIndex = index * this.bitsPerEntry;
        int localLongIndex = localBitIndex / 64;
        int lvt_4_1_ = ((index + 1) * this.bitsPerEntry - 1) / 64;
        int lvt_5_1_ = localBitIndex % 64;
        if(localLongIndex == lvt_4_1_) {
            return (int)(this.longArray[localLongIndex] >>> lvt_5_1_ & this.maxEntryValue);
        } else {
            int lvt_6_1_ = 64 - lvt_5_1_;
            return (int)((this.longArray[localLongIndex] >>> lvt_5_1_ | this.longArray[lvt_4_1_] << lvt_6_1_) & this.maxEntryValue);
        }
    }

    public long[] getBackingLongArray() {
        return this.longArray;
    }

    public int size() {
        return this.arraySize;
    }
}
