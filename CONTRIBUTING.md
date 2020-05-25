# How to contribute

Swarmpit is almost entirely written in Clojure, this allows seamless transition between frontend and backend development.

Backend part of application runs in JVM and frontend utilizes custom React & [MaterialUI](http://www.material-ui.com/) components with [RUM](https://github.com/tonsky/rum). Persistent data are stored in CouchDB. Docker is connected via a
socket.

[Leiningen](https://leiningen.org) manages project definition and its dependencies. [Figwheel](https://github.com/bhauman/lein-figwheel) is used for frontend hot-reloading.

## Setting up a development environment

Prerequisites
- Leiningen 2.8.2 or newer
- Docker socket accessible on `/var/run/docker.sock`

Install local dependencies

```
lein deps
```

Start a REPL session

```
lein repl
```

and call function `(fig-start)`, which will start db and agent containers followed by Swarmpit with Figwheel on http://localhost:3449

In order to use REPL on frontend side call additionally `(cljs-repl)`. Both `(fig-start)` & `(cljs-repl)` are
part of dev [`User`](dev/user.clj) namespace.

## Build

Whole application can be build to `jar` file 

```
lein with-profile prod uberjar
```

and then packed into Docker image

```
docker build -t swarmpit .
```

### Custom cljsjs packages

In case of outdated dependency feel free to build new version locally 

```
boot package install target
```

and deploy to local swarmpit repo e.g.

```
mvn deploy:deploy-file -Dfile=react-select-2.1.2-1.jar -DartifactId=react-select -Dversion=2.1.2 -DgroupId=cljsjs -Dpackaging=jar -Durl=file:repo
```

Finally add dependency to your maven repository

```
lein deps
```

## Reporting issues

In case of unexpected swarmpit behaviour, please create well-written issue [here](https://github.com/swarmpit/swarmpit/issues/new). It makes it easier to find & fix the problem accordingly. Please follow the template below, we really appreciate the effort.
```
Steps to reproduce:
1. create a service with any image
2. add an environment variable with name `MYVAR`
3. set `MYVAR` value to be `firstpart=second=third`
4. save the service

What happens:
- upon viewing the service, `MYVAR` with value `firstpart` 

What should happen:
- upon viewing the service `MYVAR` should have value `firstpart=second=third`
```
