# Build

To build the 'cdk/depict' image directly from source (including the MAVEN build).

There are two different versions. Both now build on Java 25. The "jakarta"
variant targets the Jakarta servlet namespace used by Spring 6 / Tomcat 10+.

```
docker/$ docker build -t cdkdepict-jsp3 -f Dockerfile ..
docker/$ docker build -t cdkdepict-jakarta -f Dockerfile.jakarta ..
```

or from the project main directory:

```
$ docker build -t cdkdepict -f docker/Dockerfile .
$ docker build -t cdkdepict-jakarta -f docker/Dockerfile.jakarta .
```

# Run

The web service is exposed through port 8080. To expose locally on port 8180 we
run the following command.

	docker run -it -p 8180:8080 cdkdepict

Note that when using docker-machine (e.g. VirtualBox) you will need to forward
the host/guest port there as well.
