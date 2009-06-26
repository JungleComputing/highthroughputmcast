#!/bin/bash

#server_address=130.37.197.2-55446/10.153.0.128/10.142.255.253/10.141.255.254-55446:55446#ns
server_address=fs0.das3.cs.vu.nl
server_port=55446
pool_port=55448

data=1bytes
times=1
script=""
test=robber
pieces=32KB
emulate_bandwidth=false
emulate_delay=false
emulate_topo=false
fast_local_network=true
tell_before=
tell_after=
validate_storage=false
fake_storage=false
fill_storage=false
nodes=
pool_name=default_pool
Dprops=

function usage() {
	echo "usage: $0 [options]"
	echo "options:"
	echo "-times"
	echo "  number of times to multicast"
	echo "-data"
	echo "  amount of data to multicast (e.g. 1bytes, 20KB, 1MB etc.)"
	echo "-test bittorrent|robber"
	echo "  multicast method to test (default: $test)"
	echo "-pieces"
	echo "  size of the pieces in which the data is multicast (same syntax as -data, default: $pieces)"
    echo "-emulate-bandwidth"
    echo "  enables emulation of bandwidth (default: $emulate_bandwidth). Implies -emulate-topo"
    echo "-emulate-delay"
    echo "  enables emulation of delay (default: $emulate_delay). Implies -emulate-topo"
    echo "-emulate-topo"
    echo "  enables emulation of the topology, without doing any traffic shaping (default: $emulate_topo)"
    echo "-no-fast-local-network"
    echo "  disables non-emulated, direct connections between nodes in the same cluster (default: enabled)"
	echo "-nodes"
	echo "  start the run on the specified number of nodes, instead of deriving this from the script file"
	echo "-pool"
	echo "  the name of the Ibis pool to join"
	echo "-script"
	echo "  the emulation script file to use"
	echo "-tell-before <string>"
	echo "  tells the emulation a certain string just before starting a multicast measurement"
	echo "-tell-after <string>"
	echo "  tells the emulation a certain string just after finishing a multicast measurement"
	echo "-validate"
	echo "  enables validation of each multicast buffer"
	echo "-D*=XXX"
	echo "  adds a java property to the startup command"
	exit 1
}

param_count=$#

# read command line parameters
while true; do
    case $1 in
        -times)
            times=$2
            shift 2
            ;;
        -data)
            data=$2
            shift 2
            ;;
        -test)
            test=$2
            shift 2
            ;;
        -pieces)
            pieces=$2
            shift 2
            ;;
        -emulate-delay)
            emulate_delay=true
	       	emulate_topo=true
    		 shift
            ;;
        -emulate-bandwidth)
            emulate_bandwidth=true
			emulate_topo=true
            shift
            ;;
        -emulate-topo)
			emulate_topo=true
            shift
            ;;
		-no-fast-local-network)
            fast_local_network=false
            shift
            ;;
		-pool)
			pool_name=$2
			shift 2
			;;
		-nodes)
			nodes=$2
			shift 2
			;;
		-script)
			script=$2
			shift 2
			;;
		-tell-before)
			tell_before="-tell-before $2"
			shift 2
			;;
		-tell-after)
			tell_after="-tell-after $2"
			shift 2
			;;
		-validate)
			validate_storage=true
			shift 1
			;;
		-D*)
			Dprops="$Dprops $1"
			shift 1
			;;	
		-*)
			echo "Unknown parameter: $1"
			usage
			;;
		*) break ;;
    esac
done

if [ -n "$script" -a ! -f "$script" ]; then
	echo "file does not exist: $script"
	exit 1
elif [ -z "$script" -a -z "$nodes" ]; then
	if [ $param_count -ge 1 ]; then
        echo -e "\nPlease provide either -script and/or -nodes\n"
    fi
    usage
    exit 1
fi

# determine buffer validation settings
echo "Validating each multicast buffer: $validate_storage"
if [ "$validate_storage" = "true" ]; then
	fake_storage=false
	fill_storage=true
else
	fake_storage=true
	fill_storage=false
fi

# determine the output dir to use
jobid=$(next_prun_jobid)
output_dir=prun.out/job-$jobid
mkdir -p $output_dir || exit

echo "Running multicast test, sending $times x $data"

# determine the number of nodes needed 

if [ -n "$script" ]; then
    # read #nodes from the script
    clusters=$(awk '{ if ($1 == "defineCluster") n++ } END { print n }' $script)

    if [ -z "$clusters" ]; then
    	echo "Script $script does not define any clusters"
	    exit 1
    fi

    app_nodes=$(awk '{ if ($1 == "defineCluster") n += NF - 2 } END { print n }' $script)

	echo "Emulating $app_nodes nodes in $clusters clusters"
	run_nodes=$(($app_nodes + $clusters))
else
    run_nodes=$nodes
fi

param_script=
if [ -n "$script" ]; then
    param_script="-script $script"
fi

echo "Starting prun job $jobid on $run_nodes nodes ..."

cmd="prun -no-panda -1 -t 15:00 -o $output_dir/node \
	$IPL_HOME/bin/ipl-run $run_nodes 
	-Xmx1024m \
	-Dlog4j.configuration=log4j.properties \
	-Dibis.server.address=$server_address \
	-Dibis.server.port=$server_port \
	-Dibis.pool.size=$run_nodes \
    -Dibis.pool.name=$pool_name \
	-Dibis.pool.server.host=$server_address \
    -Dibis.pool.server.port=$pool_port \
	-Dmcast.ht.bittorrent.min_peers=5 \
	-Dmcast.ht.bittorrent.choking=true \
	-Dmcast.ht.bittorrent.tit_for_tat_peers=4 \
	-Dmcast.ht.bittorrent.max_pending_requests=5 \
	-Dclusteremulation.bandwidth=$emulate_bandwidth \
	-Dclusteremulation.delay=$emulate_delay \
	-Dclusteremulation.fast_local_network=$fast_local_network \
	$Dprops \
	mcast.ht.test.MulticastTester \
	-times $times \
	-data $data \
    -pieces $pieces \
	-test $test \
	$param_script \
	-use-cluster-emulator $emulate_topo \
	-validate-storage $validate_storage \
	-fake-storage $fake_storage \
	-fill-storage $fill_storage \
	$tell_before \
	$tell_after \
	"

echo $cmd
$cmd

echo "Completed prun job $jobid"
