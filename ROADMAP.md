Swarmpit Roadmap
================

This document should be used as reference point for contributors and users to understand where the
project is going.

Release features and important dates are summarized in 
[milestone](https://github.com/swarmpit/swarmpit/milestones) section. <br />
All ongoing activities for particular release can be found in 
[project](https://github.com/swarmpit/swarmpit/projects) section.

If feature isn't listed, please consider to create new issue and we'll give you feedback as soon
as possible. Your feedback has the most important role in the Swarmpit future.

## How can I contribute?

As we're open source project, your contribution is very appreciated. If there is already an issue you
would like to help with, please leave a message in comment section and we'll be in touch for sure. If 
new feature is asked, please create new issue describing the functionality.

# Overview

Swarmpit is aimed to support Docker Swarm Mode and its abstraction. Therefore we won't be supporting 
majority of container API or any different orchestration platform. 
 
## What we did so far 
 
### 1.0 Release

First official release was focusing mainly on usability and coverage of docker service API.

We introduced distribution API so users can easily browse across their Dockerhub accounts and
custom Docker registries. On the top of that, sharing these accounts with other Swarmpit users
can be done by simple publishing.

Deployment section offers autoredeploy, which checks whether new version of service image has been
published and update the service automatically.

Swarmpit is trying to help users as much as possible so we prefetch placement filters based
on cluster settings and resource plugins as well.

Also exposable ports were introduced.

## What we will bring into the game

You can find here our goals and related features for future releases.

### 2.0 Release

Main goal is to introduce application stacks & resource role management.

- [[#39](https://github.com/swarmpit/swarmpit/issues/39)] Add application stacks.
- [[#20](https://github.com/swarmpit/swarmpit/issues/20)] Add service logging.
- [[#94](https://github.com/swarmpit/swarmpit/issues/94)] Add docker resource role management.