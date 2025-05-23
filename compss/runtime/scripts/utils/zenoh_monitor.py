#!/usr/bin/env python3
#
#  Topology Exporter
#  Copyright © 2022-2025 Barcelona Supercomputing Center (BSC)
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  This work has received funding from the European Union's HORIZON research 
#  and innovation programme under grant agreement No. 101070177.
#
# -*- coding: utf-8 -*-


"""
Zenoh Monitor
This file contains the service used to update the COMPSs runtime with information received 
from the Zenoh bus.
"""

# Importing necessary libraries
import json
import requests
from threading import TIMEOUT_MAX
from time import sleep
import zenoh

def setup_zenoh(zenoh_router):
    """
    Set up Zenoh session for communication.
    
    Args:
        zenoh_router (str): The address of the Zenoh router.

    Returns:
        Zenoh session object.
    """
    # Setting up Zenoh configuration
    conf = zenoh.Config()

    # Setting up Zenoh mode
    conf.insert_json5("mode", json.dumps("client"))

    # Setting up Zenoh endpoints
    conf.insert_json5("connect/endpoints", json.dumps([zenoh_router]))

    # Opening Zenoh session
    return zenoh.open(conf)


def get_configured_remote_resources(server:str, port:int):
    """
    Retrieve detailed information about the configured resources from the COMPSs REST API.
    
    Args:
        server (str): The server address.
        port (int): The port number.
        
    Returns:
        Dictionary with resources names as keys and tuples of (num_cpus, memory_size) as values.
    """
    
    url = "http://" + server + ":" + str(port) + "/COMPSs/resources"
   
    response = requests.get(url)

    data = response.json()

    deployed_pods = {}

    for resource in data["resources"]:
        resource_name = resource["name"]
        resource_num_cpus = resource["description"]["processors"][0]["units"]
        resource_memory_size = resource["description"]["memory_size"]
        resource_adaptor = resource["adaptor"]
        if resource_adaptor != "es.bsc.compss.types.COMPSsMaster":
            deployed_pods[resource_name] = {"cpu": resource_num_cpus, "memory": resource_memory_size}

    return deployed_pods


def add_resource(agent_node, agent_port, num_cpus, memory_size, remote_node, remote_port):
    """
    Add a new resource to the COMPSs runtime.
    
    Args:
        agent_node (str): The node where the agent is running.
        agent_port (int): The port number for agent communication.
        num_cpus (int): Number of CPUs of the new resource.
        memory_size (int): Memory size of the new resource.
        remote_node (str): Name of the new resource.
        remote_port (int): Port number of the new resource.
    """

    url = "http://" + agent_node + ":" + str(agent_port) + "/COMPSs/addResources"

    xml_data = f'''
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <newResource>
        <externalResource>
            <name>{remote_node}</name>
            <description>
                <processors>
                    <processor>
                        <name>MainProcessor</name>
                        <type>CPU</type>
                        <architecture>[unassigned]</architecture>
                        <computingUnits>{num_cpus}</computingUnits>
                        <internalMemory>-1.0</internalMemory>
                        <propName>[unassigned]</propName>
                        <propValue>[unassigned]</propValue>
                        <speed>-1.0</speed>
                    </processor>
                </processors>
                <memorySize>{memory_size}</memorySize>
                <memoryType>[unassigned]</memoryType>
                <storageSize>-1.0</storageSize>
                <storageType>[unassigned]</storageType>
                <operatingSystemDistribution>[unassigned]</operatingSystemDistribution>
                <operatingSystemType>[unassigned]</operatingSystemType>
                <operatingSystemVersion>[unassigned]</operatingSystemVersion>
                <pricePerUnit>-1.0</pricePerUnit>
                <priceTimeUnit>-1</priceTimeUnit>
                <value>0.0</value>
                <wallClockLimit>-1</wallClockLimit>
            </description>
            <adaptor>es.bsc.compss.agent.comm.CommAgentAdaptor</adaptor>
            <resourceConf xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ResourcesExternalAdaptorProperties">
                <Property>
                    <Name>Port</Name>
                    <Value>{remote_port}</Value>
                </Property>
            </resourceConf>
        </externalResource>
    </newResource>
    '''

    headers = {'content-type': 'application/xml'}

    xml_data = xml_data.strip()

    try:
        response = requests.put(url, headers=headers, data=xml_data)
        
    except requests.exceptions.RequestException as e:
        print(f"Error making request: {e}")


