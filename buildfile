require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/top_level_generate_dir'

Buildr::MavenCentral.define_publish_tasks(:profile_name => 'org.realityforge', :username => 'realityforge')

MAVEN_RESOLVER = %w(
  org.apache.maven.resolver:maven-resolver-api:jar:1.3.3
  org.apache.maven.resolver:maven-resolver-spi:jar:1.3.3
  org.apache.maven.resolver:maven-resolver-util:jar:1.3.3
  org.apache.maven.resolver:maven-resolver-impl:jar:1.3.3
  org.apache.maven.resolver:maven-resolver-connector-basic:jar:1.3.3
  org.apache.maven.resolver:maven-resolver-transport-file:jar:1.3.3
  org.apache.maven.resolver:maven-resolver-transport-http:jar:1.3.3
  org.apache.httpcomponents:httpcore:jar:4.4.10
  org.apache.httpcomponents:httpclient:jar:4.5.6
  org.apache.maven:maven-resolver-provider:jar:3.5.0
  org.apache.commons:commons-lang3:jar:3.5
  commons-codec:commons-codec:jar:1.12
  org.apache.maven:maven-model:jar:3.5.0
  org.codehaus.plexus:plexus-utils:jar:3.0.24
  org.codehaus.plexus:plexus-interpolation:jar:1.25
  org.sonatype.plexus:plexus-sec-dispatcher:jar:1.4
  org.sonatype.plexus:plexus-cipher:jar:1.4
  org.apache.maven:maven-repository-metadata:jar:3.5.0
  org.codehaus.plexus:plexus-component-annotations:jar:1.7.1
  org.apache.maven:maven-model-builder:jar:3.5.0
  org.apache.maven:maven-builder-support:jar:3.5.0
  org.apache.maven:maven-artifact:jar:3.5.0
  org.apache.maven:maven-settings-builder:jar:3.5.0
  org.apache.maven:maven-settings:jar:3.5.0
  com.google.guava:guava:jar:20.0
  org.slf4j:slf4j-api:jar:1.7.25
  org.slf4j:slf4j-jdk14:jar:1.7.25
  org.slf4j:jcl-over-slf4j:jar:1.7.25
)

desc 'bazel-depgen: Generate Bazel dependency scripts by traversing Maven repositories'
define 'bazel-depgen' do
  project.group = 'org.realityforge.bazel.depgen'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/bazel-depgen')
  pom.add_developer('realityforge', 'Peter Donald')
  pom.dependency_filter = Proc.new {|_| false}

  compile.with :javax_annotation,
               :getopt4j,
               MAVEN_RESOLVER,
               :snakeyaml

  generate_config_resource(project)

  package(:jar)
  package(:sources)
  package(:javadoc)
  package(:jar, :classifier => 'all').tap do |jar|
    jar.with :manifest => {'Main-Class'=>'org.realityforge.bazel.depgen.Main'}
    compile.dependencies.each do |d|
      jar.merge(d)
    end
  end

  test.using :testng
  test.with :gir, :guiceyloops

  ipr.add_default_testng_configuration(jvm_args: '-ea')

  iml.excluded_directories << project._('tmp')

  ipr.add_component_from_artifact(:idea_codestyle)
end
