# Data Integration Connectors

An internal service to provide easy access to a broad range of external enterprise data sources, taking away the complexity of infrastructure management from clients and allowing easy expansion of supported connectors by a pluggable connector ecosystem.

[Design doc](http://go/data-connectors-design)

## Getting started

#### Prerequisites
###### Local Setup
* Install Java 17 [https://www.oracle.com/in/java/technologies/downloads/#java17](https://www.oracle.com/in/java/technologies/downloads/#java17)
* Install Maven [https://maven.apache.org/install.html](https://maven.apache.org/install.html)
* Install gcloud [https://cloud.google.com/sdk/docs/install](https://cloud.google.com/sdk/docs/install)
* Install Git [https://git-scm.com/book/en/v2/Getting-Started-Installing-Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
* Install Docker [https://docs.docker.com/engine/install/](https://docs.docker.com/engine/install/)

###### Cloudtop Setup
* Install Git by running the following command on terminal
```
sudo apt-get install git-all git-remote-google
```
* Intellij Setup 
  * Download the community version [https://www.jetbrains.com/idea/download/?section=linux](https://www.jetbrains.com/idea/download/?section=linux)
  * Installation guide [https://www.jetbrains.com/help/idea/installation-guide.html#standalone](https://www.jetbrains.com/help/idea/installation-guide.html#standalone)
* Install Java 17 [https://www.oracle.com/in/java/technologies/downloads/#java17](https://www.oracle.com/in/java/technologies/downloads/#java17)
* Install JEnv [https://www.jenv.be/](https://www.jenv.be/)
* Install Maven [https://maven.apache.org/install.html](https://maven.apache.org/install.html)
* Install gCloud [go/gcloud-cli#installing-and-using-the-cloud-sdk](https://g3doc.corp.google.com/company/teams/cloud-sdk/cli/index.md?cl=head#installing-and-using-the-cloud-sdk)
* Install Docker [go/installdocker#installation](https://g3doc.corp.google.com/cloud/containers/g3doc/glinux-docker/install.md?cl=head#installation) (no need to update daemon file)

#### Steps
* Request access to [cloud-connector-service-dev-team](https://ganpati2.corp.google.com/propose_membership?parent=100457634516&child=$me.prod) MDB group

* Copy the GoB connector repo and  build the code

```
gcloud auth application-default login

git clone sso://team/cloud-datafusion-dev-team/connectors && (cd connectors && f=`git rev-parse --git-dir`/hooks/commit-msg ; mkdir -p $(dirname $f) ; curl -Lo $f https://gerrit-review.googlesource.com/tools/hooks/commit-msg ; chmod +x $f)

mvn clean package
```

* Build the Docker image and run the connector server

```
DOCKER_BUILDKIT=1 docker buildx build --build-context="gcloud=$HOME/.config/gcloud" -f Dockerfile . -t connector-server:latest

docker container run -p54321:54321 connector-server:latest -p=54321 -r=/opt/connectors/lib -k
```
### Contact
[cloud-connector-service-dev-team@google.com](mailto:cloud-connector-service-dev-team@google.com)
