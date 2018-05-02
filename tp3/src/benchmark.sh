#!/bin/bash


start=$(date +%s%N)

for i in {1..30}
do 
	#wget -qO- http://127.0.0.1:8000/ &
	wget -qO- http://132.207.12.93:8000/ &
done

wait
end=$((($(date +%s%N) - $start)/1000000))
echo "Time taken: $end milliseconds"
