desc 'Create Maven Central bundle'
task 'package_for_maven_central' do
  abort 'Maven Central packaging is script-based. Run: tools/package_maven_central.sh <version> [--gpg-key-id KEYID]. See tools/release/README.md.'
end

desc 'Create Package and deploy to Maven Central'
task 'upload_to_maven_central' do
  abort 'Maven Central upload is script-based. Run: tools/package_maven_central.sh <version>, then tools/release/upload_maven_central.sh <version>. See tools/release/README.md.'
end
