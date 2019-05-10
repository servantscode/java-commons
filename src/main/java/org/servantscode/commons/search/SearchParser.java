package org.servantscode.commons.search;

import org.servantscode.commons.ReflectionUtils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.servantscode.commons.StringUtils.isSet;

public class SearchParser<T> {
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
        String[] searchBits = string.split(":");
        String fieldName = searchBits.length > 1? searchBits[0]: defaultField;
        String value = searchBits[searchBits.length-1];

        Class<?> fieldType = ReflectionUtils.getDeepFieldType(clazz, fieldName);
        if(fieldType == String.class) {
            if(value.startsWith("\""))
                value = value.substring(1);
            if(value.endsWith("\""))
                value = value.substring(0, value.length()-1);
            return new Search.TextClause(map(fieldName), value);
        } else if(fieldType == boolean.class || fieldType == Boolean.class) {
            return new Search.BooleanClause(map(fieldName), Boolean.parseBoolean(value));
        } else if(fieldType == int.class || fieldType == Integer.class) {
            if(value.contains("[")) {
                //Parsing [date1 TO date2]
                String[] bits = value.substring(1, value.length()-1).split(" ");
                return new Search.IntegerRangeClause(map(fieldName), Integer.parseInt(bits[0]), Integer.parseInt(bits[2]));
            }
            return new Search.IntegerClause(map(fieldName), Integer.parseInt(value));
        } else if(fieldType == LocalDate.class || fieldType == ZonedDateTime.class) {
            if(value.contains("[")) {
                //Parsing [date1 TO date2]
                String[] bits = value.substring(1, value.length()-1).split(" ");
                return new Search.DateRangeClause(map(fieldName), LocalDate.parse(bits[0]), LocalDate.parse(bits[2]));
            }
            return new Search.DateClause(map(fieldName), LocalDate.parse(value));
        } else {
            throw new IllegalArgumentException(String.format("Can't figure out what to do with field %s (type: %s)", fieldName, fieldType.getSimpleName()));
        }
    }

    private String map(String fieldName) {
        if(fieldMap == null)
            return fieldName;
        return fieldMap.getOrDefault(fieldName, fieldName);
    }
}
