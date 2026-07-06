package org.example.bdoc.plugin;

import org.example.bdoc.io.DocumentHandle;

import java.util.List;

/**
 * Точка расширения Preflight (Этап 2.2). BdocIntegrityValidator превращается
 * из монолита в лёгкий диспетчер: помимо встроенных проверок он опрашивает
 * все зарегистрированные ValidatorExtension и объединяет их сообщения с
 * основным списком ошибок. severity позволяет в будущем разделить
 * Error/Warning/Info без изменения контракта.
 */
public interface ValidatorExtension {
    String getId();

    List<ValidationIssue> validate(DocumentHandle document);

    record ValidationIssue(String pluginId, Severity severity, String message) {
        public enum Severity { ERROR, WARNING, INFO }
    }
}