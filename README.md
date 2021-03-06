# ONAP aai/babel

## Introduction
Babel is a microservice in the AAI project which can be used by clients that work with TOSCA CSAR files.

It parses TOSCA YAML files extracted from a CSAR payload and generates XML files containing AAI Model data.

## Building Babel
The Babel service can be built with Maven, e.g. by issuing the command `mvn clean install`  

Maven will produce the following artifacts in the "target" folder:

* babel.jar
* babel-client.jar
* Dockerfile
* start.sh

Maven will install the following artifacts in the local repository:
* babel-{version}.jar
* babel-{version}-client.jar

To create the docker image run:
docker build -t aai/babel target

## Babel Client
The project will build a client jar which can be used by clients invoking the Babel service.   

The client jar contains two objects that are used in the Babel service API.

BabelRequest is used to supply the inputs into the Babel service.
BabelArtifact is the response artifact in the list of artifacts returned from the Babel service.

### Deploying The Microservice 

Push the Docker image that you have built to your Docker repository and pull it down to the location that you will be running Babel from.

**Create the following directories on the host machine:**

    ./logs
    ./opt/app/babel/appconfig
    ./opt/app/babel/appconfig/auth

You will be mounting these as data volumes when you start the Docker container.  For examples of the files required in these directories, see the aai/test/config repository (https://gerrit.onap.org/r/#/admin/projects/aai/test-config)

**Populate these directories as follows:**

##### Contents of /opt/app/babel/appconfig

The following file must be present in this directory on the host machine:

The purpose of this configuration directory is to maintain configuration files specific to authentication/authorization for the _Babel_ service.
The following files must be present in this directory on the host machine:

_babel-auth.properties_

    auth.policy.file=/auth/auth_policy.json
    auth.authentication.disable=false


_artifact-generator.properties_ <br />
Contains model invariants ids used by SDC artifact generator library

_logback.xml_

Logging configuration.

##### Contents of /opt/app/babel/appconfig/auth 
_auth_policy.json_
 
Create a policy file defining the roles and users that will be allowed to access _Babel_ service.  This is a JSON format file which will look something like the following example:
 
     {
         "roles": [
             {
                 "name": "admin",
                 "functions": [
                     {
                         "name": "search", "methods": [ { "name": "GET" },{ "name": "DELETE" }, { "name": "PUT" }, { "name": "POST" } ]
                     }
                 ],
                 "users": [
                    {
                         "username": "CN=babeladmin, OU=My Organization Unit, O=, L=Sometown, ST=SomeProvince, C=CA"
                     }    
                 ]
             }
         ]
     }
 
 _tomcatkeystore_
 
Create a keystore with this name containing whatever CA certificates that you want your instance of the _Babel_ service to accept for HTTPS traffic.


## Dependency Information

To include the Babel service client jar in your project add the following maven dependency to your project's pom:

		<dependency>
			<groupId>org.onap.aai</groupId>
			<artifactId>babel</artifactId>
			<version>*</version>
			<classifier>client</classifier>
		</dependency>

