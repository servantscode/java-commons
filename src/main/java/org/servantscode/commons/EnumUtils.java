package org.servantscode.commons;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnumUtils {

    public static <E extends Enum<E>> List<String> listValues(Class<E> e) {
        return Stream.of(e.getEnumConstants()).
               map(Enum::name).
               collect(Collectors.toList());
    }
}
