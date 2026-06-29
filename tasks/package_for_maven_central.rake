desc 'Create Maven Central bundle'
task 'package_for_maven_central' do
  abort 'Maven Central packaging is now Bazel-based. Run: tools/package_maven_central.sh <version> [--gpg-key-id KEYID]'
end

desc 'Create Package and deploy to Maven Central'
task 'upload_to_maven_central' do
  abort 'Maven Central packaging is now Bazel-based. Run: tools/package_maven_central.sh <version> [--gpg-key-id KEYID]'
end
