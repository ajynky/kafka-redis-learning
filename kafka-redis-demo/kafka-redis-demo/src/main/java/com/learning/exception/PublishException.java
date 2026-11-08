package com.learning.exception;

public class PublishException extends RuntimeException {
    public PublishException(String topicName, String key, Throwable cause) {
        super("Failed to publish to topic " + topicName + " with key " + key, cause);
    }
}
