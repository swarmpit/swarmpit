Swarmpit Roadmap
================

This document should be used as reference point for contributors and users to understand where the
project is going.

Release features and important dates are summarized in 
[milestone](https://github.com/swarmpit/swarmpit/milestones) section. <br />
All ongoing activities for particular release can be found in 
[project](https://github.com/swarmpit/swarmpit/projects) section.

If a feature isn't listed, please consider to create a new issue and we'll give you feedback as soon
as possible. Your feedback has the most important role in the Swarmpit future.

## How can I contribute?

As we're open source project, your contribution is very appreciated. If there is already an issue you
would like to help with, please leave a message in comment section and we'll be in touch for sure. If a new feature is asked, please create new issue describing the functionality.

But best way to get your feature in is by submitting pull-request, more details about that can be found in [CONTRIBUTING.md](CONTRIBUTING.md)

# Overview

Swarmpit is aimed to support Docker Swarm Mode and its abstraction. Therefore we won't be supporting  the majority of container API or any different orchestration platform. 
 
## What we did so far 
 
### 1.0 Release

The first official release was focusing mainly on usability and coverage of docker service API.

We introduced distribution API so users can easily browse across their Dockerhub accounts and
custom Docker registries. On the top of that, sharing these accounts with other Swarmpit users
can be done by simple publishing.

Deployment section offers auto-redeploy, which checks whether new version of service image has been
published and update the service automatically.

Swarmpit is trying to help users as much as possible so we prefetch placement filters based
on cluster settings and resource plugins as well.

Also, exposable ports were introduced.

### 1.1 Release

Service logs viewer was added in this version. Also service rollback and redeploy button has been introduced.

-  [[#20](https://github.com/swarmpit/swarmpit/issues/20)] Add service logging
-  [[#99](https://github.com/swarmpit/swarmpit/issues/99)] Allow to set services log-driver
-  [[#111](https://github.com/swarmpit/swarmpit/issues/111)] Add service rollback button
-  [[#101](https://github.com/swarmpit/swarmpit/issues/101)] Add service redeploy button

### 1.2 Release

This release was mainly aimed for swarmpit stabilization. We fixed couple of issues related to docker client
and stack inconsistency. Also all backend clients were switched from httpkit to clj-http mainly because SSL
termination issues. On top of that we introduced suggestable env variables, service resource management and
humanized time on FE.

- [[#134](https://github.com/swarmpit/swarmpit/issues/134)] Add service resource management
- [[#90](https://github.com/swarmpit/swarmpit/issues/90)] Suggest environment variables
- [[#138](https://github.com/swarmpit/swarmpit/issues/138)] Replace httpkit on server by clj-http
- [[#139](https://github.com/swarmpit/swarmpit/issues/139)] Show local times in FE

### 1.3 Release

Since 1.3 SSE support and event based rendering is supported. This means that data can be pushed directly
to FE based on received event immediately (if subscribed). To collect events from cluster nodes new component
was introduced into stack: event-collector. In order to have always actual data without refreshing please
do not remove this service from stack. Apart from that full-text search was introduced in list components. We
also added token registry fallback support so feel free to use your favourite v.2 registry such Gitlab. Finally
linked services are shown on resource (volume/network/secret/node) info page.

- [[#165](https://github.com/swarmpit/swarmpit/issues/165)] Add SSE support
- [[#183](https://github.com/swarmpit/swarmpit/issues/183)] Add view subscription for event based rendering
- [[#180](https://github.com/swarmpit/swarmpit/issues/180)] Add data config
- [[#157](https://github.com/swarmpit/swarmpit/issues/157)] Add token auth support for custom docker registry
- [[#113](https://github.com/swarmpit/swarmpit/issues/113)] Refresh data automatically on service detail page
- [[#115](https://github.com/swarmpit/swarmpit/issues/115)] Filter by more than just a name (id, node, port ...)
- [[#105](https://github.com/swarmpit/swarmpit/issues/105)] Show list of linked services in volume/network/secret ...
- [[#186](https://github.com/swarmpit/swarmpit/issues/186)] Material & State cleanup

### 1.4 Release

In 1.4 release we finally introduced application stacks. User can easily drag and drop existing compose file
into stack editor and create / update stack accordingly. Also stack rollback and redeploy functionality has
been introduced. In case of external stack user can simply link compose file by stack edit. Apart of that we
did quite a huge refactoring of application state so it's super easy to debug swarmpit in case of failure. 
Finally we enabled service auto redeploy for external services & added support for service update/rollback
deployment order.

- [[#39](https://github.com/swarmpit/swarmpit/issues/39)] Add application stacks.
- [[#207](https://github.com/swarmpit/swarmpit/issues/207)] Add progress btn for create forms instead page progress loader & cleanup local state
- [[#211](https://github.com/swarmpit/swarmpit/issues/211)] Enable autoredeploy for existing service
- [[#212](https://github.com/swarmpit/swarmpit/issues/212)] Add support for service update config order

### 1.5 Release

Stacks integration with the engine state was the main goal here. We are now officially supporting multiple ways
of editing compose either by current state, last deployed or previous one (rollback-ed). It allows seamless
transition between service & stack api. Event collector went deprecated. Dedicated agent that handle both
event & stats collecting is introduced instead. Please remove event-collector service once new stack deployed.
First stage of resource monitoring is out as well. You can now see live statistics of your running nodes & tasks.
User is offered with possibility to generate custom token for direct communication with Swarmpit backend API.

- [[#226](https://github.com/swarmpit/swarmpit/issues/226)] Generate stackfile dynamically from swarm state
- [[#227](https://github.com/swarmpit/swarmpit/issues/227)] Update stack based on service change
- [[#246](https://github.com/swarmpit/swarmpit/issues/246)] Create stack from service
- [[#225](https://github.com/swarmpit/swarmpit/issues/225)] Upgrade event-collector to fully fledged agent
- [[#98](https://github.com/swarmpit/swarmpit/issues/98)] Add swarm cluster resource monitoring
- [[#215](https://github.com/swarmpit/swarmpit/issues/215)] Generating token without expiration date

### 1.6 Release

We are pleased to announce first mobile-friendly docker swarm management UI that we worked on past couple
of months. Brand new Material UI 3.x components were introduce instead of old 0.x version with focus on
fully responsive tool you can work with on any device platform. Recharts are now being used to display 
real-time node & task statistics. New official couch image was introduced instead of deprecated claemo version.
Finally we prepared fancy installer for new swarmpit users. "Sea Wolfs" can still use good old docker stack
deployment to install swarmpit as we stay easy and true.

- [[#256](https://github.com/swarmpit/swarmpit/issues/256)] Mobile friendly UI
- [[#159](https://github.com/swarmpit/swarmpit/issues/159)] Upgrade material-ui components to NEXT once released
- [[#192](https://github.com/swarmpit/swarmpit/issues/192)] Setup single/cluster couch db
- [[#283](https://github.com/swarmpit/swarmpit/issues/283)] Add confirmation dialog box before deleting a resource

### 1.7 Release

Compatibility with custom registries from Cloud Providers and improve logs view experience were the priorities
in 1.7. New agent version `2.1` was introduced supporting container logs API so since 1.7 Swarmpit is fetching
logs via agent instead of service logs API which remains as fallback option in case of agent mallfunction. We're
now supporting 3 major container registries such Amazon ECR, Azure Container Registry & Gitlab.

- [[#236](https://github.com/swarmpit/swarmpit/issues/236)] AWS Elastic Container Registry support
- [[#331](https://github.com/swarmpit/swarmpit/issues/331)] Azure Container Registry support
- [[#341](https://github.com/swarmpit/swarmpit/issues/341)] Support for gitlab.com registry
- [[#260](https://github.com/swarmpit/swarmpit/issues/260)] Logs performance improvement

### 1.8 Release

Since 1.8 we finally supporting Swagger open API so integration with your CD should be really a piece of cake.
Influx support is in place and dashboard statistics overview was introduced as well. User can PIN important 
service/node to dashboard in order to have quick cluster overview on login. Support of custom links and immutable service
mode in service view is out. We introduced few UX improvements such option to deactivate stack, saving generated stack
for future use and ability to stop service on single click.

- [[#359](https://github.com/swarmpit/swarmpit/issues/359)] Introduce dashboard, Influx integration
- [[#359](https://github.com/swarmpit/swarmpit/issues/377)] Swagger API documentation
- [[#286](https://github.com/swarmpit/swarmpit/issues/286)] Save the generated stack file for future deployment
- [[#387](https://github.com/swarmpit/swarmpit/issues/387)] Pin service/node to dashboard 
- [[#264](https://github.com/swarmpit/swarmpit/issues/264)] Support custom links in service UI
- [[#316](https://github.com/swarmpit/swarmpit/issues/316)] Remove Nodes
- [[#347](https://github.com/swarmpit/swarmpit/issues/347)] Immutable service mode
- [[#267](https://github.com/swarmpit/swarmpit/issues/267)] Option to deactivate stack
- [[#358](https://github.com/swarmpit/swarmpit/issues/358)] Support for Custom v2 Registries

### 1.9 Release

In 1.9 we upgrade to material v4 which brings way new UI experience & forms. Swarmpit is ready to deploy to ARM 
based cluster thanks to @lumir-mrkva with custom compose file `docker-compose.arm.yml`. Last but not least we integrate
service plots and standartise dashboard metrics so user have better overview of cluster health without additional tooling.

- [[#411](https://github.com/swarmpit/swarmpit/issues/411)] Upgrade to material v4
- [[#415](https://github.com/swarmpit/swarmpit/issues/415)] Forms restyling (UX)
- [[#130](https://github.com/swarmpit/swarmpit/issues/130)] Running swarmpit on ARM
- [[#130](https://github.com/swarmpit/swarmpit/issues/446)] Integrate service plots and standardise metrics values

## What we will bring into the game

You can find here our goals and related features for future releases.

### 2.0 Release

The main goal is to introduce resource role management & service autoscalling. 

- [[#94](https://github.com/swarmpit/swarmpit/issues/94)] Add docker resource role management.
- [[#57](https://github.com/swarmpit/swarmpit/issues/57)] Integrate FaaS
- [[#288](https://github.com/swarmpit/swarmpit/issues/288)] Service autoscaling
- [[#289](https://github.com/swarmpit/swarmpit/issues/289)] Support cloud logging drivers
- [[#303](https://github.com/swarmpit/swarmpit/issues/303)] Dark mode

