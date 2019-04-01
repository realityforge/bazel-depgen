def _artifact_impl(ctx):
    parts = ctx.attr.artifact.split(":")
    packaging = None
    if len(parts) == 4:
        packaging = parts[3]
    elif len(parts) == 5:
        packaging = parts[3]
    else:
        packaging = "jar"

    artifact_name = "%s.%s" % (ctx.name, packaging)
    ctx.download(
        output=ctx.path("artifacts/%s" % artifact_name),
        url=ctx.attr.urls,
        sha256=ctx.attr.sha256,
        executable=False
    )

    build_file_contents = """
package(default_visibility = ['//visibility:public'])

filegroup(
    name = 'file',
    srcs = [
        '{artifact_name}',
    ],
    visibility = ['//visibility:public']
)\n""".format(artifact_name = artifact_name)

    if len(ctx.attr.src_urls) != 0:
        src_name="%s-sources.%s" % (ctx.name, packaging)
        ctx.download(
            output = ctx.path("artifacts/%s" % src_name),
            url = ctx.attr.src_urls,
            sha256=ctx.attr.src_sha256,
            executable=False
        )
        build_file_contents += """

filegroup(
    name = 'src',
    srcs = [
        '{src_name}'
    ],
    visibility = ['//visibility:public']
)\n""".format(src_name = src_name)

    ctx.file(ctx.path("jar/BUILD"), build_file_contents, False)
    return None

artifact = repository_rule(
    attrs = {
        "artifact": attr.string(mandatory = True),
        "sha256": attr.string(mandatory = True),
        "urls": attr.string_list(mandatory = True),
        "src_sha256": attr.string(mandatory = False, default=""),
        "src_urls": attr.string_list(mandatory = False, default=[]),
    },
    implementation = _artifact_impl
)
