# Repository Instructions

## Versioning

- Every new user-requested delivery that changes source code, configuration, schemas, deployment, tests, or documentation must increment the project version in `pom.xml` exactly once.
- Use the version explicitly requested by the user. Otherwise apply SemVer: major for incompatible changes, minor for compatible features, and patch for compatible fixes.
- Never publish different code with a version that was already deployed.
- Do not change the Spring Boot parent or dependency versions when only the project version needs to be incremented.
- Keep release-specific operational documentation under `docs/vX.Y.Z/`.
- Verify the version with `mvn help:evaluate -Dexpression=project.version -q -DforceStdout` and confirm that Maven-generated `build-info.properties` contains the same version.

## Verification

- Run `mvn test` after backend, configuration, migration, or POM changes.
- Run `npm run build` from `admin-web` after frontend changes.
