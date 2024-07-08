# Distributed and Parallel Execution

The Distributed & Parallel Execution (D&PE) is a component of the ICOS architecture that aims at exploiting at their best the available computing devices assigned to an application deployment. To that purpose, the component deploys on each container an agent in charge of managing the computing resources supporting its execution. This agent offers a public API where to request the execution of application methods and, upon the reception of a request, the analyses the logic of the method to divide its workload in different parts that could potentially run in parallel or have dependencies among them, represeting it as a task-based workflow. The agents collaboratively orchestrate the execution of these tasks aiming to exploit the underlying IT infrastructure while reducint the execution time of the execution and its energy footprint.

## Developing a distributed application
To facilitate the development of the workflows, the distributed and parallel execution component supports the COMPSs programming model. COMPSs provides a programming interface for the development of the applications and a runtime system that exploits the inherent parallelism of applications at execution time. With it, the component is able to automatically translate sequential Python or Java code into a task-based workflow. Further detail on how to develop applications following the COMPSs programming model can be found in the [official documentation](https://compss-doc.readthedocs.io/en/stable/Sections/02_App_Development.html) of the framework.


## Containerising the application
To facilitate the creation of container images that include applications exploiting the Distributed & Parallel Execution component, ICOS provides a base image that can be extended with the necessary software. This image already contains the runtime system of the D&PE which will deal with the conversion of the application code into a workflow and orchestrate the execution. Besides, that the runtime will also interact with all the ICOS meta-kernel layer to obtain the available computing devices, discover nearby resources with whom to share the workload and provide performance metrics regarding the execution for the meta-kernel layer to dynamically adapt the resource pool. The create a Dockerfile that generates the container image, developers can build on the `icos/dp-exec:latest` image, include the application distribution (e.g., the `sample.py` python code) into the image and define the `APP_PATH` environment variable to include it as part of the classpath/python path of the execution.

```dockerfile
FROM icos/dp-exec:latest
    COPY sample.py /app/sample.py
    ENV APP_PATH="/app"    
```
Sometimes, other software dependencies required by the application enforce the usage of a different base image. In this case, the D&PE component copied from the `icos/dp-exec:latest` image into the necessary base image along with the application. Bear in mind that in this case, the D&PE will also require to install its dependencies. The following snippet shows a Dockerfile example where the D&PE is copied into an ubuntu:20.04 image.


```dockerfile
FROM icos/dp-exec:latest AS compss_fw
FROM ubuntu:20.04
    # Install dependencies
    RUN apt-get update && \
        apt-get install -y --no-install-recommends \
            openjdk-8-jdk=8u412-ga-1~20.04.1 \
            graphviz=2.42.2-3build2 \
            xdg-utils=1.1.3-2ubuntu1.20.04.2 \
            uuid-runtime=2.34-0.1ubuntu9.6 \
            python3=3.8.2-0ubuntu2 \
            curl=7.68.0-1ubuntu2.22 \
            jq=1.6-1ubuntu0.20.04.1 && \
        apt-get autoclean && \
        rm -rf /var/lib/apt/lists/* && \
        ln -s /usr/bin/python3 /usr/bin/python

    # Copy the COMPSs framework
    COPY --from=compss_fw /opt/COMPSs /opt/COMPSs

    # Set up the environment
    ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
    ENV PATH="${PATH}:/opt/COMPSs/Runtime/scripts/user:/opt/COMPSs/Bindings/c/bin:/opt/COMPSs/Runtime/scripts/utils"
    ENV CLASSPATH="${CLASSPATH}:/opt/COMPSs/Runtime/compss-engine.jar"
    ENV LD_LIBRARY_PATH="/opt/COMPSs/Bindings/bindings-common/lib:${JAVA_HOME}/jre/lib/amd64/server"
    ENV COMPSS_HOME=/opt/COMPSs/
    ENV PYTHONPATH="${COMPSS_HOME}/Bindings/python/3:${PYTHONPATH}"

    EXPOSE 46101
    EXPOSE 46102

    CMD ["/bin/bash", "-c", "/opt/COMPSs/Runtime/scripts/user/compss_agent_start --hostname=${HOSTNAME} --pythonpath=${APP_PATH} --log_dir=/log --rest_port=46101 --comm_port=46102"]

    # Copy the application
    COPY sample.py /app/sample.py
    ENV APP_PATH="/app"    
```

## Invoking applications
Once the containerised application has been deployed, it becomes a service expecting to receive function executions request through its REST API. To that end, it offers an endpoint at http://${host_ip}:46101/COMPSs/startApplication that can be invoked with a POST method passing in an XML description of the function to run and its parameters. More information can be found in the [API](./api.md) specification.
