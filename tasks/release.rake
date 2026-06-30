def abort_release_flow_replaced
  abort <<~MESSAGE
    The Ruby release flow has been replaced.

    Run the all-in-one release workflow:
      tools/release/perform_release.sh <version>

    Or rerun individual split steps:
      tools/release/check_ready.sh
      tools/release/next_version.sh
      tools/release/prepare_release.sh <version> [--dry-run]
      tools/package_maven_central.sh <version>
      tools/release/upload_maven_central.sh <version>
      tools/release/finalize_release.sh <version>

    See tools/release/README.md for the Maven Central release workflow.
  MESSAGE
end

desc 'Release bazel-depgen'
task 'release' do
  abort_release_flow_replaced
end

desc 'Release bazel-depgen'
task 'perform_release' do
  abort_release_flow_replaced
end
