package org.example.bdoc.spi;

public interface ScriptFunctionExtension {
    String getNamespace();
    String getFunctionName();
    Object invoke(Object context, Object... args);
}