#!/bin/csh
set TRANSPORT=$1
if (${TRANSPORT} == "") set TRANSPORT="grizzly"
echo Running with transport ${TRANSPORT}
./rungmsdemo.sh server cluster SPECTATOR 300000 INFO ${TRANSPORT} >&! server.log &
sleep 5
./rungmsdemo.sh instance01 cluster CORE 120000 INFO ${TRANSPORT} 9130 9160 >&! instance01.log &
./rungmsdemo.sh instance02 cluster CORE 120000 INFO ${TRANSPORT} 9160 9190 >&! instance02.log &
./rungmsdemo.sh instance03 cluster CORE 120000 CONFIG ${TRANSPORT} 9230 9260 >&! instance03.log &
./rungmsdemo.sh instance04 cluster CORE 120000 CONFIG ${TRANSPORT} 9261 9290 >&! instance04.log &
./rungmsdemo.sh instance05 cluster CORE 120000 CONFIG ${TRANSPORT} 9330 9360 >&! instance05.log &
./rungmsdemo.sh instance06 cluster CORE 120000 CONFIG ${TRANSPORT} 9361 9390 >&! instance06.log &
./rungmsdemo.sh instance07 cluster CORE 120000 CONFIG ${TRANSPORT} 9430 9460 >&! instance07.log &
./rungmsdemo.sh instance08 cluster CORE 120000 CONFIG ${TRANSPORT} 9461 9490 >&! instance08.log &
./rungmsdemo.sh instance09 cluster CORE 120000 CONFIG ${TRANSPORT} 9530 9560 >&! instance09.log &
./rungmsdemo.sh instance10 cluster CORE 120000 CONFIG ${TRANSPORT} 9561 9590 >&! instance10.log &


