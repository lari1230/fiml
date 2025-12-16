package main.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class JsonResponse {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String success(Object data) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return mapper.writeValueAsString(response);
    }

    public static String success(String message) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return mapper.writeValueAsString(response);
    }

    public static String success(Object data, String message) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return mapper.writeValueAsString(response);
    }

    public static String error(String message) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return mapper.writeValueAsString(response);
    }

    public static String error(String message, int code) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("code", code);
        return mapper.writeValueAsString(response);
    }
}