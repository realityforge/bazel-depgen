package org.realityforge.bazel.depgen.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.ReplacementConfig;
import org.realityforge.bazel.depgen.config.ReplacementTargetConfig;

public final class ReplacementModel {
    private final ReplacementConfig _source;

    private final String _group;

    private final String _id;

    private final List<ReplacementTargetModel> _targets;

    public static ReplacementModel parse(final ReplacementConfig source, final Nature defaultNature) {
        final String coord = source.getCoord();
        final List<ReplacementTargetConfig> targets = source.getTargets();
        if (null == targets) {
            throw new InvalidModelException("The replacement must specify the 'targets' property.", source);
        } else if (null == coord) {
            throw new InvalidModelException("The replacement must specify the 'coord' property.", source);
        } else {
            for (final ReplacementTargetConfig config : targets) {
                if (null == config.getTarget()) {
                    throw new InvalidModelException(
                            "The replacement target must specify the 'target' property.", config);
                }
            }

            final String[] components = coord.split(":");
            if (components.length != 2) {
                throw new InvalidModelException(
                        "The 'coord' property on the dependency must specify 2 components "
                                + "separated by the ':' character. The 'coords' must be in the form; "
                                + "'group:id'.",
                        source);
            } else {
                final List<ReplacementTargetModel> targetModels = targets.stream()
                        .map(target -> new ReplacementTargetModel(
                                target.getNature() == null ? defaultNature : target.getNature(),
                                Objects.requireNonNull(target.getTarget())))
                        .collect(Collectors.toList());
                return new ReplacementModel(source, components[0], components[1], targetModels);
            }
        }
    }

    private ReplacementModel(
            final ReplacementConfig source,
            final String group,
            final String id,
            final List<ReplacementTargetModel> targets) {
        _source = Objects.requireNonNull(source);
        _group = Objects.requireNonNull(group);
        _id = Objects.requireNonNull(id);
        _targets = Collections.unmodifiableList(Objects.requireNonNull(targets));
    }

    public ReplacementConfig getSource() {
        return _source;
    }

    public String getGroup() {
        return _group;
    }

    public String getId() {
        return _id;
    }

    public List<ReplacementTargetModel> getTargets() {
        return _targets;
    }

    /**
     * Return the replacement target associated with the specified nature.
     *
     * @param nature the nature.
     * @return the target.
     * @throws NullPointerException if no target found.
     */
    public String getTarget(final Nature nature) {
        return Objects.requireNonNull(findTarget(nature));
    }

    /**
     * Return the replacement target associated with the specified nature.
     *
     * @param nature the nature.
     * @return the target or null if no such target can be found.
     */
    @Nullable
    public String findTarget(final Nature nature) {
        return _targets.stream()
                .filter(target -> target.getNature() == nature)
                .map(ReplacementTargetModel::getTarget)
                .findAny()
                .orElse(null);
    }
}
