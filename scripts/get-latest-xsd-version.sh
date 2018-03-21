basedir=$1
cd ${basedir}/target/tmp/aai_schema
cp `ls -v | tail -1` ${basedir}/target/tmp/aai_schema/aai_schema_latest.xsd || exit 1 
echo "get-latest-xsd-version.sh has successfully copied aai_schema_latest.xsd to ${basedir}/target/tmp/aai_schema/latest_aai_schema"
exit 0


