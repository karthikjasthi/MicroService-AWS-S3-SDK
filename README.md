Amnesty


About

Rate limits are currently supported in a number of services, but these
limits are driven by configuration and require deployments to
adjust. Additionally these limits are uniform for all clients with no
flexibility. The goal of Amnesty is to centrally manage a flexible set
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

Components of Amnesty

Rate limiting/Amnesty is made up of four components:


Amnesty API
route-common
amnesty-overrides
amnesty-sync



Amnesty API (This repo)

Git: https://git.vandevlab.com/iot/amnesty

Swagger: https://librarian-regionals.smartthingsgdev.com/api/amnesty.st.internal.v20180126#

The Amnesty API is a CRUD service that serves as a repository of all
rate limits. Please view the swagger for info on how to use the API.


route-common

Git: https://git.vandevlab.com/iot/route-common

route-common is a ratpack/dropwizard utility that polls amnesty every
"x" minutes and maintains a local cache of all the overrides for a
particular service. Please view platform-api for an example of a
service that uses route-common with amnesty. The polling frequency can
be controlled via the app config.


amnesty-overrides

Git: https://git.vandevlab.com/iot/amnesty-overrides

The amnesty-overrides is a repo of yaml files that contain the
overrides for each service. Upon commit to this repo, all the changes
are written out the amnesty API. Please view the README.md in the repo
for info on how to use it.


amnesty-sync

Git: https://git.vandevlab.com/iot/amnesty-sync

amnesty-sync is an AWS Lambda function that is invoked by the
amnesty-overrides repo. The Lambda function takes in a list of JSON
objects that consists of overrides and updates the amnesty api with
this information.


Workflow

Anyone wishing to use amnesty must first pull in
route-common into their ratpack/dropwizard project, and then update
the amnesty-overrides repo with their custom overrides. Once a commit
is made to the amnesty-overrides repo the amnesty-sync lambda is
invoked which then updates the amnesty API. Since route-common is
polling amnesty periodically, it'll pull in these changes in its next
polling cycle.
