package com.davidlab.pricemonitor.common.exception;

public class UnsupportedPlatformException extends RuntimeException {

    public UnsupportedPlatformException(String url) {
        super("No crawler found for URL: " + url);
    }
}
