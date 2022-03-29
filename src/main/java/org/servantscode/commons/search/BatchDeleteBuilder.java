package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BatchDeleteBuilder<T extends Object> extends FilterableBuilder<BatchDeleteBuilder<T>> {
    private static Logger LOG = LogManager.getLogger(BatchDeleteBuilder.class);

    private enum BuilderState { START, TABLE, VALUES, WHERE, BATCHES, DONE };

    private String table = null;
    private List<Function<T, ? extends Object>> valueSource = new LinkedList<>();
    private List<T> batches = new LinkedList<>();

    private BuilderState state = BuilderState.START;

    public BatchDeleteBuilder() {}

    public BatchDeleteBuilder<T> deleteFrom(String table) {
        setState(BuilderState.TABLE);
        this.table = table;
        return this;
    }

    public BatchDeleteBuilder<T> with(String field, Object value) {
        startFiltering();
        if(value == null) {
            this.wheres.add(field + " IS NULL");
        } else {
            this.wheres.add(field + "=?");
            values.add(value);
        }
        return this;
    }

    public BatchDeleteBuilder<T> withSource(String field, Function<T, ? extends Object> source) {
        setState(BatchDeleteBuilder.BuilderState.WHERE);
        this.wheres.add(field + "=?");
        this.valueSource.add(source);
        return this;
    }


    public BatchDeleteBuilder<T> addBatch(T batch) {
        setState(BatchDeleteBuilder.BuilderState.BATCHES);
        this.batches.add(batch);
        return this;
    }

    public BatchDeleteBuilder<T> addBatches(List<T> batches) {
        setState(BatchDeleteBuilder.BuilderState.BATCHES);
        this.batches.addAll(batches);
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
        sql.append("DELETE FROM ").append(table);
        if(!ors.isEmpty())
            sql.append(" WHERE (")
               .append(ors.stream().map(wheres -> String.join(" AND ", wheres)).collect(Collectors.joining(") OR (")))
               .append(")");
         if(!wheres.isEmpty())
            sql.append(" WHERE ").append(String.join(" AND ", wheres));

        return sql.toString();
    }

    @Override
    protected void fillStatement(PreparedStatement stmt, AtomicInteger pos) {
        AtomicInteger batchCount = new AtomicInteger();
        values.forEach(value -> {
            setValue(stmt, pos, value);
        });

        int startPos = pos.get();
        batches.forEach(item -> {
            try {
                pos.set(startPos);
                valueSource.stream().map(source -> source.apply(item)).forEach(value -> setValue(stmt, pos, value));
                stmt.addBatch();
                batchCount.getAndIncrement();
            } catch (SQLException e) {
                throw new RuntimeException(String.format("Could not addBatch: %s at pos: %d\nsql: %s", batchCount.get(), pos.get() - 1, getSql()), e);
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
