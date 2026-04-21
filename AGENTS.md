# AGENTS.md

This file is a working guide for developers and coding agents contributing to
this repository. It focuses on architecture, local workflow, and the places
where changes usually need coordination.

## Repository Shape

This is a multi-module Maven repository:

- `cdkdepict-lib`
  - chemistry logic
  - Spring MVC controller endpoints
  - reaction mapping through RDT
  - tests
- `cdkdepict-webapp`
  - WAR packaging
  - Spring Boot entrypoint
  - Spring MVC configuration
  - static UI assets
- `docker`
  - container build files
  - container usage notes
- `docs`
  - endpoint contract notes
  - RDT integration notes
  - public mirror workflow notes
- `.github/workflows`
  - CI plus private-repo maintenance workflows

The unusual bit is that the main HTTP API lives in `cdkdepict-lib`, not in the
webapp module.

## Key Files

- `cdkdepict-lib/src/main/java/org/openscience/cdk/app/DepictController.java`
  - main request handler for `/depict`, `/react`, `/map`, and `/map/smi`
  - owns most rendering, transform, and mapping behavior
- `cdkdepict-lib/src/main/java/org/openscience/cdk/app/MolOp.java`
  - chemistry helper utilities such as radical and dative-bond handling
- `cdkdepict-webapp/src/main/java/org/openscience/cdk/app/Context.java`
  - MVC config and static resource wiring
- `cdkdepict-webapp/src/main/spring-boot/org/openscience/cdk/app/Application.java`
  - Spring Boot entrypoint for the bootable WAR
- `cdkdepict-webapp/src/main/webapp/WEB-INF/static/`
  - static HTML, JS, CSS, fonts, and images
- [`docs/depict-endpoint-agent.md`](docs/depict-endpoint-agent.md)
  - best source of truth for public endpoint parameters and behavior
- [`docs/RDT_CDK_INTEGRATION.md`](docs/RDT_CDK_INTEGRATION.md)
  - deeper notes on the RDT dependency and mapping flow
- [`docs/public-mirror.md`](docs/public-mirror.md)
  - private-to-public sanitized mirror workflow

## Architecture Summary

Request flow:

1. Static pages (`depict.html`, `react.html`, `map.html`) collect user input and
   build query strings in lightweight JavaScript.
2. Requests hit `DepictController`.
3. `DepictController` parses molecule or reaction input, applies options,
   generates coordinates if needed, and renders through CDK's
   `DepictionGenerator`.
4. For mapping endpoints, `DepictController` calls `RDT.map(...)`, then routes
   the mapped reaction back through the normal depict flow.
5. For transform endpoints, `DepictController` applies SMIRKS, synthesizes
   reaction objects for products, and then depicts those reactions.

Important boundaries:

- `cdkdepict-lib` is the behavioral core.
- `cdkdepict-webapp` is mostly packaging and UI shell.
- There is no frontend build pipeline. Changes to HTML or JS take effect
  directly in the WAR.

## Local Development

Baseline requirements:

- Java 25
- Maven 3.9+
- a Java-25-compatible local install of `com.bioinceptionlabs:rdt:4.0.0`

The CI workflow installs RDT from a pinned ReactionDecoder commit before the
main build. If local builds fail due to a missing or incompatible RDT artifact,
reinstall it from the same pinned ref used by CI:

```bash
RDT_REF=0d6632251108e7a8f625d15c60607a136093c4d8
git clone https://github.com/asad/ReactionDecoder.git /tmp/ReactionDecoder
git -C /tmp/ReactionDecoder checkout --detach "${RDT_REF}"
mvn -B -f /tmp/ReactionDecoder/pom.xml clean install -DskipTests=true
```

If the local Maven cache contains an older incompatible RDT build, remove:

```bash
rm -rf ~/.m2/repository/com/bioinceptionlabs/rdt/4.0.0
```

Common commands:

```bash
mvn -B clean test package
mvn -B -DskipTests compile
mvn -B -Dtest=DepictControllerTest test
java -Dserver.port=8081 -jar cdkdepict-webapp/target/cdkdepict-webapp-*.war
./run_depict.sh
```

## Tests And Change Safety

Primary tests:

- `DepictControllerTest`
- `ReactionCenterDetectionTest`
- `MolOpTest`

When changing behavior:

- add or adjust tests in `cdkdepict-lib/src/test/java/...`
- prefer targeted tests around the behavior you changed instead of broad
  snapshot-style assertions
- update `docs/depict-endpoint-agent.md` when request parameters, defaults, or
  response behavior change

There are currently no dedicated `cdkdepict-webapp` tests, so UI-shell changes
usually need manual verification.

## Editing Guidance

When touching the controller:

- keep route signatures stable unless the change is intentional
- prefer extracting helper methods over making `DepictController` more deeply
  nested
- preserve support for both molecule and reaction input paths
- be careful with shared options such as `annotate`, `abbr`, `hdisp`, `zoom`,
  `rotate`, `flip`, and color overrides because they affect multiple endpoints

When touching mapping logic:

- assume Java 25 is required because of the current RDT artifact line
- keep the agent-preserving `reactants>agents>products` handling intact
- preserve error-path behavior for invalid reaction mapping and invalid SMIRKS
  inputs

When touching the frontend:

- update the matching HTML and JS together
- remember there is no bundler, transpiler, or framework layer
- keep option names aligned with backend query parameter names

## CI And Release Notes

- `.github/workflows/maven.yml` is the public-safe CI workflow
- the private repo also contains deployment and mirror-maintenance workflows
- sanitized public mirror publishing is driven by
  `scripts/publish_public_mirror.sh` and `.public-mirror-excludes`

If you add new private-only files that should never be mirrored publicly, update
`.public-mirror-excludes` and `docs/public-mirror.md` in the same change.

## Suggested Doc Updates With Code Changes

Update these docs when the related area changes:

- `README.md`
  - setup, runtime requirements, module layout, developer entrypoints
- `docs/depict-endpoint-agent.md`
  - endpoint/query contract changes
- `docs/RDT_CDK_INTEGRATION.md`
  - mapping strategy, dependency, or integration changes
- `docs/public-mirror.md`
  - mirror workflow, secrets, branch strategy, or exclusions
