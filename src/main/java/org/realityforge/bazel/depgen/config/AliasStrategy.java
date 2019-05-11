package org.realityforge.bazel.depgen.config;

public enum AliasStrategy
{
  // Alias is generated based on the groupId and artifactId of the artifact
  GroupIdAndArtifactId,
  // Alias is generated based on the artifactId of the artifact
  ArtifactId
}
