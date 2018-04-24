MicroService-AWS-S3-SDK


About

Rate limits are currently supported in a number of services, but these
limits are driven by configuration and require deployments to
adjust. Additionally these limits are uniform for all clients with no
flexibility. The goal of the service is to centrally manage a flexible set
of rate limits for the entire platform and at a more granular
level. Principal and device specific rateLimitOverrides should be
supported. For more information, please view the engineering
doc


Run with:

./gradlew :amnesty-web:run
Debug With:

./gradlew :amnesty-web:run --debug-jvm
Run Tests:

./gradlew:amnesty-web:test
