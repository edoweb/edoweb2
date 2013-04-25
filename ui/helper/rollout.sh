#! /bin/bash

PREFIX=$1
HOME=/opt/archive
SRC=$HOME/src
WEBAPPS=$HOME/fedora/tomcat/webapps
API_SRC=$SRC/edoweb2-api/target/edoweb2-api.war
API_DEST=$WEBAPPS/edoweb2-api.war
SYNCER_SRC=$SRC/${PREFIX}Sync/target/${PREFIX}Sync-0.0.1-SNAPSHOT-jar-with-dependencies.jar
SYNCER_DEST=$HOME/sync/${PREFIX}sync.jar
FRONTEND_SRC=$SRC/ui/htdocs
FRONTEND_DEST=$HOME/html

echo "Update src must be done manually!"
echo "OK?"
 $HOME/fedora/tomcat/bin/shutdown.sh

echo "Compile..."
cd $SRC
mvn clean install
cd $SRC/${PREFIX}Sync
mvn assembly:assembly 
cd $SRC/edoweb2-api
echo "Compile end ..."

echo "Install Webapi"
mvn war:war
echo "Rollout..."
rm -rf  $WEBAPPS/edoweb2-api*
cp $API_SRC $API_DEST
cp $SYNCER_SRC $SYNCER_DEST 

rm -rf  $WEBAPPS/oai-pmh*
cp $SRC/ui/compiled/oai-pmh.war $WEBAPPS
cp $SRC/ui/conf/proai.properties $WEBAPPS/oai-pmh/WEB-INF/classes

$HOME/fedora/tomcat/bin/startup.sh
echo "FINISHED!"
echo install htdocs
cp -r $SRC/$FRONTEND_SRC/* $HOME/$FRONTEND_DEST

tail -f $HOME/fedora/tomcat/logs/catalina.out
