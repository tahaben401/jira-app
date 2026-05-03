package com.gateway.project.projectservice.exception;

public class ProjectException {

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String resource, Long id) {
            super(resource + " not found with id: " + id);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }
}
