package com.capte;

import com.wind.common.exception.AssertUtils;
import com.wind.common.util.WindReflectUtils;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.IntStream;

/**
 * podam 工具类
 *
 * @author wuxp
 */
public final class PodamUtils {

    private PodamUtils() {
        throw new AssertionError();
    }

    private static final PodamFactory FACTORY = new PodamFactoryImpl();

    public static <T> T manuSinglePojo(Class<T> clazz, String... ignoreFields) {
        AssertUtils.notNull(clazz, "argument clazz must not null");
        T result = FACTORY.manufacturePojo(clazz);
        for (String ignoreField : ignoreFields) {
            WindReflectUtils.setFieldValue(ignoreField, result, null);
        }
        return result;
    }

    public static <T> List<T> manuPojo(Class<T> clazz, Integer size, String... ignoreFields) {
        AssertUtils.notNull(clazz, "argument clazz must not null");
        return IntStream.range(0, size)
                .mapToObj(i -> manuSinglePojo(clazz, ignoreFields))
                .toList();
    }

    public static <T> T manufacturePojo(Class<T> clazz, Type... types) {
        return FACTORY.manufacturePojo(clazz, types);
    }
}
