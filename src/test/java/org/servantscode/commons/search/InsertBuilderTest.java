package org.servantscode.commons.search;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InsertBuilderTest {

    @Test
    public void testInsertBuilder(){
        InsertBuilder cmd = new InsertBuilder().into("entity").value("col_1", 7);
        assertEquals("INSERT INTO entity (col_1)  VALUES (?)", cmd.getSql());
    }

    @Test
    public void testInsertBuilderSelect(){
        InsertBuilder cmd = new InsertBuilder().into("activity")
                .select("activity.id, p.id, false")
                .value("col_1", 7);
        assertEquals("INSERT INTO activity (col_1)  SELECT activity.id, p.id, false VALUES (?)", cmd.getSql());
    }

    // Test our normal case.
    @Test
    public void testInsertBuilder_1(){
        InsertBuilder cmd = new InsertBuilder().into("invite")
                .value("event_id",1 )
                .value("person_id", 2)
                .value("invited_by_id", 3)
                .value("invite_date", 4)
                .value("is_active", true);
        assertEquals("INSERT INTO invite (event_id, person_id, invited_by_id, invite_date, is_active)  VALUES (?, ?, ?, ?, ?)", cmd.getSql());
    }

    @Test
    public void testInsertBuilderWhere(){
        InsertBuilder cmd = new InsertBuilder().into("activity_read_status")
                .field("activity_id","person_id","is_read")
                .select("activity.id, p.id, FALSE")
                .from("person p")
                .leftJoin("affiliation a ON p.id = a.person_id AND a.status = 'ACTIVE'")
                .leftJoin("activity ON a.entity_id = activity.entity_id")
                .where("activity.id =?", 6)
                .on("CONFLICT (activity_id, person_id) DO NOTHING");

        assertEquals("INSERT INTO activity_read_status (activity_id, person_id, is_read)  " +
            "SELECT activity.id, p.id, FALSE " +
            "FROM person p " +
            "LEFT JOIN affiliation a ON p.id = a.person_id AND a.status = 'ACTIVE' " +
            "LEFT JOIN activity ON a.entity_id = activity.entity_id " +
            "WHERE activity.id =? " +
            "ON CONFLICT (activity_id, person_id) DO NOTHING", cmd.getSql());

    }
}
