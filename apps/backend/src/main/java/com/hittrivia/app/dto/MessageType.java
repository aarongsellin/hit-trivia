package com.hittrivia.app.dto;

public enum MessageType {
        DATA("data"),
        SUCCESS("success"),
        ERROR("error"),
        SERVER_ERROR("server_error");

        private final String value;

        MessageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static boolean isValid(String type) {
            for (MessageType mt : values()) {
                if (mt.value.equals(type)) {
                    return true;
                }
            }
        return false;
    }
} 
