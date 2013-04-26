#! /bin/bash

PREFIX=$1
ARCHIVE_HOME=$2

ARCHIVE_USER=fedoraAdmin
ARCHIVE_PASSWORD=fedoraAdmin
SERVER=localhost
TOMCAT_PORT=8080
DOWNLOAD=http://themis.hbz-nrw.de:9280/fedora/
OAI=http://www.dipp.nrw.de/repository/
SET=pub-type:journal

SRC=$ARCHIVE_HOME/src
WEBAPPS=$ARCHIVE_HOME/fedora/tomcat/webapps
API_SRC=$SRC/edoweb2-api/target/edoweb2-api.war
API_DEST=$WEBAPPS/edoweb2-api.war
SYNCER_SRC=$SRC/${PREFIX}Sync/target/${PREFIX}Sync-0.0.1-SNAPSHOT-jar-with-dependencies.jar
SYNCER_DEST=$ARCHIVE_HOME/sync/${PREFIX}sync.jar
FRONTEND_SRC=$SRC/ui/htdocs
FRONTEND_DEST=$ARCHIVE_HOME/html


mkdir -v $ARCHIVE_HOME/${PREFIX}base
echo "Update src must be done manually!"
echo "OK?"
$ARCHIVE_HOME/fedora/tomcat/bin/shutdown.sh

echo "Compile..."
cd $SRC/
mvn -q -e clean install
cd $SRC/${PREFIX}Sync
mvn -q -e assembly:assembly 
cd $SRC/edoweb2-api
echo "Compile end ..."

echo "Install Webapi"
mvn -q -e war:war
echo "Rollout..."
rm -rf  $WEBAPPS/edoweb2-api*
cp $API_SRC $API_DEST
cp $SYNCER_SRC $SYNCER_DEST 

rm -rf  $WEBAPPS/oai-pmh*
cp $SRC/ui/bin/oai-pmh.war $WEBAPPS
cp $SRC/ui/conf/proai.properties $WEBAPPS/oai-pmh/WEB-INF/classes

$ARCHIVE_HOME/fedora/tomcat/bin/startup.sh
echo "FINISHED!"
echo install htdocs
cp -r $FRONTEND_SRC/* $FRONTEND_DEST


echo -e "#! /bin/bash" > ${PREFIX}Sync.sh
echo -e "" >> ${PREFIX}Sync.sh
echo -e "export LANG=en_US.UTF-8" >> ${PREFIX}Sync.sh
echo -e "" >> ${PREFIX}Sync.sh
echo -e "cd $ARCHIVE_HOME/sync" >> ${PREFIX}Sync.sh
echo -e "" >> ${PREFIX}Sync.sh
echo -e "cp .oaitimestamp$PREFIX oaitimestamp$PREFIX`date +"%Y%m%d"`" >> ${PREFIX}Sync.sh
echo -e "" >> ${PREFIX}Sync.sh
echo -e "java -jar -Xms512m -Xmx512m $PREFIXsync.jar --mode SYNC -list $ARCHIVE_HOME/sync/pidlist.txt --user $ARCHIVE_USER --password $ARCHIVE_PASSWORD --dtl $DOWNLOAD --cache $ARCHIVE_HOME/${PREFIX}base --oai  $OAI --set $SET --timestamp .oaitimestamp$PREFIX --fedoraBase http://$SERVER:$TOMCAT_PORT/fedora --host http://$SERVER >> ${PREFIX}log`date +"%Y%m%d"`.txt 2>&1" >> ${PREFIX}Sync.sh
echo -e "" >> ${PREFIX}Sync.sh
echo -e "cd -" >> ${PREFIX}Sync.sh

mv ${PREFIX}Sync.sh $ARCHIVE_HOME/sync


