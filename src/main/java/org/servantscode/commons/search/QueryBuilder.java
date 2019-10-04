package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.Organization;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.security.OrganizationContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.servantscode.commons.StringUtils.isSet;

public class QueryBuilder extends FilterableBuilder<QueryBuilder> {
    private static Logger LOG = LogManager.getLogger(QueryBuilder.class);

    private enum BuilderState {START, SELECT, FROM, JOIN, WHERE, GROUP, SORT, LIMIT, OFFSET, DONE};

    private List<String> selections = new LinkedList<>();
    private List<String> tables = new LinkedList<>();
    private List<String> joins = new LinkedList<>();

    private List<String> groupBy = new LinkedList<>();
    private String sort;
    private boolean limit;
    private boolean offset;

    private BuilderState state = BuilderState.START;

    public QueryBuilder() {
    }

    public QueryBuilder select(String... selections) {
        setState(BuilderState.SELECT);
        this.selections.addAll(asList(selections));
        return this;
    }

    public QueryBuilder from(String... tables) {
        setState(BuilderState.FROM);
        this.tables.addAll(asList(tables));
        return this;
    }

    public QueryBuilder join(String... joins) {
        setState(BuilderState.JOIN);
        this.joins.addAll(asList(joins));
        return this;
    }

    public QueryBuilder leftJoin(String join) {
        setState(BuilderState.JOIN);
        this.joins.add("LEFT JOIN " + join);
        return this;
    }

    @Override
    protected void startFiltering() {
        setState(BuilderState.WHERE);
    }

    public QueryBuilder groupBy(String... fields) {
        setState(BuilderState.GROUP);
        groupBy.addAll(Arrays.asList(fields));
        return this;
    }

    public QueryBuilder page(String sort, int start, int count) {
        return this.sort(sort).limit(count).offset(start);
    }

    public QueryBuilder sort(String sort) {
        setState(BuilderState.SORT);
        this.sort = sort;
        return this;
    }

    public QueryBuilder limit(int limit) {
        setState(BuilderState.LIMIT);
        if(limit > 0) {
            this.limit = true;
            values.add(limit);
        }
        return this;
    }

    public QueryBuilder offset(int offset) {
        setState(BuilderState.OFFSET);
        if(offset > 0) {
            this.offset = true;
            values.add(offset);
        }
        return this;
    }


    public String getSql() {
        setState(BuilderState.DONE);
        if(!wheres.isEmpty() && !ors.isEmpty()) {
            ors.add(wheres);
            wheres = Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(String.join(", ", selections));
        sql.append(" FROM ").append(String.join(", ", tables));
        if(!joins.isEmpty())
            sql.append(" ").append(String.join(" ", joins));
        if(!ors.isEmpty())
            sql.append(" WHERE (")
               .append(ors.stream().map(wheres -> String.join(" AND ", wheres)).collect(Collectors.joining(") OR (")))
               .append(")");
        if(!wheres.isEmpty())
            sql.append(" WHERE ").append(String.join(" AND ", wheres));
        if(!groupBy.isEmpty())
            sql.append(" GROUP BY ").append(String.join(", ", groupBy));
        if(isSet(sort))
            sql.append(" ORDER BY " + sort);
        if(limit)
            sql.append(" LIMIT ?");
        if(offset)
            sql.append(" OFFSET ?");
        return sql.toString();
    }

    // ----- Private -----
    private void setState(BuilderState nextState) {
        if(nextState.compareTo(state) < 0)
            throw new IllegalStateException("Cannot " + nextState + " after " + state);

        state = nextState;
    }
}
