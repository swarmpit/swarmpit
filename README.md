[![swarmpit](http://swarmpit.io/img/logo-swarmpit.svg)](http://swarmpit.io)

Lightweight Docker Swarm orchestration.

![screenshot](http://swarmpit.io/img/example.jpg)

Swarmpit provides simple and easy to use interface for your Docker Swarm cluster. You can manage your services, secrets, volumes, networks etc. After linking your Docker Hub account or custom registry, private repositories can be easily deployed on Swarm. Best of all, you can share this management console securely with your whole team.

We have more features coming like automatic redeployments, stack management, user permissions constraints and more, so stay tuned or even better help us shape features you would like.

## Installation

The only dependency for Swarmpit deployment is Docker with Swarm initialized, we are supporting Docker 1.13 and newer.

The simplest way to deploy Swarmpit is by using a Compose file from our git repo.

```
git clone https://github.com/swarmpit/swarmpit
docker stack deploy -c swarmpit/docker-compose.yml swarmpit
```

[This stack](docker-compose.yml) is a composition of Swarmpit and CouchDB. Feel free to edit the stackfile to change a port on which will be Swarmpit published and we're strongly recommending you to specify `db-data` volume driver to shared-volume driver of your choice. Alternatively, you can link db service to the specific node by using [constraint](https://docs.docker.com/compose/compose-file/#placement).

Swarmpit is published on port `888` by default and you can sign in with user/pass `admin/admin`.  

## Development

Swarmpit is written purely in Clojure and utilizes React on front-end. CouchDB is just used to store data, that cannot be stored directly in Docker API.

Everything about building Swarmpit and setting up development environment can be found in [CONTRIBUTING.md](CONTRIBUTING.md)

## Preview

<kbd>
  <img src="http://swarmpit.io/img/demo-screen-1.gif?asdf">
</kbd></br></br>

[![Try in PWD](https://cdn.rawgit.com/play-with-docker/stacks/cff22438/assets/images/button.png)](http://play-with-docker.com?stack=/swarmpit/swarmpit/latest)

Use the following credentials to access the service: **admin**/**admin**
