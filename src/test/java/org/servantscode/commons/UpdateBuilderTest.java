package org.servantscode.commons;

import org.junit.Test;
import org.servantscode.commons.search.UpdateBuilder;

import static org.junit.Assert.assertEquals;

public class UpdateBuilderTest {

    @Test
    public void testWithCte(){
        UpdateBuilder updateBuilder = new UpdateBuilder();
        String sql = updateBuilder.withCte("table1").update("entity").getSql();

        assertEquals(sql, "WITH table1 UPDATE entity");
    }

}
