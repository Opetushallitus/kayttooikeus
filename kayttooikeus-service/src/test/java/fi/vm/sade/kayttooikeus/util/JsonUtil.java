package fi.vm.sade.kayttooikeus.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JsonUtil {
    private JsonUtil() {
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * @param object to turn into JSON
     * @return JSON string
     */
    public static String asJson(Object object) {
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Writing JSON failed: " + e.getMessage(), e);
        }
    }

    /**
     * @param json JSON to convert into Java objects
     * @return List containing Map-objects
     */
    public static List<Map<String,Object>> jsonToList(@Language("JSON") String json) {
        try {
            return getObjectMapper().reader().forType(ArrayList.class).readValue(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading JSON failed: " + e.getMessage(), e);
        }
    }

    /**
     * @param json JSON to convert into Java objects
     * @return recursive HashMap using Collections for arrays of the JSON
     */
    public static Map<String,Object> jsonToMap(@Language("JSON") String json) {
        try {
            return getObjectMapper().reader().forType(HashMap.class).readValue(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading JSON failed: " + e.getMessage(), e);
        }
    }

    public static<T> T readJson(@Language("JSON") String json, Class<T> type) throws IOException {
//        try {
            return getObjectMapper().readerFor(type).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(json);
//        } catch (IOException e) {
//            throw new IllegalArgumentException("Reading JSON failed: " + e.getMessage(), e);
//        }
    }
    
    public static<T> T readJson(@Language("JSON") String json, TypeReference<T> type) {
        try {
            return getObjectMapper().readerFor(type).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading JSON failed: " + e.getMessage(), e);
        }
    }

    // For Intellij IDEA
    public static String json(@Language("JSON") String json) {
        return json;
    }
}
