package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.security.OrganizationContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BatchInsertBuilder<T extends Object> extends SqlBuilder {
    private static Logger LOG = LogManager.getLogger(QueryBuilder.class);

    private enum BuilderState {START, INTO, VALUES, BATCHES, DONE };

    private String table = null;
    private List<String> fields = new LinkedList<>();
    private List<Function<T, ? extends Object>> valueSource = new LinkedList<>();
    private List<T> batches = new LinkedList<>();

    private BuilderState state = BuilderState.START;

    public BatchInsertBuilder() {}

    public BatchInsertBuilder<T> into(String table) {
        setState(BuilderState.INTO);
        this.table = table;
        return this;
    }

    public BatchInsertBuilder<T> value(String field, Object value) {
        setState(BuilderState.VALUES);
        this.fields.add(field);
        this.values.add(value);
        return this;
    }

    public BatchInsertBuilder<T> valueSource(String field, Function<T, ? extends Object> source) {
        setState(BuilderState.VALUES);
        this.fields.add(field);
        this.valueSource.add(source);
        return this;
    }

    public BatchInsertBuilder<T> inOrg() {
        setState(BuilderState.VALUES);
        this.fields.add("org_id");
        this.values.add(OrganizationContext.orgId());
        return this;
    }

    public BatchInsertBuilder<T> inOrg(String field) {
        setState(BuilderState.VALUES);
        this.fields.add(field);
        this.values.add(OrganizationContext.orgId());
        return this;
    }

    public BatchInsertBuilder<T> addBatch(T batch) {
        setState(BuilderState.BATCHES);
        this.batches.add(batch);
        return this;
    }

    public BatchInsertBuilder<T> addBatches(List<T> batches) {
        setState(BuilderState.BATCHES);
        this.batches.addAll(batches);
        return this;
    }

    @Override
    public String getSql() {
        setState(BuilderState.DONE);
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(table);
        sql.append(" (").append(String.join(", ", fields)).append(") ");
        sql.append(" VALUES (").append(fields.stream().map(f -> "?").collect(Collectors.joining(", "))).append(")");
        return sql.toString();
    }

    @Override
    protected void fillStatement(PreparedStatement stmt, AtomicInteger pos) {
        AtomicInteger batchCount = new AtomicInteger();
        int startPos = pos.get();
        batches.forEach(item -> {
            try {
                pos.set(startPos);
                valueSource.stream().map(source -> source.apply(item)).forEach(value -> setValue(stmt, pos, value));
                stmt.addBatch();
                batchCount.getAndIncrement();
            } catch (SQLException e) {
                throw new RuntimeException(String.format("Could addBatch: %s at pos: %d\nsql: %s", batchCount.get(), pos.get() - 1, getSql()), e);
            }
        });
    }

    // ----- Private -----
    private void setState(BuilderState nextState) {
        if(nextState.compareTo(state) < 0)
            throw new IllegalStateException("Cannot " + nextState + " after " + state);

        state = nextState;
    }
}
