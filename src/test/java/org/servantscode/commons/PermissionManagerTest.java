package org.servantscode.commons;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.servantscode.commons.security.PermissionManager.matches;

public class PermissionManagerTest {

    @Test
    public void testMatchesSame() {
        String userPerm = "people.list";
        String testPerm = "people.list";
        assertTrue("Same permission string should match", matches(userPerm, testPerm));
    }

    @Test
    public void testMatchesVeryLongSame() {
        String userPerm = "a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z";
        String testPerm = "a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z";
        assertTrue("Same permission string should match", matches(userPerm, testPerm));
    }

    @Test
    public void testNotMatchesDifferent() {
        String userPerm = "people.list";
        String testPerm = "metrics.view";
        assertFalse("Different permission string should not match", matches(userPerm, testPerm));
    }

    @Test
    public void testMatchesSystemWildcard() {
        String userPerm = "*";
        String testPerm = "people.list";
        assertTrue("Wildcard permission string should match", matches(userPerm, testPerm));
    }

    @Test
    public void testNotMatchesRequestedWildcard() {
        String userPerm = "people.list";
        String testPerm = "*";
        assertFalse("Wildcard permission as query string should match", matches(userPerm, testPerm));
    }

    @Test
    public void testMatchesWildcard() {
        String userPerm = "people.*";
        String testPerm = "people.list";
        assertTrue("Wildcard permission string should match", matches(userPerm, testPerm));
    }

    @Test
    public void testMatchesShorterPerm() {
        String userPerm = "people";
        String testPerm = "people.list";
        assertTrue("Shorter permission string should match", matches(userPerm, testPerm));
    }


    @Test
    public void testNotMatchesShorterRequest() {
        String userPerm = "people.list";
        String testPerm = "people";
        assertFalse("Shorter request string should not match", matches(userPerm, testPerm));
    }
}
