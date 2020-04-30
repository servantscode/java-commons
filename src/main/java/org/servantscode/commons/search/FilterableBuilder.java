package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.security.OrganizationContext;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class FilterableBuilder<T extends FilterableBuilder<T>> extends SqlBuilder {
    private static Logger LOG = LogManager.getLogger(FilterableBuilder.class);

    protected List<List<String>> ors = new LinkedList<>();
    protected List<String> wheres = new LinkedList<>();

    protected abstract void startFiltering();

    public FilterableBuilder() {}

    public T withId(int id) {
        startFiltering();
        this.wheres.add("id=?");
        values.add(id);
        return (T)this;
    }

    public T withId(long id) {
        startFiltering();
        this.wheres.add("id=?");
        values.add(id);
        return (T)this;
    }

    public T withIdIn(List<Integer> ids) {
        return withAny("id", ids);
//        startFiltering();
//        this.wheres.add(String.format("id in (%s)", ids.stream().map(id -> "?").collect(Collectors.joining(","))));
//        values.addAll(ids);
//        return (T)this;
    }

    public T with(String field, Object value) {
        startFiltering();
        this.wheres.add(field + "=?");
        values.add(value);
        return (T)this;
    }

    public T withAny(String field, List<? extends Object> ids) {
        startFiltering();
        this.wheres.add(String.format("%s in (%s)", field, ids.stream().map(id -> "?").collect(Collectors.joining(","))));
        values.addAll(ids);
        return (T)this;
    }

    public T or() {
        startFiltering();
        this.ors.add(this.wheres);
        this.wheres = new LinkedList<>();
        return (T)this;
    }

    public T where(String clause, Object value) {
        startFiltering();
        this.wheres.add(clause);
        values.add(value);
        return (T)this;
    }

    public T where(String clause, Object... value) {
        startFiltering();
        this.wheres.add(clause);
        values.addAll(Arrays.asList(value));
        return (T)this;
    }

    public T where(String clause) {
        startFiltering();
        this.wheres.add(clause);
        return (T)this;
    }

    public T whereIdIn(String field, QueryBuilder subselect) {
        startFiltering();
        this.wheres.add(String.format("%s IN (%s)", field, subselect.getSql()));
        values.add(subselect);
        return (T)this;
    }

    public T whereIdNotIn(String field, QueryBuilder subselect) {
        startFiltering();
        this.wheres.add(String.format("%s NOT IN (%s)", field, subselect.getSql()));
        values.add(subselect);
        return (T)this;
    }

    public T inOrg() {
        return inOrg("org_id", OrganizationContext.orgId());
    }

    public T inOrg(boolean includeSystem) {
        return includeSystem?
                inOrgOrSystem("org_id", OrganizationContext.orgId()):
                inOrg("org_id", OrganizationContext.orgId());
    }

    public T inOrg(String field) {
        return inOrg(field, OrganizationContext.orgId());
    }

    public T inOrg(String field, boolean includeSystem) {
        return includeSystem?
                inOrgOrSystem(field, OrganizationContext.orgId()):
                inOrg(field, OrganizationContext.orgId());
    }

    public T inOrg(int orgId) {
        return inOrg("org_id", orgId);
    }

    public T inOrg(String field, int orgId) {
        startFiltering();
        this.wheres.add(String.format("%s=?", field));
        values.add(orgId);
        return (T)this;
    }

    public T inOrgOrSystem(String field, int orgId) {
        startFiltering();
        this.wheres.add(String.format("(%s=? OR %s IS NULL)", field, field));
        values.add(orgId);
        return (T)this;
    }

    public T search(Search search) {
        startFiltering();
        if(search != null) {
            search.getClauses().forEach(clause -> {
                this.wheres.add(clause.getSql());
                this.values.addAll(clause.getValues());
            });
        }
        return (T)this;
    }
}
