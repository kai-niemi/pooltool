#!/bin/bash

releaseversion="v25.3.0"
nodes="1"
zones="eu-central-1a"
machinetype="m6i.large"

#############################################

if [ "$(whoami)" == "root" ]; then
    fn_echo_warning "Do NOT run as root!"
    exit 1
fi

if [ -z "${CLUSTER}" ] t; then
  fn_echo_warning "No \$CLUSTER id variable set!"
  echo "Use: export CLUSTER='your-cluster-id'"
  exit 1
fi

echo ">> Creating cluster"

roachprod create $CLUSTER --clouds=aws \
--aws-machine-type-ssd=${machinetype} --aws-zones=${zones} \
--aws-profile crl-revenue --aws-config ~/rev.json \
--geo --local-ssd-no-ext4-barrier \
--nodes=${nodes} \
--os-volume-size 750 \
--lifetime 24h0m0s

echo ">> Staging cluster"
roachprod stage $CLUSTER release $releaseversion

echo ">> Starting cluster"
roachprod start --insecure $CLUSTER:$nodes
roachprod admin --insecure --open --ips $CLUSTER:1

echo ">> Staging client"
roachprod run --insecure ${CLUSTER}:$nodes 'sudo apt-get -qq update'
roachprod run --insecure ${CLUSTER}:$nodes 'sudo apt-get -qq install -y openjdk-21-jre-headless'
roachprod put ${CLUSTER}:$nodes start.sh
roachprod put ${CLUSTER}:$nodes stop.sh
roachprod put ${CLUSTER}:$nodes run.sh
roachprod put ${CLUSTER}:$nodes target/pool-tool.jar

echo ">> Starting client"
roachprod run --insecure ${CLUSTER}:$nodes "./start.sh"

echo "Done"