package org.realityforge.bazel.depgen.config;

public enum NameStrategy
{
  // Name is generated based on the groupId and artifactId of the artifact
  GroupIdAndArtifactId,
  // Name is generated based on the artifactId of the artifact
  ArtifactId
}
