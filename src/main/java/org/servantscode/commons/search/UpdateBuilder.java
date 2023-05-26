package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.servantscode.commons.StringUtils.isSet;

public class UpdateBuilder extends FilterableBuilder<UpdateBuilder> {
    private static Logger LOG = LogManager.getLogger(UpdateBuilder.class);

    private enum BuilderState { START, WITH, TABLE, JOIN, VALUES, WHERE, DONE };

    private String table = null;
    private String with = null;
    private List<String> fields = new LinkedList<>();
    private List<String> joins = new LinkedList<>();

    private BuilderState state = BuilderState.START;

    public UpdateBuilder() {}

    public UpdateBuilder withCte(String with, Object... values) {
        setState(BuilderState.WITH);
        this.with = with;
        this.values.addAll(Arrays.asList(values));
        return this;
    }


    public UpdateBuilder update(String table) {
        setState(BuilderState.TABLE);
        this.table = table;
        return this;
    }

    public UpdateBuilder leftJoin(String join, Object... values) {
        setState(BuilderState.JOIN);
        this.joins.add("LEFT JOIN " + join);
        this.values.addAll(Arrays.asList(values));
        return this;
    }

    public UpdateBuilder value(String field, Object value) {
        setState(BuilderState.VALUES);
        this.fields.add(field);
        this.values.add(value);
        return this;
    }

    @Override
    protected void startFiltering() {
        setState(BuilderState.WHERE);
    }

    @Override
    public String getSql() {
        setState(BuilderState.DONE);
        if(!wheres.isEmpty() && !ors.isEmpty()) {
            ors.add(wheres);
            wheres = Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder();
        if(isSet(with))
            sql.append("WITH ").append(with).append(" ");
        sql.append("UPDATE ").append(table);
        if(!joins.isEmpty())
            sql.append(" ").append(String.join(" ", joins));
        if(!fields.isEmpty())
            sql.append(" SET ").append(fields.stream().map(f -> f + "=?").collect(Collectors.joining(", ")));
        if(!ors.isEmpty())
            sql.append(" WHERE (")
               .append(ors.stream().map(wheres -> String.join(" AND ", wheres)).collect(Collectors.joining(") OR (")))
               .append(")");
         if(!wheres.isEmpty())
            sql.append(" WHERE ").append(String.join(" AND ", wheres));

        return sql.toString();
    }

    // ----- Private -----
    private void setState(BuilderState nextState) {
        if(nextState.compareTo(state) < 0)
            throw new IllegalStateException("Cannot " + nextState + " after " + state);

        state = nextState;
    }
}
