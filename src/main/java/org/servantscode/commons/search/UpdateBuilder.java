package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.filter.Filterable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateBuilder extends FilterableBuilder<UpdateBuilder> {
    private static Logger LOG = LogManager.getLogger(UpdateBuilder.class);

    private enum BuilderState { START, TABLE, VALUES, WHERE, DONE };

    private String table = null;
    private List<String> fields = new LinkedList<>();

    private BuilderState state = BuilderState.START;

    public UpdateBuilder() {}

    public UpdateBuilder update(String table) {
        setState(BuilderState.TABLE);
        this.table = table;
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
        sql.append("UPDATE ").append(table);
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
