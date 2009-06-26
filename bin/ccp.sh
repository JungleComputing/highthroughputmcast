#!/bin/bash

ibis_server_addr=fs0.das3.cs.vu.nl
ibis_server_port=55446

source_nodes=
source_data=
dest_nodes=
dest_dir=
method=robber
res=prun

function usage() {
	echo "usage: $0 [options] source[,source...]:file_or_dir[,file_or_dir...] dest[,dest...]:directory"
	echo "options:"
	echo "-method robber"
	echo "  distribution method to use (default: $method)"
	echo "-res prun|gcmd"
	echo "  reservation method to use (default: $res)"
	exit 1
}

function parse_nodes() {
	s=$1

	nodes=$(echo $s | sed 's/:.*//')
	nodes=$(echo $nodes | sed 's/,/ /g')
	
	echo $nodes
}

function parse_data() {
	s=$1

	data=$(echo $s | sed 's/.*://')
	data=$(echo $data | sed 's/,/ /g')
	
	echo $data
}

# read command line options
while true; do
    case $1 in
        -method)
            method=$2
            shift 2
            ;;
		-res)
			res=$2
			shift 2
			;;
		-h|--help|-?)
			usage
			;;
		-*)
			echo "Unknown parameter: $1"
			usage
			;;
		*) break ;;
    esac
done

case $method in
	robber)
		impl=mcast.ht.apps.filecopy.RobberFileMulticast
		;;
	bittorrent)
		impl=mcast.ht.apps.filecopy.BitTorrentFileMulticast
		;;
	*)
		echo "Unknown method: $method"
		usage
		;;
esac

source=$1
dest=$2

if [ -z "$source" ]; then
	echo 'No source(s) specified'
	usage
fi

if [ -z "$dest" ]; then
	echo 'No destination(s) specified'
	usage
fi

source_nodes=$(parse_nodes $source)
source_data=$(parse_data $source)
dest_nodes=$(parse_nodes $dest)
dest_dir=$(parse_data $dest)

if [ -z "$source_nodes" ]; then
	echo "No source nodes specified"
	usage
fi
if [ -z "$source_data" ]; then
	echo "No source data specified"
	usage
fi
if [ -z "$dest_nodes" ]; then
	echo "No destination nodes specified"
	usage
fi
if [ -z "$dest_dir" ]; then
	echo "No destination directory specified"
	usage
fi

echo "Performing cluster copy"
echo "- source nodes: $source_nodes"
echo "- source data:  $source_data"
echo "- dest. nodes:  $dest_nodes"
echo "- dest. dir:    $dest_dir"
echo "- method:       $method"

# determine number of nodes and prun queues
queues=
nodecount=0
concat=
for node in $source_nodes $dest_nodes; do
	queues="${queues}${concat}all.q@${node}"
	nodecount=$(($nodecount + 1))
	concat=","
done

# determine the output dir to use
jobid=$(next_prun_jobid)
output_dir=prun.out/job-$jobid
mkdir -p $output_dir || exit
echo "Starting prun job $jobid on $nodecount nodes ..."

# create the source files arguments
source_data_args=
concat=
for data in $source_data; do
	source_data_args="${source_data_args}${concat}-s $data"
	concat=" "
done 

# create the -sender arguments
sender_args=
for node in $source_nodes; do
	sender_args="$sender_args -sender $node"
done

# create the name server key
now=$(date +%Y-%m-%d-%H:%M:%S)
pool_name="ccp-${USER}-${now}"

# create and run command
cmd="-Xms3500m \
	-Xmx3500m \
	-Dibis.server.address=$ibis_server_addr \
	-Dibis.server.port=$ibis_server_port \
	-Dibis.pool.size=$nodecount \
    -Dibis.pool.name=$pool_name \
    -Dlog4j.configuration=file://$(pwd)/log4j.properties \
    -Dmcast.ht.apps.filecopy.piece_size=128KB \
	\
	mcast.ht.apps.filecopy.FileCopy \
	$sender_args \
	$source_data_args \
	-t $dest_dir \
	-impl $impl \
    -v \
	"

if [ $res = "prun" ]; then
	cmd="prun -no-panda -1 -t 15:00 -o $output_dir/node -q $queues \
		java $nodecount \
		$cmd"
elif [ $res = "gcmd" ]; then
	cmd="gcmd -bg -wait -o $output_dir \
		$source_nodes $dest_nodes -
		java $cmd"
fi

echo $cmd
$cmd

echo "Completed prun job $jobid"
