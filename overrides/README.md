# overrides

Build-time injection point for app resources, so private data never needs to
be committed here. Every file placed in this folder (except this README)
replaces the same-named file in
`cdkdepict-lib/src/main/resources/org/openscience/cdk/app/` when the image is
built with `docker/Dockerfile.jakarta`.

The intended use is a custom reagent (agent) abbreviation list:

```bash
cp path/to/custom.smi overrides/reagent_abbr.smi
docker build -f docker/Dockerfile.jakarta -t cdkdepict .
```

Format: one `SMILES<tab-or-space>label` per line, `#` for comments.
`reagent_abbr.smi` is loaded by `AbbreviationLoader`, which fails startup (and
so the tests run inside the Docker build) on invalid SMILES, missing labels or
2+ attachment points, naming the offending line. A molecule listed twice keeps
the later label and logs a warning.

Files here are gitignored but still part of the Docker build context, also
for `az acr build` (it honours only `.dockerignore`, which excludes just this
README).
