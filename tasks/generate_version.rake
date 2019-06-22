def generate_version_resource(project)
  generated_dir = project._(:target, :generated, :versions, :main, :resources)
  project.iml.main_resource_directories << generated_dir
  t = project.file(generated_dir) do
    rm_rf generated_dir
    base_dir = "#{generated_dir}/#{project.group.gsub('.', '/')}"
    mkdir_p base_dir
    File.write("#{base_dir}/version.txt", project.version)
  end
  desc 'Generate version resource file'
  project.task(':version:generate' => [t.name])
  project.resources.from(generated_dir)
end