# CDK Depict

CDK Depict is a Java 25 Spring MVC application for rendering molecules and
reactions from SMILES, molfiles, and mapped reaction inputs. In addition to
plain depiction it supports SMARTS highlighting, reaction-center annotation,
SMIRKS-based transforms, and reaction mapping through RDT (Reaction Decoder
Tool).

The project is organized as a multi-module Maven build:

- `cdkdepict-lib`: core depiction logic, HTTP endpoints, reaction mapping, and
  tests
- `cdkdepict-webapp`: Spring Boot WAR packaging plus the static `depict`,
  `react`, and `map` frontends
- `docker`: container build files and container-specific notes
- `docs`: developer notes for endpoint contracts, RDT integration, and the
  private-to-public mirror workflow

## Features

- Depict molecules and reactions in `svg`, `pdf`, `png`, `jpg`, and `gif`
- Highlight SMARTS matches and explicit atom/bond subsets
- Annotate atom numbers, atom maps, CIP labels, and reaction-center changes
- Apply SMIRKS transforms through `/react/{style}/{fmt}`
- Map reaction SMILES with RDT through `/map/{style}/{fmt}` and `/map/smi`
- Serve a simple static UI with no frontend build step

## Requirements

- Java 25
- Maven 3.9+
- A local `com.bioinceptionlabs:rdt:4.0.0` artifact built with Java 25

The CI workflow installs RDT from a pinned ReactionDecoder commit before
building this repo. If your local machine does not already have a compatible
RDT artifact in `~/.m2`, install it first:

```bash
RDT_REF=0d6632251108e7a8f625d15c60607a136093c4d8
git clone https://github.com/asad/ReactionDecoder.git /tmp/ReactionDecoder
git -C /tmp/ReactionDecoder checkout --detach "${RDT_REF}"
mvn -B -f /tmp/ReactionDecoder/pom.xml clean install -DskipTests=true
```

If you previously installed an incompatible RDT build, remove
`~/.m2/repository/com/bioinceptionlabs/rdt/4.0.0` and reinstall it with Java
25.

## Build And Run

Build the full project:

```bash
mvn -B clean test package
```

Run the bootable WAR locally:

```bash
java -Dserver.port=8081 -jar cdkdepict-webapp/target/cdkdepict-webapp-*.war
```

There is also a tiny helper script:

```bash
./run_depict.sh
```

By default the UI is available at [http://localhost:8081/depict.html](http://localhost:8081/depict.html)
when using the direct `java` command, or at port `8001` when using
`run_depict.sh`.

## API Overview

Main routes:

- `GET /depict/{style}/{fmt}?smi=...`
- `GET /react/{style}/{fmt}?smi=...&smirks=...`
- `GET /map/{style}/{fmt}?smi=...`
- `GET /map/smi?smi=...`

Examples:

```text
/depict/cot/svg?smi=CCO
/depict/bot/svg?smi=[CH3:1][CH3:2]>>[CH2:1]=[CH2:2]&annotate=colmap
/react/cot/svg?smi=CCO&smirks=[C:1]-[O:2]>>[C:1]-[N:2]
/map/smi?smi=CC(=O)O.OCC>>CC(=O)OCC.O
```

The detailed depict endpoint contract, supported styles, query parameters, and
response behavior live in [docs/depict-endpoint-agent.md](docs/depict-endpoint-agent.md).

## Architecture

At a high level:

- [`DepictController`](cdkdepict-lib/src/main/java/org/openscience/cdk/app/DepictController.java)
  owns the main HTTP API and nearly all depiction, reaction-transform, and
  reaction-mapping logic.
- [`MolOp`](cdkdepict-lib/src/main/java/org/openscience/cdk/app/MolOp.java)
  contains chemistry-specific helpers such as radical and dative-bond handling.
- [`Context`](cdkdepict-webapp/src/main/java/org/openscience/cdk/app/Context.java)
  wires Spring MVC resources and view resolution.
- [`Application`](cdkdepict-webapp/src/main/spring-boot/org/openscience/cdk/app/Application.java)
  is the Spring Boot entrypoint for the bootable WAR.
- `cdkdepict-webapp/src/main/webapp/WEB-INF/static/` holds the static HTML,
  CSS, and small jQuery-based frontends for depict, react, and map modes.

Two architecture details matter for contributors:

- The "library" module is not just chemistry helpers. It also contains the HTTP
  controller and most request-handling logic.
- The map flow delegates reaction mapping to RDT, then feeds the mapped reaction
  back through the same depiction pipeline used by the normal depict endpoint.

For deeper background on the RDT integration, see
[docs/RDT_CDK_INTEGRATION.md](docs/RDT_CDK_INTEGRATION.md).

## Development Notes

- CI is defined in [`.github/workflows/maven.yml`](.github/workflows/maven.yml)
  and uses Java 25 plus a pinned ReactionDecoder checkout.
- The private repo can publish a sanitized public mirror with
  [`scripts/publish_public_mirror.sh`](scripts/publish_public_mirror.sh).
  See [docs/public-mirror.md](docs/public-mirror.md).
- Container notes live in
  [docker/README.md](docker/README.md).

Useful local commands:

```bash
mvn -B test
mvn -B -DskipTests compile
mvn -B -Dtest=DepictControllerTest test
```

## Tests

Current tests live in `cdkdepict-lib`:

- `DepictControllerTest`: endpoint behavior and rendering-oriented coverage
- `ReactionCenterDetectionTest`: reaction-center detection and fallback logic
- `MolOpTest`: lower-level chemistry helpers

There are currently no dedicated `cdkdepict-webapp` tests.

## License

This project is licensed under LGPL v2.1 or later. See [LICENSE](LICENSE).
