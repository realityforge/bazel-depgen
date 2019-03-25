# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Add validate phase against `ArtifactConfig` that will:
  - verify coord to ensure that is in correct format
  - verify coords is not present with other spec components and set other spec components when parsing coord.
  - verify excludes to ensure that they contain at most 1-2 spec components (i.e. group + optional id) and
    create inner model object to represent exclude

* Add config section to dependencies that determines the output format of tool. This includes things like
  name of function to add symbols, prefixes for names, whether to support `omit_*` config. This will
  unfortunately result in the following config moving into the config file
  -  WORKSPACE_DIR_OPT
  - EXTENSION_FILE_OPT
