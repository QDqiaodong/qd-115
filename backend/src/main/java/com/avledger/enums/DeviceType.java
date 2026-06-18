package com.avledger.enums;

public enum DeviceType {
    SPEAKER("音响"),
    PROJECTOR("投影仪"),
    PLAYER("播放器"),
    AMPLIFIER("功放");

    private final String label;

    DeviceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
