FROM jenkins/jenkins:lts

USER root

# Install JDK 17
#RUN apt-get update && \
#    apt-get install -y openjdk-17-jdk

# Install Maven 3.11.0
#RUN wget https://dlcdn.apache.org/maven/maven-3/3.11.0/binaries/apache-maven-3.11.0-bin.tar.gz && \
#    tar xzvf apache-maven-3.11.0-bin.tar.gz -C /opt/ && \
#    ln -s /opt/apache-maven-3.11.0 /opt/maven

# Set Maven and JDK environment variables
ENV JAVA_HOME /usr/local/openjdk-17
ENV PATH $MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH

USER jenkins