package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.util.Collections;
import java.util.List;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.graph.Dependency;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.Test;

public class OptionalDependencySelectorTest extends AbstractTest {
    @Test
    public void selectDependency_alwaysSelectsDependency() throws Exception {
        writeConfigFile("");

        assertTrue(new OptionalDependencySelector(loadApplicationModel()).selectDependency(dependency(false)));
    }

    @Test
    public void deriveChildSelector_withoutDependency_returnsSelf() throws Exception {
        writeConfigFile("");

        final var selector = new OptionalDependencySelector(loadApplicationModel());

        assertSame(selector.deriveChildSelector(context(null)), selector);
    }

    @Test
    public void deriveChildSelector_forNonOptionalDependency_returnsSelf() throws Exception {
        writeConfigFile("");

        final var selector = new OptionalDependencySelector(loadApplicationModel());

        assertSame(selector.deriveChildSelector(context(dependency(false))), selector);
    }

    @Test
    public void deriveChildSelector_forUnconfiguredOptionalDependency_rejectsDependency() throws Exception {
        writeConfigFile("");

        final var selector = new OptionalDependencySelector(loadApplicationModel());

        assertSame(selector.deriveChildSelector(context(dependency(true))), RejectDependencySelector.INSTANCE);
    }

    @Test
    public void deriveChildSelector_forConfiguredOptionalDependencyWithoutIncludeOptional_rejectsDependency()
            throws Exception {
        writeConfigFile("artifacts:\n  - coord: com.example:child:1.0\n");

        final var selector = new OptionalDependencySelector(loadApplicationModel());

        assertSame(selector.deriveChildSelector(context(dependency(true))), RejectDependencySelector.INSTANCE);
    }

    @Test
    public void deriveChildSelector_forConfiguredOptionalDependencyWithIncludeOptional_returnsSelf() throws Exception {
        writeConfigFile("artifacts:\n  - coord: com.example:child:1.0\n    includeOptional: true\n");

        final var selector = new OptionalDependencySelector(loadApplicationModel());

        assertSame(selector.deriveChildSelector(context(dependency(true))), selector);
    }

    @NonNull
    private static Dependency dependency(final boolean optional) {
        return new Dependency(new DefaultArtifact("com.example:child:jar:1.0"), "compile", optional);
    }

    @NonNull
    private static DependencyCollectionContext context(@Nullable final Dependency dependency) {
        return new DependencyCollectionContext() {
            @Override
            public RepositorySystemSession getSession() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Artifact getArtifact() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nullable Dependency getDependency() {
                return dependency;
            }

            @Override
            public List<Dependency> getManagedDependencies() {
                return Collections.emptyList();
            }
        };
    }
}
