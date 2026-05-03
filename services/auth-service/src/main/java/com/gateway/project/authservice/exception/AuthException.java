package com.gateway.project.authservice.exception;

public class AuthException {

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
        public NotFoundException(String resource, Long id) {
            super(resource + " not found with id: " + id);
        }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    public static class TokenException extends RuntimeException {
        public TokenException(String message) {
            super(message);
        }
    }
}
