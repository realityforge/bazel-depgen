# How to Contribute

We'd love to accept your patches and contributions to this project. Pull requests are part of
what makes open source great. There are just a few small guidelines you need to follow.

## Code of Conduct

Participation in this project comes under the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md)

## Submitting code via Pull Requests

- We follow the [Github Pull Request Model](https://help.github.com/articles/about-pull-requests/) for
  all contributions.
- For large bodies of work, we recommend creating an issue outlining the feature that you wish to build,
  and describing how it will be implemented. This gives a chance for review to happen early, and ensures
  no wasted effort occurs.
- All submissions, will require review before being merged.
- Finally - *Thanks* for considering submitting code to the project!

## Formatting

When submitting pull requests, make sure to do the following:

- Maintain the same code style as the rest of the project.
- Remove trailing whitespace. Many editors will do this automatically.
- Ensure any new files have [a trailing newline](https://stackoverflow.com/questions/5813311/no-newline-at-end-of-file)

## How to speed the merging of pull requests

* Describe your changes in the CHANGELOG.md (if present).
* Give yourself some credit in the appropriate place (usually the CHANGELOG.md).
* Make commits of logical units.
* Ensure your commit messages help others understand what you are doing and why.
* Check for unnecessary whitespace with `git diff --check` before committing.
* Maintain the same code style.
* Maintain the same level of test coverage or improve it.

## Maven Central bundle

Run the full project verification before creating a release bundle:

```bash
tools/check.sh
```

Create the signed Maven Central upload zip with the wrapper:

```bash
GPG_USER=KEYID tools/package_maven_central.sh 1.2.3
```

The wrapper runs the release artifact build, the standalone all-jar integration test, and the dist assembly. Signing
matches the previous Buildr release flow: `GPG_USER` selects the key and optional `GPG_PASS` supplies the passphrase.
Pass `--gpg-key-id KEYID` to override `GPG_USER`. The raw commands are:

```bash
bazel build //tools/release:maven_artifacts --release_version=1.2.3
bazel test //tools/release:all_tests --release_version=1.2.3
GPG_USER=KEYID bazel run //tools/release:dist --release_version=1.2.3
```

The dist command writes the staged repository to `dist/bazel-depgen-1.2.3/` and the upload bundle to
`dist/bazel-depgen-1.2.3.zip`.

## Additional Resources

* [General GitHub documentation](http://help.github.com/)
* [How to write a good Git Commit message](https://chris.beams.io/posts/git-commit/) -
  Great way to make sure your Pull Requests get accepted.
* [An Open Source Etiquette Guidebook](https://css-tricks.com/open-source-etiquette-guidebook/#article-header-id-1)
