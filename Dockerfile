ARG DEBIAN_FRONTEND=noninteractive

ARG BUILDER_BASE=base22
ARG BUILDER_BASE_VERSION=250207-135955
ARG AGENT_BASE=ubuntu
ARG AGENT_BASE_VERSION=22.04

FROM compss/${BUILDER_BASE}_ci:${BUILDER_BASE_VERSION} AS builder_base
FROM ${AGENT_BASE}:${AGENT_BASE_VERSION} AS agent_base


FROM builder_base AS compss_fw
    ENV GRADLE_HOME=/opt/gradle \
        PATH=$PATH:/opt/gradle/bin

    COPY . /framework

    ENV PATH=$PATH:/opt/COMPSs/Runtime/scripts/user:/opt/COMPSs/Bindings/c/bin:/opt/COMPSs/Runtime/scripts/utils:/opt/gradle/bin \
        CLASSPATH=$CLASSPATH:/opt/COMPSs/Runtime/compss-engine.jar \
        LD_LIBRARY_PATH=/opt/COMPSs/Bindings/bindings-common/lib:$LD_LIBRARY_PATH \
        COMPSS_HOME=/opt/COMPSs/


    # Install COMPSs
    WORKDIR /framework

    RUN ./submodules_get.sh && \
        /framework/builders/buildlocal -T -J -M -D -C -K /opt/COMPSs

    WORKDIR /

    # Expose SSH port and run SSHD
    EXPOSE 22
    CMD ["/usr/sbin/sshd","-D"]


# COMPSs BASE IMAGE
FROM agent_base AS compss_agent

    LABEL maintainer="COMPSs Support <support-compss@bsc.es>" \
        vendor="Barcelona Supercomputing Center (BSC)" \
        url="http://compss.bsc.es"

    RUN apt-get update && \
        apt-get install -y --no-install-recommends \
            openjdk-11-jdk=11.0.27+6~us1-0ubuntu1~22.04 \
            graphviz=2.42.2-6ubuntu0.1 \
            xdg-utils=1.1.3-4.1ubuntu3~22.04.1 \
            uuid-runtime=2.37.2-4ubuntu3.4 \
            python3=3.10.6-1~22.04.1 \
            python3-pip=22.0.2+dfsg-1ubuntu0.5 \
            curl=7.81.0-1ubuntu1.20 \
            jq=1.6-2.1ubuntu3 && \
        apt-get autoclean && \
        rm -rf /var/lib/apt/lists/* && \
        ln -s /usr/bin/python3 /usr/bin/python && \
        python3 -m pip install --no-cache-dir \
            requests==2.32.3 \
            eclipse_zenoh==1.2.1        

    ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/

    COPY --from=compss_fw /opt/COMPSs /opt/COMPSs
    COPY --from=compss_fw /etc/profile.d/compss.sh /etc/profile.d/compss.sh

    ENV PATH="${PATH}:/opt/COMPSs/Runtime/scripts/user:/opt/COMPSs/Bindings/c/bin:/opt/COMPSs/Runtime/scripts/utils" \
        CLASSPATH="${CLASSPATH}:/opt/COMPSs/Runtime/compss-engine.jar" \
        LD_LIBRARY_PATH="/opt/COMPSs/Bindings/bindings-common/lib:${JAVA_HOME}/jre/lib/amd64/server" \
        COMPSS_HOME=/opt/COMPSs/ \
        PYTHONPATH="${COMPSS_HOME}/Bindings/python/3:${PYTHONPATH}" \
        APP_PATH=/app \
        LOG_LEVEL=OFF
    CMD ["/bin/bash", "-c", "/opt/COMPSs/Runtime/scripts/user/compss_agent_start --hostname=$(hostname -I) --pythonpath=${APP_PATH} --log_dir=/log --log_level=${LOG_LEVEL} --rest_port=46101 --comm_port=46102"]

    EXPOSE 46101
    EXPOSE 46102
    EXPOSE 19090
