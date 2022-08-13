package io.github.potjerodekool.demo.service;

public record CrudOperationResult(Status status,
                                  String message) {

    public CrudOperationResult(final Status status) {
        this(status, null);
    }

    public static CrudOperationResult success() {
        return new CrudOperationResult(Status.SUCCESS);
    }

    public static CrudOperationResult notFound() {
        return new CrudOperationResult(Status.NOT_FOUND);
    }

    public static CrudOperationResult failed() {
        return new CrudOperationResult(Status.FAILED);
    }

    public static CrudOperationResult failed(final String message) {
        return new CrudOperationResult(Status.FAILED, message);
    }

    public enum Status {
        SUCCESS,
        FAILED,
        NOT_FOUND
    }
}
