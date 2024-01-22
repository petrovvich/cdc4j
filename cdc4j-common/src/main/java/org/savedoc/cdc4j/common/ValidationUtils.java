package org.savedoc.cdc4j.common;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern ALPHANUM_PATTERN = Pattern.compile("^\\w+$");
    private static final Pattern NUM_PATTERN = Pattern.compile("^[0-9]+$");

    public static boolean isNotEmpty(String aString) {
        return !isEmpty(aString);
    }
    public static boolean isEmpty(String aString) {
        return aString == null || aString.isEmpty();
    }

    public static boolean isNotAlphaNumeric(String replicationSlotName) {
        return !isAlphaNumeric(replicationSlotName);
    }

    public static boolean isAlphaNumeric(String replicationSlotName) {
        return Optional.ofNullable(replicationSlotName)
                .map(ALPHANUM_PATTERN::matcher)
                .map(Matcher::find)
                .orElse(false);
    }

    public static boolean isNotNumeric(String port) {
        return !isNumeric(port);
    }

    private static boolean isNumeric(String port) {
        return Optional.ofNullable(port)
                .map(NUM_PATTERN::matcher)
                .map(Matcher::find)
                .orElse(false);
    }
}
