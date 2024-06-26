package org.springframework.rocket.support;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class JavaUtils {

    /**
     * The singleton instance of this utility class.
     */
    public static final JavaUtils INSTANCE = new JavaUtils();

    private JavaUtils() {
    }

    /**
     * Invoke {@link Consumer#accept(Object)} with the value if the condition is true.
     * @param condition the condition.
     * @param value the value.
     * @param consumer the consumer.
     * @param <T> the value type.
     * @return this.
     */
    public <T> JavaUtils acceptIfCondition(boolean condition, T value, Consumer<T> consumer) {
        if (condition) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Invoke {@link Consumer#accept(Object)} with the value if it is not null.
     * @param value the value.
     * @param consumer the consumer.
     * @param <T> the value type.
     * @return this.
     */
    public <T> JavaUtils acceptIfNotNull(@Nullable T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Invoke {@link Consumer#accept(Object)} with the value if it is not null or empty.
     * @param value the value.
     * @param consumer the consumer.
     * @return this.
     */
    public JavaUtils acceptIfHasText(String value, Consumer<String> consumer) {
        if (StringUtils.hasText(value)) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Invoke {@link Consumer#accept(Object)} with the cast value if the object is an
     * instance of the provided class.
     * @param <T> the type of the class to check and cast.
     * @param type the type.
     * @param value the value to be checked and cast.
     * @param consumer the consumer.
     * @return this.
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public <T> JavaUtils acceptIfInstanceOf(Class<T> type, Object value, Consumer<T> consumer) {
        if (type.isAssignableFrom(value.getClass())) {
            consumer.accept((T) value);
        }
        return this;
    }

    /**
     * Invoke {@link Consumer#accept(Object)} with the value if it is not null or empty.
     * @param value the value.
     * @param consumer the consumer.
     * @param <K> the map key type.
     * @param <V> the map value type.
     * @return this.
     */
    public <K, V> JavaUtils acceptIfNotEmpty(Map<K, V> value, Consumer<Map<K, V>> consumer) {
        if (!ObjectUtils.isEmpty(value)) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Invoke {@link Consumer#accept(Object)} with the value if it is not null or empty.
     * @param value the value.
     * @param consumer the consumer.
     * @param <T> the value type.
     * @return this.
     */
    public <T> JavaUtils acceptIfNotEmpty(List<T> value, Consumer<List<T>> consumer) {
        if (!ObjectUtils.isEmpty(value)) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Invoke {@link Consumer#accept(Object)} with the value if it is not null or empty.
     * @param value the value.
     * @param consumer the consumer.
     * @param <T> the value type.
     * @return this.
     */
    public <T> JavaUtils acceptIfNotEmpty(T[] value, Consumer<T[]> consumer) {
        if (!ObjectUtils.isEmpty(value)) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Invoke {@link BiConsumer#accept(Object, Object)} with the arguments if the
     * condition is true.
     * @param condition the condition.
     * @param t1 the first consumer argument
     * @param t2 the second consumer argument
     * @param consumer the consumer.
     * @param <T1> the first argument type.
     * @param <T2> the second argument type.
     * @return this.
     */
    public <T1, T2> JavaUtils acceptIfCondition(boolean condition, T1 t1, T2 t2, BiConsumer<T1, T2> consumer) {
        if (condition) {
            consumer.accept(t1, t2);
        }
        return this;
    }

    /**
     * Invoke {@link BiConsumer#accept(Object, Object)} with the arguments if the t2
     * argument is not null.
     * @param t1 the first argument
     * @param t2 the second consumer argument
     * @param consumer the consumer.
     * @param <T1> the first argument type.
     * @param <T2> the second argument type.
     * @return this.
     */
    public <T1, T2> JavaUtils acceptIfNotNull(T1 t1, T2 t2, BiConsumer<T1, T2> consumer) {
        if (t2 != null) {
            consumer.accept(t1, t2);
        }
        return this;
    }

    /**
     * Invoke {@link BiConsumer#accept(Object, Object)} with the arguments if the value
     * argument is not null or empty.
     * @param t1 the first consumer argument.
     * @param value the second consumer argument
     * @param <T> the first argument type.
     * @param consumer the consumer.
     * @return this.
     */
    public <T> JavaUtils acceptIfHasText(T t1, String value, BiConsumer<T, String> consumer) {
        if (StringUtils.hasText(value)) {
            consumer.accept(t1, value);
        }
        return this;
    }

}

