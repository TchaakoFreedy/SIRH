// com.fric.sirh.model.PayrollStatus
package com.fric.sirh.model;

public enum PayrollStatus {
    PROCESSING,
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    EMPLOYEE_NOT_FOUND,
    OCR_ERROR,
    INVALID_DOCUMENT
}