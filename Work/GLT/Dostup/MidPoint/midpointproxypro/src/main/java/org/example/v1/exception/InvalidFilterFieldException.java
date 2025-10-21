package org.example.v1.exception;

public class InvalidFilterFieldException extends RuntimeException {
    public InvalidFilterFieldException(String field) {
        super("Поле \"" + field + "\" не существует в фильтруемой сущности");
    }
}
