package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.servantscode.commons.StringUtils.isSet;

public class QueryBuilder extends FilterableBuilder<QueryBuilder> {
    private static Logger LOG = LogManager.getLogger(QueryBuilder.class);

    private enum BuilderState {START, WITH_CTE, SELECT, FROM, JOIN, WHERE, GROUP, SORT, LIMIT, OFFSET, DONE};

    private String with = null;
    private List<String> selections = new LinkedList<>();
    private List<String> tables = new LinkedList<>();
    private List<String> joins = new LinkedList<>();

    private List<String> groupBy = new LinkedList<>();
    private String sort;
    private boolean limit;
    private boolean offset;
    private boolean distinct;

    private BuilderState state = BuilderState.START;

    public QueryBuilder() {
    }


    public QueryBuilder withCte(String with, Object... values) {
        return withCte(with, asList(values));
    }
    public QueryBuilder withCte(String with, List<Object> values){
        setState(BuilderState.WITH_CTE);
        this.with = with;
        this.values.add(values);
        return this;
    }

    public QueryBuilder select(String... selections) {
        return select(asList(selections));
    }

    public QueryBuilder select(List<String> selections) {
        setState(BuilderState.SELECT);
        this.selections.addAll(selections);
        return this;
    }

    public QueryBuilder distinct() {
        setState(BuilderState.SELECT);
        distinct = true;
        return this;
    }

    public QueryBuilder selectWith(String selection, Object... values) {
        setState(BuilderState.SELECT);
        this.selections.add(selection);
        this.values.addAll(asList(values));
        return this;
    }

    public QueryBuilder from(String... tables) {
        setState(BuilderState.FROM);
        this.tables.addAll(asList(tables));
        return this;
    }

    public QueryBuilder from(Function<QueryBuilder, QueryBuilder> tableSelector) {
        setState(BuilderState.FROM);
        return tableSelector.apply(this);
    }

    public QueryBuilder from(QueryBuilder query, String alias) {
        setState(BuilderState.FROM);
        this.tables.add(String.format("(%s) %s", query.getSql(), alias));
        this.values.add(query);
        return this;
    }

    public QueryBuilder apply(Function<QueryBuilder, QueryBuilder> queryModifier) {
        return queryModifier.apply(this);
    }

    public QueryBuilder join(String... joins) {
        setState(BuilderState.JOIN);
        this.joins.addAll(asList(joins));
        return this;
    }

    public QueryBuilder leftJoin(String join, Object... values) {
        setState(BuilderState.JOIN);
        this.joins.add("LEFT JOIN " + join);
        this.values.addAll(Arrays.asList(values));
        return this;
    }

    public QueryBuilder leftJoin(QueryBuilder query, String alias, String joinOn) {
        this.setState(BuilderState.JOIN);
        this.joins.add(String.format("LEFT JOIN (%s) %s ON %s", query.getSql(), alias, joinOn));
        this.values.add(query);
        return this;
    }

    public QueryBuilder innerJoin(String join, Object... values) {
        setState(BuilderState.JOIN);
        this.joins.add("INNER JOIN " + join);
        this.values.addAll(Arrays.asList(values));
        return this;
    }

    public QueryBuilder innerJoin(QueryBuilder query, String alias, String joinOn) {
        this.setState(BuilderState.JOIN);
        this.joins.add(String.format("INNER JOIN (%s) %s ON %s", query.getSql(), alias, joinOn));
        this.values.add(query);
        return this;
    }

    public QueryBuilder leftJoinLateral(QueryBuilder query, String alias, String joinOn) {
        this.setState(BuilderState.JOIN);
        this.joins.add(String.format("LEFT JOIN LATERAL (%s) %s ON %s", query.getSql(), alias, joinOn));
        this.values.add(query);
        return this;
    }

    public QueryBuilder fullOuterJoin(QueryBuilder query, String alias, String joinOn) {
        this.setState(BuilderState.JOIN);
        this.joins.add(String.format("FULL OUTER JOIN (%s) %s ON %s", query.getSql(), alias, joinOn));
        this.values.add(query);
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
        this.sort = (searchParser != null)? searchParser.translateSort(sort): sort;
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
        if(isSet(with))
            sql.append("WITH ").append(with).append(" ");
        sql.append("SELECT ");
        if(distinct)
            sql.append("DISTINCT ");
        sql.append(String.join(", ", selections));
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
            sql.append(" ORDER BY ").append(sort);
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
