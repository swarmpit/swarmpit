# How to contribute

Swarmpit is almost entirely written in Clojure, this allows seamless transition between frontend and backend development.

Backend part of application runs in JVM and frontend utilizes custom React & [MaterialUI](http://www.material-ui.com/) components with [RUM](https://github.com/tonsky/rum). Persistent data are stored in CouchDB. Docker is connected via a
socket.

[Leiningen](https://leiningen.org) manages project definition and its dependencies. [Figwheel](https://github.com/bhauman/lein-figwheel) is used for frontend hot-reloading.

## Setting up a development environment

Prerequisites
- Leiningen 2.6.1 or newer
- Docker socket accesible on `/var/run/docker.sock`

Start a REPL session

```
lein repl
```

and call function `(run)`, which will start DB container and Swarmpit with Figwheel on http://localhost:3449
In order to use REPL on frontend side call additionally `(browser-repl)`. Both `(run)` & `(browser-repl)` are
part of dev `User` namespace.

## Build

Whole application can be build to `jar` file 

```
lein with-profile prod uberjar
```

and then packed into Docker image

```
docker build -t swarmpit .
```

