ARG DEBIAN_FRONTEND=noninteractive

ARG BUILDER_BASE=base20
ARG BUILDER_BASE_VERSION=230308-090438
ARG AGENT_BASE=ubuntu
ARG AGENT_BASE_VERSION=20.04

FROM compss/${BUILDER_BASE}_ci:${BUILDER_BASE_VERSION} as builder_base
FROM ${AGENT_BASE}:${AGENT_BASE_VERSION} as agent_base


FROM builder_base as compss_fw
ENV GRADLE_HOME /opt/gradle
ENV PATH $PATH:/opt/gradle/bin

COPY . /framework

ENV PATH $PATH:/opt/COMPSs/Runtime/scripts/user:/opt/COMPSs/Bindings/c/bin:/opt/COMPSs/Runtime/scripts/utils:/opt/gradle/bin
ENV CLASSPATH $CLASSPATH:/opt/COMPSs/Runtime/compss-engine.jar
ENV LD_LIBRARY_PATH /opt/COMPSs/Bindings/bindings-common/lib:$LD_LIBRARY_PATH
ENV COMPSS_HOME=/opt/COMPSs/

# Install COMPSs
WORKDIR /framework

RUN ./submodules_get.sh && \
    /framework/builders/buildlocal /opt/COMPSs -T -J -M -D -C

WORKDIR /

# Expose SSH port and run SSHD
EXPOSE 22
CMD ["/usr/sbin/sshd","-D"]


# COMPSs BASE IMAGE
FROM agent_base as compss_agent

    LABEL maintainer="COMPSs Support <support-compss@bsc.es>" \
        vendor="Barcelona Supercomputing Center (BSC)" \
        url="http://compss.bsc.es"

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

    ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/

    COPY --from=compss_fw /opt/COMPSs /opt/COMPSs
    COPY --from=compss_fw /etc/profile.d/compss.sh /etc/profile.d/compss.sh

    ENV PATH="${PATH}:/opt/COMPSs/Runtime/scripts/user:/opt/COMPSs/Bindings/c/bin:/opt/COMPSs/Runtime/scripts/utils"
    ENV CLASSPATH="${CLASSPATH}:/opt/COMPSs/Runtime/compss-engine.jar"
    ENV LD_LIBRARY_PATH="/opt/COMPSs/Bindings/bindings-common/lib:${JAVA_HOME}/jre/lib/amd64/server"
    ENV COMPSS_HOME=/opt/COMPSs/
    ENV PYTHONPATH="${COMPSS_HOME}/Bindings/python/3:${PYTHONPATH}"

    ENV APP_PATH=/app
    CMD ["/bin/bash", "-c", "/opt/COMPSs/Runtime/scripts/user/compss_agent_start --hostname=${HOSTNAME} --pythonpath=${APP_PATH} --log_dir=/log --rest_port=46101 --comm_port=46102"]

    EXPOSE 46101
    EXPOSE 46102