package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.security.OrganizationContext;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.servantscode.commons.StringUtils.isSet;

public class InsertBuilder extends SqlBuilder {
    private static Logger LOG = LogManager.getLogger(QueryBuilder.class);

    private enum BuilderState {START, INTO, FIELDS, VALUES, SELECT, ON, DONE };

    private String table = null;
    private List<String> fields = new LinkedList<>();
    private String selectStatement = null;

    private String conflictResolution = null;

    private BuilderState state = BuilderState.START;

    public InsertBuilder() {}

    public InsertBuilder into(String table) {
        setState(BuilderState.INTO);
        this.table = table;
        return this;
    }

    //Manually select fields to add to. Meant to be used in conjunction with a query.
    //For standard field/values additions use value(key, value);
    public InsertBuilder fields(String... fields) {
        setState(BuilderState.FIELDS);
        this.fields.addAll(asList(fields));
        return this;
    }

    public InsertBuilder select(QueryBuilder query) {
        if(state == BuilderState.VALUES)
            throw new IllegalStateException("Cannot SELECT after VALUES");
        setState(BuilderState.SELECT);
        this.selectStatement = query.getSql();
        this.values.add(query.values);
        return this;
    }

    public InsertBuilder value(String field, Object value) {
        setState(BuilderState.VALUES);
        this.fields.add(field);
        this.values.add(value);
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

    public InsertBuilder onConflict(String resolution) {
        setState(BuilderState.ON);
        this.conflictResolution = resolution;
        return this;
    }

    @Override
    public String getSql() {
        setState(BuilderState.DONE);
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(table);
        sql.append(" (").append(String.join(", ", fields)).append(") ");
        if(isSet(selectStatement))
            sql.append(selectStatement);
        else
            sql.append(" VALUES (").append(fields.stream().map(f -> "?").collect(Collectors.joining(", "))).append(")");

        if(isSet(conflictResolution))
            sql.append(" ON CONFLICT " + conflictResolution);
        return sql.toString();
    }

    // ----- Private -----
    private void setState(BuilderState nextState) {
        if(nextState.compareTo(state) < 0)
            throw new IllegalStateException("Cannot " + nextState + " after " + state);

        state = nextState;
    }
}
