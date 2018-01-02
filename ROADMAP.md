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

## What we will bring into the game

You can find here our goals and related features for future releases.

### 1.4 Release

The main goal is to introduce application stacks.

- [[#39](https://github.com/swarmpit/swarmpit/issues/39)] Add application stacks.

### 2.0 Release

The main goal is to introduce FaaS & resource role management.

- [[#94](https://github.com/swarmpit/swarmpit/issues/94)] Add docker resource role management.
- [[#57](https://github.com/swarmpit/swarmpit/issues/57)] Integrate FaaS
