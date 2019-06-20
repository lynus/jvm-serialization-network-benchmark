#!/bin/bash
set -e

raw_result_dir="results/raw"

mem=-Xmx256m
clz=serializers.BenchmarkRunner

cpgen=$(cat build/gen-cp)
cplib=$(cat build/lib-cp)
sep=':'
java='/home/lynus/java_staff/JikesRVM/dist/production_x86_64_m64-linux/rvm'
cp=./build/bytecode/main$sep$cpgen$sep$cplib
sentence=$($java -cp $cp serializers.BenchmarkExporter) # just grab all serializers
echo $sentence
$java $mem -cp $cp $clz -iterations=200 -warmup-time=100 -testRunMillis=1000 -include=$sentence data/media.1.json