def reduce_resource(agent_node, agent_port, num_cpus, memory_size, remote_node):
    """
    Reduce the resources allocated to a specific node in the COMPSs runtime.
    
    Args:
        agent_node (str): The node where the agent is running.
        agent_port (int): The port number for agent communication.
        num_cpus (int): Number of CPUs to be reduced.
        memory_size (int): Memory size to be reduced.
        remote_node (str): Name of the node.
    """

    url = "http://" + agent_node + ":" + str(agent_port) + "/COMPSs/removeResources"

    xml_data = f'''
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <reduceNode>
    <workerName>{remote_node}</workerName>
    <resources>
        <processors>
            <processor>
                <name>MainProcessor</name>
                <type>CPU</type>
                <architecture>[unassigned]</architecture>
                <computingUnits>{num_cpus}</computingUnits>
                <internalMemory>-1.0</internalMemory>
                <propName>[unassigned]</propName>
                <propValue>[unassigned]</propValue>
                <speed>-1.0</speed>
            </processor>
        </processors>
        <memorySize>{memory_size}</memorySize>
        <memoryType>[unassigned]</memoryType>
        <storageSize>-1.0</storageSize>
        <storageType>[unassigned]</storageType>
        <operatingSystemDistribution>[unassigned]</operatingSystemDistribution>
        <operatingSystemType>[unassigned]</operatingSystemType>
        <operatingSystemVersion>[unassigned]</operatingSystemVersion>
        <pricePerUnit>-1.0</pricePerUnit>
        <priceTimeUnit>-1</priceTimeUnit>
        <value>0.0</value>
        <wallClockLimit>-1</wallClockLimit>
    </resources>
    </reduceNode>
    '''

    headers = {'content-type': 'application/xml'}

    xml_data = xml_data.strip()

    try:
        response = requests.put(url, headers=headers, data=xml_data)

    except requests.exceptions.RequestException as e:
        print(f"Error making request: {e}")


def process_new_config(config_str):
    """
    Changes the configuration of the local COMPSs agent
    
    Args:
        config_str (str): JSON description of the current topology of the application.
    """
    config_json=json.loads(config_str)
    
    import os
    hostname = os.environ.get("HOSTNAME")
    
    deployed_ips = []
    local_cluster = None
    for cluster_id, cluster_desc in config_json["clusters"].items():
        pods = cluster_desc["pods"]
        if pods is not None:
            host_pod = None
            for pod_id, pod_desc in pods.items():
                if pod_desc["name"] == hostname:
                    host_pod = pod_id
                    local_cluster = cluster_desc
            if host_pod is not None:
                pods.pop(host_pod)
                deployed_ips = [pod_desc["ip"] for pod_id, pod_desc in pods.items()]
    
    resources = get_configured_remote_resources(server="localhost", port=46101)
    
    if local_cluster is not None:
        to_remove_resources = {k:v for k,v in resources.items() if k not in deployed_ips }
        for res in to_remove_resources:
            print("Removing " + res, flush = True)
            reduce_resource("localhost", 46101, 1, -1, res)

        for pod_id, pod_desc in local_cluster["pods"].items():
            p_ip = pod_desc["ip"]
            if p_ip not in resources:
                print("Adding " + p_ip, flush = True)
                add_resource("localhost", 46101, 1, -1, p_ip, 46102)



# Defining Zenoh subscriber handler
def listener(sample):
    """
    Handler of messages received through the bus.
    Args:
        sample (sample): message received through the bus.
    """
    print("Obtained from Zenoh: \n" + sample.payload.to_string(), flush = True)
    process_new_config(sample.payload.to_string())


def main():

    zenoh_ep = "tcp/zenoh.icos-system.svc.cluster.local:7447"
    session = setup_zenoh(zenoh_ep)

    import os
    app_id = os.environ.get("ICOS_APP_INSTANCE")
    component_name = os.environ.get("ICOS_APP_COMPONENT")
    zenoh_key = app_id + "/" + component_name

    print("Setting up subscriber on topic " + zenoh_key, flush = True)
    sub = session.declare_subscriber(key_expr = zenoh_key, handler=listener)

    while True:
        sleep(TIMEOUT_MAX)

    sub.undeclare()
    session.close()

# Listening Application Instance Component messages from Zenoh bus
if __name__ == "__main__":
    print("Starting ZENOH MONITOR", flush = True)
    main()