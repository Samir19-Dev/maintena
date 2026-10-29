package com.maintena.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {
	private String timestamp;
	private int status;
	private String error;
	private String message;
	private String path;
	private List<String> details;
}
