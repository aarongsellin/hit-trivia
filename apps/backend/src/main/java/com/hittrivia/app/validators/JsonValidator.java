package com.hittrivia.app.validators;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class JsonValidator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static boolean validate(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(jsonString.trim());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}