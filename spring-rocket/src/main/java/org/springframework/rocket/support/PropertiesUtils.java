package org.springframework.rocket.support;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PropertiesUtils {

    public static String extractAsString(Function<String, Object> valueFunction, String attribute) {
        Object value = valueFunction.apply(attribute);
        return value == null ? null : String.valueOf(value);
    }

    public static String extractAsString(Map<String, Object> properties, String attribute) {
        if (ObjectUtils.isEmpty(properties) || !StringUtils.hasText(attribute)) {
            return null;
        }
        return extractAsString(properties::get, attribute);
    }

    public static Boolean extractAsBoolean(Function<String, Object> valueFunction, String attribute) {
        Object value = valueFunction.apply(attribute);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string)  {
            return Boolean.parseBoolean(string);
        }
        throw new IllegalStateException(String.format(
                """
                The [%s] must resolve to a Boolean or a String that can be parsed as a Boolean.\s
                Resolved to [%s] for [%s]
                """, attribute, value.getClass(), value)
        );
    }
    public static Boolean extractAsBoolean(Map<String, Object> properties, String attribute) {
        if (ObjectUtils.isEmpty(properties) || !StringUtils.hasText(attribute)) {
            return null;
        }
        return extractAsBoolean(properties::get, attribute);
    }

    public static Integer extractAsInteger(Function<String, Object> valueFunction, String attribute) {
        Object value = valueFunction.apply(attribute);
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return Integer.parseInt(string);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException(String.format(
                """
                The [%s] must resolve to an Number or a String that can be parsed as an Integer.\s
                Resolved to [%s] for [%s]
                """, attribute, value.getClass(), value)
        );
    }
    public static Integer extractAsInteger(Map<String, Object> properties, String attribute) {
        if (ObjectUtils.isEmpty(properties) || !StringUtils.hasText(attribute)) {
            return null;
        }
        return extractAsInteger(properties::get, attribute);
    }


    public static Long extractAsLong(Function<String, Object> valueFunction, String attribute) {
        Object value = valueFunction.apply(attribute);
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return Long.parseLong(string);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException(String.format(
                """
                The [%s] must resolve to an Number or a String that can be parsed as an Integer.\s
                Resolved to [%s] for [%s]
                """, attribute, value.getClass(), value)
        );
    }
    public static Long extractAsLong(Map<String, Object> properties, String attribute) {
        if (ObjectUtils.isEmpty(properties) || !StringUtils.hasText(attribute)) {
            return null;
        }
        return extractAsLong(properties::get, attribute);
    }

    public static <E extends Enum<?>> E extractAsEnum(Function<String, Object> valueFunction, String attribute, Class<E> enumeration) {
        String enumName = extractAsString(valueFunction, attribute);
        if (enumName == null) {
            return null;
        }
        for (E e : enumeration.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(enumName)) {
                return e;
            }
        }
        return null;
    }
    public static <E extends Enum<?>> E extractAsEnum(Map<String, Object> properties, String attribute, Class<E> enumeration) {
        if (ObjectUtils.isEmpty(properties) || !StringUtils.hasText(attribute)) {
            return null;
        }
        return extractAsEnum(properties::get, attribute, enumeration);
    }

    public static Map<String, Object> asMap(Properties properties) {
        if (ObjectUtils.isEmpty(properties)) {
            return new HashMap<>(64);
        }
        return properties.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue, (prev, next) -> next,
                        HashMap::new));
    }


    public static Map<String, Object> asMap(Properties properties, Properties overrideProperties) {
        return asMap(asMap(properties), overrideProperties);
    }

    public static Map<String, Object> asMap(Properties properties, Map<String, Object> overrideProperties) {
        return asMap(asMap(properties), overrideProperties);
    }

    public static Map<String, Object> asMap(Map<String, Object> properties, Properties overrideProperties) {
        if (ObjectUtils.isEmpty(properties) && ObjectUtils.isEmpty(overrideProperties)) {
            return new HashMap<>(64);
        }
        if (ObjectUtils.isEmpty(overrideProperties)) {
            return new HashMap<>(properties);
        }
        Map<String, Object> map = asMap(overrideProperties);
        if (!ObjectUtils.isEmpty(properties)) {
            properties.forEach(map::putIfAbsent);
        }
        return map;
    }

    public static Map<String, Object> asMap(Map<String, Object> properties, Map<String, Object> overrideProperties) {
        if (ObjectUtils.isEmpty(properties) && ObjectUtils.isEmpty(overrideProperties)) {
            return new HashMap<>(64);
        }
        if (ObjectUtils.isEmpty(overrideProperties)) {
            return new HashMap<>(properties);
        }

        Map<String, Object> map = new HashMap<>(overrideProperties);
        if (!ObjectUtils.isEmpty(properties)) {
            properties.forEach(map::putIfAbsent);
        }
        return map;
    }

}
