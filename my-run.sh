#! /bin/bash

mem=-Xmx512m
server=serializers.RDMA.RDMAServer
client=serializers.RDMA.RDMAItemBenchmark

libdisni=/home/lynus/tpc/
cpgen=$(cat build/gen-cp)
cplib=$(cat build/lib-cp)
cp=$cpgen:$cplib:./build/bytecode/main:lib

javaopt="$mem -cp $cp -Djava.library.path=$libdisni"

testTime=100
warmupTime=150
iter=10

raw_result_dir="results/rdma/`hostname`/raw"

if [ $# -lt 2 ]; then
  echo "my-run.sh client/server host [serializer]"
  exit 1
fi
if [ $1 == server ]; then
  java $javaopt $server -host=$2 data/media.1.json
elif [ $1 == client ]; then
  if [ $# -gt 2 ]; then
    java $javaopt $client -iterations=$iter -warmup-time=$warmupTime -testRunMillis=$testTime -include=$3 -host=$2 data/media.1.json
  exit $?
  fi
  if [ ! -e $raw_result_dir ]; then
    mkdir -p $raw_result_dir
  fi
  sentence=$(java $javaopt serializers.BenchmarkExporter) 
  sentence=${sentence//,/$'\n'}  # change the colons to white space
  for word in $sentence
  do
    echo "running $word .."
    file=$word-result.txt
    file="$raw_result_dir"/${file//\//-}  # change '/' to '-'

    java $javaopt $client -iterations=$iter -warmup-time=$warmupTime -testRunMillis=$testTime -include=$word -host=$2 data/media.1.json > $file
    if [ $? -ne 0 ]; then
      echo "error: run $work"
      exit $?
    fi
  done
else
  echo "my-run.sh client/server"
  exit 1
fi


