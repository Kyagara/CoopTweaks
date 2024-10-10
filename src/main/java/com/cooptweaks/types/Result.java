package com.cooptweaks.types;

/** A result of an operation, which can be either a success or an error. */
public class Result<T> {
	private final T value;
	private final String error;

	private Result(T value, String error) {
		this.value = value;
		this.error = error;
	}

	/** Create a success {@link Result} with a value. */
	public static <T> Result<T> success(T value) {
		return new Result<>(value, null);
	}

	/** Create an error {@link Result} with an error message. */
	public static <T> Result<T> error(String error) {
		return new Result<>(null, error);
	}

	public boolean isSuccess() {
		return error == null;
	}

	/**
	 Get the value of the {@link Result}, if it is a success.

	 @throws IllegalStateException if the result is an error.
	 */
	public T getValue() {
		if (!isSuccess()) {
			throw new IllegalStateException("Error is not empty.");
		}

		return value;
	}

	/**
	 Get the error of the {@link Result}, if it is an error.

	 @throws IllegalStateException if the result is a success.
	 */
	public String getError() {
		if (isSuccess()) {
			throw new IllegalStateException("Value is not empty.");
		}

		return error;
	}
}