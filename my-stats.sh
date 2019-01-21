#! /bin/bash

if [ $# -ne 1 ]; then
	echo my-stats.sh hostname
fi
host=$1
raw_result_dir="results/rdma/$host/raw"
combined_file="results/rdma/$host/stats.txt"
output_file="results/rdma/$host/report.texfile"

mkdir -p "$(dirname "$combined_input")"
echo "rdma $1 create ser deser network total size +dfl" > "$combined_file"
for f in "$raw_result_dir/"*"-result.txt"; do
   echo "Processing: $f"
   awk '/./{line=$0} END{print line}' "$f" >> "$combined_file"
done
