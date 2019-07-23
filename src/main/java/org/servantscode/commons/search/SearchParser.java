package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.ReflectionUtils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class SearchParser<T> {
    private static final Logger LOG = LogManager.getLogger(SearchParser.class);

    private final Class<T> clazz;
    private final String defaultField;
    private final Map<String, String> fieldMap;

    public SearchParser(Class<T> clazz) {
        this(clazz, "name", null);
    }

    public SearchParser(Class<T> clazz, String defaultField) {
        this(clazz, defaultField, null);
    }

    public SearchParser(Class<T> clazz, String defaultField, Map<String, String> fieldMap) {
        this.clazz = clazz;
        this.defaultField = defaultField;
        this.fieldMap = fieldMap;
    }

    public Search parse(String searchString) {
        if(isEmpty(searchString))
            return null;

        LOG.trace("Parsing search string: " + searchString);
        String[] clauseStrings = parseText(searchString);

        Search search = new Search();
        Arrays.stream(clauseStrings).forEach(clause -> search.addClause(createClause(clause)));
        return search;
    }

    public String[] parseText(String searchString) {
        boolean quote=false;
        boolean range=false;

        char[] chars = searchString.trim().toCharArray();
        List<String> clauses = new LinkedList<>();
        int start = 0;
        for(int i=0; i<chars.length; i++) {
            switch (chars[i]) {
                case ' ':
                    if(quote || range)
                        break;

                    String clause = new String(chars, start, i-start);
                    if(isSet(clause))
                        clauses.add(clause);
                    start=i+1;
                    break;
                case '\"':
                    quote = !quote;
                    break;
                case '[':
                    if(quote)
                        break;

                    if(range)
                        throw new RuntimeException("Could not parse query string.");

                    range = true;
                    break;
                case ']':
                    if(quote)
                        break;

                    if(!range)
                        throw new RuntimeException("Could not parse query string.");
                    range = false;
                    break;
            }
        }
        if(start < chars.length) {
            if(quote || range)
                throw new RuntimeException("Could not parse query string.");

            String clause = new String(chars, start, chars.length-start);
            if(isSet(clause))
                clauses.add(clause);
        }

        return clauses.toArray(new String[clauses.size()]);
    }

    private Search.SearchClause createClause(String string) {
//        LOG.trace("Parsing clause: " + string);
        String[] searchBits = string.split(":", 2);
        String fieldName = searchBits.length > 1? searchBits[0]: defaultField;
        String value = searchBits[searchBits.length-1];

        Class<?> fieldType = ReflectionUtils.getDeepFieldType(clazz, fieldName);
        if(fieldType == String.class) {
            return new Search.TextClause(map(fieldName), stripQuotes(value));
        } else if(fieldType.isEnum()) {
            return new Search.EnumClause(map(fieldName), value);
        } else if(List.class.isAssignableFrom(fieldType)) {
            List<String> items = Arrays.stream(value.split("\\|")).map(this::stripQuotes).collect(toList());
            return new Search.ListItemClause(map(fieldName), items);
        } else if(fieldType == boolean.class || fieldType == Boolean.class) {
            return new Search.BooleanClause(map(fieldName), Boolean.parseBoolean(value));
        } else if(fieldType == int.class || fieldType == Integer.class ||
                  fieldType == float.class || fieldType == Float.class ||
                  fieldType == double.class || fieldType == Double.class ||
                  fieldType == long.class || fieldType == Long.class) {
            if(value.contains("[")) {
                //Parsing [date1 TO date2]
                String[] bits = value.substring(1, value.length()-1).split(" ");
                return new Search.NumberRangeClause(map(fieldName), parseNumber(bits[0]), parseNumber(bits[2]));
            }
            return new Search.NumberClause(map(fieldName), parseNumber(value));
        } else if(fieldType == LocalDate.class) {
            if(value.contains("[")) {
                //Parsing [date1 TO date2]
                String[] bits = value.substring(1, value.length()-1).split(" ");
                return new Search.DateRangeClause(map(fieldName), parseDate(bits[0]), parseDate(bits[2]));
            }
            return new Search.DateClause(map(fieldName), parseDate(value));
        } else if(fieldType == ZonedDateTime.class){
            if(!value.contains("["))
                throw new IllegalArgumentException("Could not process time range: " + value);

            //Parsing [date1 TO date2]
            String[] bits = value.substring(1, value.length()-1).split(" ");
            return new Search.TimeRangeClause(map(fieldName), parseTime(bits[0]), parseTime(bits[2], true));
        } else {
            throw new IllegalArgumentException(String.format("Can't figure out what to do with field %s (type: %s)", fieldName, fieldType.getSimpleName()));
        }
    }

    private String stripQuotes(String value) {
        if (value.startsWith("\""))
            value = value.substring(1);
        if (value.endsWith("\""))
            value = value.substring(0, value.length() - 1);
        return value;
    }

    private Number parseNumber(String number) {
        try {
            return number.equals("*")? null: NumberFormat.getInstance().parse(number);
        } catch (ParseException e) {
            return null;
        }
    }

    private LocalDate parseDate(String bit) { return bit.equals("*")? null: LocalDate.parse(bit); }

    private ZonedDateTime parseTime(String bit) { return parseTime(bit, false); }

    private ZonedDateTime parseTime(String bit, boolean endOfDayOption) {
        if(bit.equals("*")) return null;

        ZonedDateTime zdt = parseZonedDateTime(bit);
        if(zdt != null)
            return zdt;

        LocalDateTime ldt = parseLocalDateTime(bit);
        if(ldt != null)
            return ldt.atZone(ZoneId.systemDefault());

        return LocalDate.parse(bit).plusDays(endOfDayOption? 1:0).atStartOfDay(ZoneId.systemDefault()).minusNanos(1);
    }

    private ZonedDateTime parseZonedDateTime(String bit) {
        try {
            return ZonedDateTime.parse(bit);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseLocalDateTime(String bit) {
        try {
            return LocalDateTime.parse(bit);
        } catch (Exception e) {
            return null;
        }
    }

    private String map(String fieldName) {
        if(fieldMap == null)
            return fieldName;
        return fieldMap.getOrDefault(fieldName, fieldName);
    }
}
