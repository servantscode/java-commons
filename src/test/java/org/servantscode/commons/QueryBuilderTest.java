package org.servantscode.commons;

import org.junit.Test;
import org.servantscode.commons.search.QueryBuilder;

import static org.junit.Assert.assertEquals;

public class QueryBuilderTest {

    @Test
    public void testWithCte(){
        QueryBuilder queryBuilder = new QueryBuilder();
        String sql = queryBuilder.withCte("table1").select("e.id").from("entity e").getSql();

        assertEquals(sql, "WITH table1 SELECT e.id FROM entity e");
    }

}