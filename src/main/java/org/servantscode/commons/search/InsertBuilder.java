package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.security.OrganizationContext;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.servantscode.commons.StringUtils.isSet;

public class InsertBuilder extends SqlBuilder {
    private static Logger LOG = LogManager.getLogger(QueryBuilder.class);

    private enum BuilderState {START, INTO, FIELDS, SELECT, FROM, JOIN, WHERE, VALUES, ON, DONE };

    private String table = null;
    private String select = null;
    private String from = null;
    private List<String> fields = new LinkedList<>();
    private List<String> joins = new LinkedList<>();
    protected List<String> wheres = new LinkedList<>();
    protected String on = null;

    private boolean addValueStatment = false;

    private BuilderState state = BuilderState.START;

    public InsertBuilder() {}

    public InsertBuilder into(String table) {
        setState(BuilderState.INTO);
        this.table = table;
        return this;
    }

    public InsertBuilder field (String... fields){
        setState(BuilderState.FIELDS);
        this.fields.addAll(List.of(fields));
        return this;
    }

    public InsertBuilder field(String field) {
        setState(BuilderState.FIELDS);
        this.fields.add(field);
        return this;
    }

    public InsertBuilder value(String field, Object value) {
        setState(BuilderState.VALUES);
        this.fields.add(field);
        this.values.add(value);
        addValueStatment = true;
        return this;
    }

    public InsertBuilder select(String select) {
        setState(BuilderState.SELECT);
        this.select = select;
        return this;
    }

    public InsertBuilder from(String from){
        setState(BuilderState.FROM);
        this.from = from;
        return this;
    }

    public InsertBuilder leftJoin(String join, Object... values) {
        setState(BuilderState.JOIN);
        this.joins.add("LEFT JOIN " + join);
        this.values.addAll(Arrays.asList(values));
        return this;
    }



    public InsertBuilder inOrg() {
        setState(BuilderState.VALUES);
        this.fields.add("org_id");
        this.values.add(OrganizationContext.orgId());
        return this;
    }

    public InsertBuilder inOrg(String field) {
        setState(BuilderState.VALUES);
        this.fields.add(field);
        this.values.add(OrganizationContext.orgId());
        return this;
    }

    public InsertBuilder where(String clause, Object value) {
        this.wheres.add(clause);
        values.add(value);
        return this;
    }

    public InsertBuilder where(String clause, Object... value) {
        this.wheres.add(clause);
        values.addAll(Arrays.asList(value));
        return this;
    }

    public InsertBuilder where(String clause) {
        this.wheres.add(clause);
        return this;
    }

    public InsertBuilder on(String on){
       setState(BuilderState.ON);
       this.on= on;
       return this;
    }


    @Override
    public String getSql() {
        setState(BuilderState.DONE);
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(table);
        sql.append(" (").append(String.join(", ", fields)).append(") ");
        if(isSet(select))
            sql.append(" SELECT ").append(select);
        if(isSet(from))
            sql.append(" FROM ").append(from);
        if(!joins.isEmpty())
            sql.append(" ").append(String.join(" ", joins));
        if(!wheres.isEmpty())
            sql.append(" WHERE ").append(String.join(" AND ", wheres));
        if(addValueStatment)
            sql.append(" VALUES (").append(fields.stream().map(f -> "?").collect(Collectors.joining(", "))).append(")");

        if(isSet(on))
            sql.append(" ON ").append(on);


        return sql.toString();
    }

    // ----- Private -----
    private void setState(BuilderState nextState) {
        if(nextState.compareTo(state) < 0)
            throw new IllegalStateException("Cannot " + nextState + " after " + state);

        state = nextState;
    }
}
