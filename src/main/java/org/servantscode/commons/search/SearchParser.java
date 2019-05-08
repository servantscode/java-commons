package org.servantscode.commons.search;

import org.servantscode.commons.ReflectionUtils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.servantscode.commons.StringUtils.isSet;

public class SearchParser<T> {
    private final Class<T> clazz;
    private final String defaultField;

    public SearchParser(Class<T> clazz) {
        this(clazz, "name");
    }

    public SearchParser(Class<T> clazz, String defaultField) {
        this.clazz = clazz;
        this.defaultField = defaultField;
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

        Class<?> fieldType = ReflectionUtils.getFieldType(clazz, fieldName);
        if(fieldType == String.class) {
            return new Search.TextClause(fieldName, value);
        } else if(fieldType == boolean.class || fieldType == Boolean.class) {
            return new Search.BooleanClause(fieldName, Boolean.parseBoolean(value));
        } else if(fieldType == LocalDate.class || fieldType == ZonedDateTime.class) {
            if(value.contains("[")) {
                //Parsing [date1 TO date2]
                String[] bits = value.substring(1, value.length()-1).split(" ");
                return new Search.DateRangeClause(fieldName, LocalDate.parse(bits[0]), LocalDate.parse(bits[2]));
            }
            return new Search.DateClause(fieldName, LocalDate.parse(value));
        } else {
            throw new IllegalArgumentException(String.format("Can't figure out what to do with field %s (type: %s)", fieldName, fieldType.getSimpleName()));
        }
    }
}
