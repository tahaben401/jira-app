package com.gateway.project.taskservice.exception;

public class TaskException {

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String resource, Long id) {
            super(resource + " not found with id: " + id);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }
}
