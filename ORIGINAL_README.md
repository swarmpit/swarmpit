[![swarmpit](https://raw.githubusercontent.com/swarmpit/swarmpit/master/resources/public/img/logo.svg?sanitize=true)](https://swarmpit.io)

Lightweight mobile-friendly Docker Swarm management UI

[![version](https://img.shields.io/github/release-pre/swarmpit/swarmpit.svg)](https://github.com/swarmpit/swarmpit/releases) 
[![gitter](https://badges.gitter.im/trezor/community.svg)](https://gitter.im/swarmpit_io/swarmpit) 
[![Build Status](https://travis-ci.org/swarmpit/swarmpit.svg?branch=master)](https://travis-ci.org/swarmpit/swarmpit)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/swarmpit/swarmpit/pulls)
[![Financial Contributors on Open Collective](https://opencollective.com/swarmpit/all/badge.svg?label=financial+contributors)](https://opencollective.com/swarmpit) 

[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/fold_left.svg?style=social&label=Follow%20%40swarmpit_io)](https://twitter.com/swarmpit_io)

<img src="https://raw.githubusercontent.com/swarmpit/swarmpit/master/resources/public/imac.png">

Swarmpit provides simple and easy to use interface for your Docker Swarm cluster. You can manage your stacks, services, secrets, volumes, networks etc. After linking your Docker Hub account or custom registry, private repositories can be easily deployed on Swarm. Best of all, you can share this management console securely with your whole team.

More details about future and past releases can be found in [ROADMAP.md](ROADMAP.md)

[![opencollective](https://opencollective.com/swarmpit/tiers/backers.svg?avatarHeight=50)](https://opencollective.com/swarmpit)

## Installation

The only dependency for Swarmpit deployment is Docker with Swarm initialized, we are supporting Docker 1.13 and newer. Linux hosts on x86 and ARM architectures are supported as well.

### Package installer
Installer is your guide to setup Swarmpit platform. For more details see the [installer](https://github.com/swarmpit/installer)

#### Stable version
Deploy our current milestone version

```
docker run -it --rm \
  --name swarmpit-installer \
  --volume /var/run/docker.sock:/var/run/docker.sock \
  swarmpit/install:1.9
```

#### Edge version
Deploy latest version for the brave and true

```
docker run -it --rm \
  --name swarmpit-installer \
  --volume /var/run/docker.sock:/var/run/docker.sock \
  swarmpit/install:edge
```
### Manual installation
Deploy Swarmpit by using a compose file from our git repo with branch of corresponding version.

```
git clone https://github.com/swarmpit/swarmpit -b master
docker stack deploy -c swarmpit/docker-compose.yml swarmpit
```

For ARM based cluster use custom compose file.

```
git clone https://github.com/swarmpit/swarmpit -b master
docker stack deploy -c swarmpit/docker-compose.arm.yml swarmpit
```

[This stack](docker-compose.yml) is a composition of 4 services:

* app - Swarmpit
* [agent](https://github.com/swarmpit/agent) - Swarmpit agent
* db - CouchDB (Application data)
* influxdb - InfluxDB (Cluster statistics)

Feel free to edit the stackfile to change an application port and we strongly recommend to specify following volumes:

* db-data 
* influxdb-data 

to shared-volume driver type of your choice. Alternatively, you can link db service to the specific node by using [constraint](https://docs.docker.com/compose/compose-file/#placement).

Swarmpit is published on port `888` by default.

## Configuration

Refer to following [document](https://github.com/swarmpit/swarmpit/blob/master/doc/configuration.md) 

## Development

Swarmpit is written purely in Clojure and utilizes React on front-end. CouchDB is used to persist application data & InfluxDB for cluster statistics.

Everything about building, issue reporting and setting up development environment can be found in [CONTRIBUTING.md](CONTRIBUTING.md)

## Demo

[![Try in PWD](https://cdn.rawgit.com/play-with-docker/stacks/cff22438/assets/images/button.png)](http://play-with-docker.com?stack=https://raw.githubusercontent.com/swarmpit/swarmpit/master/docker-compose.yml) 

Deploys Swarmpit to play-with-docker sandbox. Please wait few moments till application is up and running before accessing
port 888. Initialization might take a few seconds.

## Contributors

### Code Contributors

This project exists thanks to all the people who contribute. [[Contribute](CONTRIBUTING.md)].
<a href="https://github.com/swarmpit/swarmpit/graphs/contributors"><img src="https://opencollective.com/swarmpit/contributors.svg?width=890&button=false" /></a>

### Financial Contributors

Become a financial contributor and help us sustain our community. [[Contribute](https://opencollective.com/swarmpit/contribute)]

#### Individuals

<a href="https://opencollective.com/swarmpit"><img src="https://opencollective.com/swarmpit/individuals.svg?width=890"></a>

#### Organizations

Support this project with your organization. Your logo will show up here with a link to your website. [[Contribute](https://opencollective.com/swarmpit/contribute)]

<a href="https://opencollective.com/swarmpit/organization/0/website"><img src="https://opencollective.com/swarmpit/organization/0/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/1/website"><img src="https://opencollective.com/swarmpit/organization/1/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/2/website"><img src="https://opencollective.com/swarmpit/organization/2/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/3/website"><img src="https://opencollective.com/swarmpit/organization/3/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/4/website"><img src="https://opencollective.com/swarmpit/organization/4/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/5/website"><img src="https://opencollective.com/swarmpit/organization/5/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/6/website"><img src="https://opencollective.com/swarmpit/organization/6/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/7/website"><img src="https://opencollective.com/swarmpit/organization/7/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/8/website"><img src="https://opencollective.com/swarmpit/organization/8/avatar.svg"></a>
<a href="https://opencollective.com/swarmpit/organization/9/website"><img src="https://opencollective.com/swarmpit/organization/9/avatar.svg"></a>
