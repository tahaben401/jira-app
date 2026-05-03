package com.gateway.project.projectservice.exception;

import com.gateway.project.projectservice.dto.ProjectDtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProjectException.NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ProjectException.NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProjectException.UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(ProjectException.UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProjectException.ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ProjectException.ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unexpected error: " + ex.getMessage()));
    }
}
