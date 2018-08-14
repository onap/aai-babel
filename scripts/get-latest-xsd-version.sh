basedir=$1
cd ${basedir}/target/aai-schema
cp `ls -v | tail -1` ${basedir}/target/aai-schema/aai_schema_latest.xsd || exit 1 
echo "get-latest-xsd-version.sh has successfully copied aai_schema_latest.xsd to ${basedir}/target/aai-schema"
exit 0


