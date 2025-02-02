package com.filantrop.pvnclient.enums;

import lombok.Getter;

@Getter
public enum HandlerMessageTypes {
    // цифры от балды
    CONNECTION_STATE(586),
    SPEED_INFO(599),
    ERROR(9881),
    UNKNOWN(6000);

    public final int value;

    HandlerMessageTypes(int value) {
        this.value = value;
    }
}
