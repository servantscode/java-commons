package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteBuilder extends FilterableBuilder<DeleteBuilder> {
    private static Logger LOG = LogManager.getLogger(DeleteBuilder.class);

    private enum BuilderState { START, TABLE, WHERE, DONE };

    private String table = null;

    private BuilderState state = BuilderState.START;

    public DeleteBuilder() {}

    public DeleteBuilder delete(String table) {
        setState(BuilderState.TABLE);
        this.table = table;
        return this;
    }

    @Override
    protected void startFiltering() {
        setState(BuilderState.WHERE);
    }

    @Override
    protected String getSql() {
        setState(BuilderState.DONE);
        if(!wheres.isEmpty() && !ors.isEmpty()) {
            ors.add(wheres);
            wheres = Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(table);
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
