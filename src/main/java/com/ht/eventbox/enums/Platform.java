package com.ht.eventbox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Platform {
    WEB("web_push"), ANDROID("android"), IOS("ios");
    private final String value;
}