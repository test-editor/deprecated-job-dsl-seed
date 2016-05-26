FROM jenkinsci/jenkins:latest

# Install some basics
USER root
RUN apt-get update
RUN apt-get install -y wget git curl zip vim
RUN apt-get install -y openjdk-8-jdk
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
USER jenkins

#COPY plugins.txt /usr/share/jenkins/ref/
COPY custom.groovy /usr/share/jenkins/ref/init.groovy.d/custom.groovy
#RUN /usr/local/bin/plugins.sh /usr/share/jenkins/ref/plugins.txt

#indicate that this Jenkins installation is fully configured. Otherwise a banner will appear prompting the user to install additional plugins, which may be inappropriate.
#RUN echo 2.0 > /usr/share/jenkins/ref/jenkins.install.UpgradeWizard.state
