require 'buildr/git_auto_version'
require 'buildr/gpg'

desc 'bazel-depgen: Generate Bazel Dependencies scripts'
define 'bazel-depgen' do
  project.group = 'org.realityforge.bazel.depgen'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/bazel-depgen')
  pom.add_developer('realityforge', 'Peter Donald')

  manifest['Main-Class'] = 'org.realityforge.bazel.depgen.Main'

  compile.with :javax_annotation,
               :getopt4j,
               :snakeyaml

  package(:jar)
  package(:sources)
  package(:javadoc)
  package(:jar, :classifier => 'all').tap do |jar|
    compile.dependencies.each do |d|
      jar.merge(d)
    end
  end
end
