package org.servantscode.commons;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Ref;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ReflectionUtils {
    private static final Logger LOG = LogManager.getLogger(ReflectionUtils.class);
    public static Class<?> getDeepFieldType(Class<?> clazz, String fieldName) {
        String[] fieldPath = fieldName.split("\\.");

        Class<?> currentClazz = clazz;
        for(String field: fieldPath) {
            currentClazz = getFieldType(currentClazz, field);
            if(currentClazz == null)
                return null;
        }

        return currentClazz;
    }

    public static Class<?> getFieldType(Class<?> clazz, String fieldName) {
        Method getter = getGetter(clazz, fieldName);
        if(getter == null)
            return null;
        return getter.getReturnType();
    }

    public static <T> String getFieldValue(Method m, T d) {
        try {
            Object o = m.invoke(d);
            return o != null ? o.toString() : "null";
        } catch (InvocationTargetException | IllegalAccessException var4) {
            throw new RuntimeException("Could not get value from: " + m.getName(), var4);
        }
    }

    public static <T> Field getField(Method m) {
        String fieldName = getFieldName(m);
        return stream(m.getDeclaringClass().getDeclaredFields())
                .filter(f -> f.getName().equals(fieldName))
                .findFirst().orElse(null);
    }

    // Find accessors
    public static Method getGetter(Class<?> clazz, String fieldName) {
        List<Method> methods = filterMethods(clazz, m -> isGetter(m) && getFieldName(m).equals(fieldName));
        return methods.size() > 0? methods.get(0): null;
    }

    public static Method getSetter(Class<?> clazz, String fieldName) {
        List<Method> methods = filterMethods(clazz, m -> isSetter(m) && getFieldName(m).equals(fieldName));
        return methods.size() > 0? methods.get(0): null;
    }

    public static List<Method> getSetters(Class<?> clazz) {
        return filterMethods(clazz, ReflectionUtils::isSetter);
    }

    public static List<Method> getGetters(Class<?> clazz) {
        return filterMethods(clazz, ReflectionUtils::isGetter);
    }

    public static List<Method> filterMethods(Class<?> clazz, Predicate<Method> pred) {
        return stream(clazz.getMethods()).filter(pred).collect(Collectors.toList());
    }

    public static boolean isSetter(Method m) {
        return m.getName().startsWith("set");
    }

    public static boolean isGetter(Method m) {
        return (m.getName().startsWith("get") || m.getName().startsWith("is")) && !m.getName().equals("getClass");
    }

    public static String getFieldName(Method m) {
        JsonProperty methodAnnotation = m.getAnnotation(JsonProperty.class);
        if(methodAnnotation != null) {
            String annotatedFieldName = methodAnnotation.value();
            LOG.debug("ReflectionUtils calculated field name: " + annotatedFieldName + " from method annotation: " + m.getName());
            return annotatedFieldName;
        }

        String methodName = m.getName();
        if(!methodName.startsWith("get") && !methodName.startsWith("set") && !methodName.startsWith("is"))
            throw new IllegalArgumentException("Method provided is not an accessor: " + methodName);

        String fieldName = methodName.startsWith("is") ? methodName.substring(2) : methodName.substring(3);
        fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);

        JsonProperty fieldAnnotation = null;
        try {
            Field field = m.getDeclaringClass().getDeclaredField(fieldName);
            fieldAnnotation = field.getAnnotation(JsonProperty.class);
        } catch (NoSuchFieldException e) { /* If no field, just move on. */}

        return fieldAnnotation != null? fieldAnnotation.value(): fieldName;
    }

    public static <T> void patchObject(Class<T> clazz, T existing, Map<String, Object> data) {
        T updates;
        try {
            ObjectMapper mapper = ObjectMapperFactory.getMapper();
            updates = mapper.readValue(mapper.writeValueAsString(data), clazz);
        } catch (IOException e) {
            throw new RuntimeException("Unamppable data provided: " + data, e);
        }

        data.keySet().forEach(key -> {
            try {
                Method getter = getGetter(clazz, key);
                Object value = getter.invoke(updates);

                Method setter = getSetter(clazz, key);
                setter.invoke(existing, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to update field: " + key, e);
            }
        });
    }
}
