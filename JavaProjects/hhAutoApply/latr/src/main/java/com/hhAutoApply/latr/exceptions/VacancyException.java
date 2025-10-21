package com.hhAutoApply.latr.exceptions;

public class VacancyException extends RuntimeException {
    public VacancyException(String responseReceivedIsEmpty) {
        super(responseReceivedIsEmpty);
    }

}
