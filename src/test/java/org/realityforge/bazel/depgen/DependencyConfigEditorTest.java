package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.util.ArrayList;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.nodes.NodeTuple;

public class DependencyConfigEditorTest extends AbstractTest {
    @Test
    public void getTopLevelSequence_rejectsNonSequence() {
        final var entries = new ArrayList<NodeTuple>();
        entries.add(DependencyConfigEditor.tuple("artifacts", DependencyConfigEditor.scalarNode("value")));

        final DepgenValidationException exception = expectThrows(
                DepgenValidationException.class,
                () -> DependencyConfigEditor.getTopLevelSequence(
                        DependencyConfigEditor.mappingNode(entries), "artifacts", "remove"));
        assertEquals(exception.getMessage(), "The remove command only supports sequence-style artifacts.");
    }

    @Test
    public void scalarHelpers() {
        final var entries = new ArrayList<NodeTuple>();
        entries.add(DependencyConfigEditor.tuple("coord", DependencyConfigEditor.scalarNode("com.example:myapp:1.0")));
        final var mapping = DependencyConfigEditor.mappingNode(entries);

        assertEquals(DependencyConfigEditor.scalarMappingValue(mapping, "coord"), "com.example:myapp:1.0");
        assertNull(DependencyConfigEditor.scalarMappingValue(mapping, "missing"));
        assertNull(DependencyConfigEditor.scalarValue(mapping));
    }

    @Test
    public void loadRootMapping_rejectsNonMappingRoot() throws Exception {
        writeConfigFile("plain\n");

        final DepgenValidationException exception = expectThrows(
                DepgenValidationException.class, () -> DependencyConfigEditor.loadRootMapping(getDefaultConfigFile()));
        assertEquals(exception.getMessage(), "The dependency configuration must be a YAML mapping.");
    }
}
