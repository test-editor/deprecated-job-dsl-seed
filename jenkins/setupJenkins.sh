#!/bin/bash
docker build --tag=test-editor/jenkins .
docker create --name jenkins -p 8080:8080 -p 50000:50000 -v /var/jenkins_home:/var/jenkins_home -t test-editor/jenkins /usr/local/bin/jenkins.sh
docker start jenkins
  
# Open a bash on a running container
# docker exec -it jenkins /bin/bash

# JENKINS_HOST=username:password@myhost.com:port
# curl -sSL "http://$JENKINS_HOST/pluginManager/api/xml?depth=1&xpath=/*/*/shortName|/*/*/version&wrapper=plugins" | perl -pe 's/.*?<shortName>([\w-]+).*?<version>([^<]+)()(<\/\w+>)+/\1 \2\n/g'|sed 's/ /:/'