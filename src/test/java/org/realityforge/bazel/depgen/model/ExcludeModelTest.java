package org.realityforge.bazel.depgen.model;

import static org.testng.Assert.*;

import java.util.UUID;
import org.testng.annotations.Test;

public class ExcludeModelTest {
    @Test
    public void createWithId() {
        final var group = randomString().replace("-", "_");
        final var id = randomString().replace("-", "_");
        final var model = ExcludeModel.parse(group + ":" + id);

        assertEquals(model.getGroup(), group);
        assertTrue(model.hasId());
        assertEquals(model.getId(), id);
    }

    @Test
    public void createWithoutId() {
        final var group = randomString().replace("-", "_");
        final var model = ExcludeModel.parse(group);

        assertEquals(model.getGroup(), group);
        assertFalse(model.hasId());
        assertNull(model.getId());
    }

    private static String randomString() {
        final var string = UUID.randomUUID().toString();
        return string.substring(0, Math.min(50, string.length()));
    }
}
