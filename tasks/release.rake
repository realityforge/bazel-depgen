require 'buildr/release_tool'

Buildr::ReleaseTool.define_release_task do |t|
  t.extract_version_from_changelog
  t.zapwhite
  t.ensure_git_clean
  t.verify_no_todo
  t.cleanup_staging
  t.build
  t.patch_changelog('realityforge/bazel-depgen')
  t.patch_maven_version_in_readme
  t.tag_project
  t.maven_central_publish
  t.patch_changelog_post_release
  t.push_changes
  t.github_release('realityforge/bazel-depgen')
end
