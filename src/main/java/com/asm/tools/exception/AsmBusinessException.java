package com.asm.tools.exception;

public class AsmBusinessException extends RuntimeException {
    public AsmBusinessException(String message, Throwable e) {
        super(message, e);
    }
}
