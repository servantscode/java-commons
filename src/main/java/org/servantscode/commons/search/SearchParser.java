package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.ReflectionUtils;
import org.servantscode.commons.search.FieldTransformer.Transformation;
import org.servantscode.commons.search.Search.CompoundClause;
import org.servantscode.commons.search.Search.CompoundClause.ClauseType;
import org.servantscode.commons.search.Search.SearchClause;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class SearchParser<T> {
    private static final Logger LOG = LogManager.getLogger(SearchParser.class);

    private final Class<T> clazz;
    private final String defaultField;
    private final FieldTransformer transformer;

    private static class TestClass {
        String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static void main(String[] args) {
        SearchParser<TestClass> parser = new SearchParser<>(TestClass.class);
        Search parsed = parser.parse("test test2");
        System.out.println("Parsed: " + parsed.getSql());
        System.out.println("Values: " + parsed.getValues().stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    public SearchParser(Class<T> clazz) {
        this(clazz, "name", new FieldTransformer());
    }

    public SearchParser(Class<T> clazz, String defaultField) {
        this(clazz, defaultField, new FieldTransformer());
    }

    public SearchParser(Class<T> clazz, String defaultField, Map<String, String> fieldMap) {
        this(clazz, defaultField, new FieldTransformer(fieldMap));
    }

    public SearchParser(Class<T> clazz, String defaultField, FieldTransformer transformer) {
        this.clazz = clazz;
        this.defaultField = defaultField;
        this.transformer = transformer;
    }

    public Search parse(String searchString) {
        if(isEmpty(searchString))
            return null;

        LOG.trace("Parsing search string: " + searchString);
        String[] clauseStrings = parseText(searchString);

        CompoundClause andClause = createOrClause(clauseStrings, new AtomicInteger(0));

        Search search = new Search();
        search.addClause(andClause);
        return search;
    }

    private CompoundClause createAndClause(String[] clauseStrings, AtomicInteger loc) {
        CompoundClause andClause = new CompoundClause(ClauseType.AND);
        boolean openParen = false;
        while(loc.get() < clauseStrings.length) {
            String clause = clauseStrings[loc.get()];
            switch (clause) {
                case "(":
                    openParen = true;
                    loc.incrementAndGet();
                    andClause.addClause(createOrClause(clauseStrings, loc));
                    break;
                case ")":
                    if(!openParen)
                        return andClause;

                    loc.incrementAndGet();
                    return andClause;
               case "OR":
                    loc.incrementAndGet();
                    return andClause;
                case "AND":
                    loc.incrementAndGet();
                    break;
                default:
                    loc.incrementAndGet();
                    andClause.addClause(createClause(clause));
            }
        }
        return andClause;
    }

    private CompoundClause createOrClause(String[] clauseStrings, AtomicInteger loc) {
        CompoundClause orClause = new CompoundClause(ClauseType.OR);
        boolean openParen = false;
        while(loc.get() < clauseStrings.length) {
            String clause = clauseStrings[loc.get()];
            switch (clause) {
                case "(":
                    openParen = true;
                    loc.incrementAndGet();
                    orClause.addClause(createOrClause(clauseStrings, loc));
                    break;
                case ")":
                    if(!openParen)
                        return orClause;

                    loc.incrementAndGet();
                    return orClause;
                default:
                    orClause.addClause(createAndClause(clauseStrings, loc));
            }
        }
        return orClause;
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
                case '(':
                case ')':
                    if(quote || range)
                        break;

                    String clause = new String(chars, start, i-start).trim();
                    if(isSet(clause))
                        clauses.add(clause);

                    if(chars[i] != ' ')
                        clauses.add(chars[i]+"");

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

    private SearchClause createClause(String string) {
//        LOG.trace("Parsing clause: " + string);
        String[] searchBits = string.split(":", 2);
        String fieldName = searchBits.length > 1? searchBits[0]: defaultField;
        String value = searchBits[searchBits.length-1];

        Transformation transformation = transformer.get(fieldName);

        if(transformation.isCustom())
            return new Search.CustomClause(transformation.getCustomSql(), transformation.transform(value));

        Class<?> fieldType = transformation.getFieldType() != null? transformation.getFieldType(): ReflectionUtils.getDeepFieldType(clazz, fieldName);
        if(fieldType == null) {
            return new Search.GenericClause(transformation.fieldName(), transformation.transform(stripQuotes(value)));
        } else if(fieldType == String.class) {
            return new Search.TextClause(transformation.fieldName(), (String) transformation.transform(stripQuotes(value)));
        } else if(fieldType.isEnum()) {
            return new Search.EnumClause(transformation.fieldName(), value);
        } else if(List.class.isAssignableFrom(fieldType)) {
            List<String> items = Arrays.stream(value.split("\\|")).map(this::stripQuotes).collect(toList());
            return new Search.ListItemClause(transformation.fieldName(), items);
        } else if(fieldType == boolean.class || fieldType == Boolean.class) {
            return new Search.BooleanClause(transformation.fieldName(), Boolean.parseBoolean(value));
        } else if(fieldType == int.class || fieldType == Integer.class ||
                  fieldType == float.class || fieldType == Float.class ||
                  fieldType == double.class || fieldType == Double.class ||
                  fieldType == long.class || fieldType == Long.class) {
            if(value.contains("[")) {
                //Parsing [date1 TO date2]
                String[] bits = value.substring(1, value.length()-1).split(" ");
                return new Search.NumberRangeClause(transformation.fieldName(), parseNumber(bits[0]), parseNumber(bits[2]));
            }
            return new Search.NumberClause(transformation.fieldName(), parseNumber(value));
        } else if(fieldType == LocalDate.class) {
            if(value.contains("[")) {
                //Parsing [date1 TO date2]
                String[] bits = value.substring(1, value.length()-1).split(" ");
                return new Search.DateRangeClause(transformation.fieldName(), parseDate(bits[0]), parseDate(bits[2]));
            }
            return new Search.DateClause(transformation.fieldName(), parseDate(value));
        } else if(fieldType == ZonedDateTime.class){
            if(!value.contains("["))
                throw new IllegalArgumentException("Could not process time range: " + value);

            //Parsing [date1 TO date2]
            String[] bits = value.substring(1, value.length()-1).split(" ");
            return new Search.TimeRangeClause(transformation.fieldName(), parseTime(bits[0]), parseTime(bits[2], true));
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
}
