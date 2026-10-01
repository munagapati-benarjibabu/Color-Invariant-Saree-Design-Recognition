package com.saree.search.controller;

import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.bind.MissingServletRequestPartException.class})
  ResponseEntity<Map<String,String>> badRequest(Exception e) { return response(HttpStatus.BAD_REQUEST, e.getMessage() == null ? "Invalid request." : e.getMessage()); }
  @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<Map<String,String>> tooLarge(MaxUploadSizeExceededException e) { return response(HttpStatus.PAYLOAD_TOO_LARGE,"Image must be 10 MB or smaller."); }
  @ExceptionHandler(NoSuchElementException.class) ResponseEntity<Map<String,String>> missing(NoSuchElementException e) { return response(HttpStatus.NOT_FOUND,e.getMessage()); }
  @ExceptionHandler(Exception.class) ResponseEntity<Map<String,String>> unknown(Exception e) { return response(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to process the request. Check the application logs and database connection."); }
  private ResponseEntity<Map<String,String>> response(HttpStatus status,String msg) { return ResponseEntity.status(status).body(Map.of("message",msg)); }
}
