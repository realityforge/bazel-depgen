package org.realityforge.bazel.depgen.model;

import static org.testng.Assert.*;

import java.util.Collections;
import java.util.List;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.realityforge.bazel.depgen.config.ReplacementConfig;
import org.realityforge.bazel.depgen.config.ReplacementTargetConfig;
import org.testng.annotations.Test;

public class ReplacementModelTest extends AbstractTest {
    @Test
    public void parseReplacementWithCoord() {
        final var source = new ReplacementConfig();
        source.setCoord("com.example:myapp");
        final var targetConfig = new ReplacementTargetConfig();
        targetConfig.setTarget("@com.example//:myapp");
        source.setTargets(Collections.singletonList(targetConfig));

        final ReplacementModel model = ReplacementModel.parse(source, OptionsConfig.DEFAULT_NATURE);
        assertEquals(model.getSource(), source);
        assertEquals(model.getGroup(), "com.example");
        assertEquals(model.getId(), "myapp");
        final List<ReplacementTargetModel> targets = model.getTargets();
        assertEquals(targets.size(), 1);
        final ReplacementTargetModel targetModel = targets.get(0);
        assertEquals(targetModel.getNature(), OptionsConfig.DEFAULT_NATURE);
        assertEquals(targetModel.getTarget(), targetConfig.getTarget());
    }

    @Test
    public void parse_missing_targets() {
        final var source = new ReplacementConfig();
        source.setCoord("com.example:myapp");

        final InvalidModelException exception = expectThrows(
                InvalidModelException.class, () -> ReplacementModel.parse(source, OptionsConfig.DEFAULT_NATURE));
        assertEquals(exception.getMessage(), "The replacement must specify the 'targets' property.");
        assertEquals(exception.getModel(), source);
    }

    @Test
    public void parse_missing_coord() {
        final var source = new ReplacementConfig();
        source.setTargets(Collections.emptyList());

        final InvalidModelException exception = expectThrows(
                InvalidModelException.class, () -> ReplacementModel.parse(source, OptionsConfig.DEFAULT_NATURE));
        assertEquals(exception.getMessage(), "The replacement must specify the 'coord' property.");
        assertEquals(exception.getModel(), source);
    }

    @Test
    public void parse_missing_target() {
        final var source = new ReplacementConfig();
        source.setCoord("com.example:myapp");
        final var targetConfig = new ReplacementTargetConfig();
        source.setTargets(Collections.singletonList(targetConfig));

        final InvalidModelException exception = expectThrows(
                InvalidModelException.class, () -> ReplacementModel.parse(source, OptionsConfig.DEFAULT_NATURE));
        assertEquals(exception.getMessage(), "The replacement target must specify the 'target' property.");
        assertEquals(exception.getModel(), targetConfig);
    }

    @Test
    public void parseReplacementWith1PartCoord() {
        final var source = new ReplacementConfig();
        source.setCoord("com.example");
        source.setTargets(Collections.emptyList());

        final InvalidModelException exception = expectThrows(
                InvalidModelException.class, () -> ReplacementModel.parse(source, OptionsConfig.DEFAULT_NATURE));
        assertEquals(
                exception.getMessage(),
                "The 'coord' property on the dependency must specify 2 components separated by the ':' character. The"
                        + " 'coords' must be in the form; 'group:id'.");
        assertEquals(exception.getModel(), source);
    }

    @Test
    public void parseReplacementWith3PartCoord() {
        final var source = new ReplacementConfig();
        source.setCoord("com.example:myapp:1.0");
        source.setTargets(Collections.emptyList());

        final InvalidModelException exception = expectThrows(
                InvalidModelException.class, () -> ReplacementModel.parse(source, OptionsConfig.DEFAULT_NATURE));
        assertEquals(
                exception.getMessage(),
                "The 'coord' property on the dependency must specify 2 components separated by the ':' character. The"
                        + " 'coords' must be in the form; 'group:id'.");
        assertEquals(exception.getModel(), source);
    }
}
