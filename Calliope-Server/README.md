# Calliope Index Server Deployment

Step 1: Launch a new Ubuntu 18.04 instance on OpenStack

Step 2: Create `~/.ssh/authorized_keys` and import your public ssh key

# Provisioning with Docker and iCommands

Right now, we're using a 4-core, 32GB RAM, 60 GB root size. We have a 500GB external volume attached. 

## Update and Upgrade

```
sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y 
```

## Add dependencies

```
sudo apt-get install -y apt-transport-https ca-certificates curl gnupg gnupg-agent \
     htop less libpq-dev lsb software-properties-common vim wget
sudo apt-get clean
sudo rm -rf /usr/lib/apt/lists/*
```

## Install iCommands

```
sudo wget -qO - https://packages.irods.org/irods-signing-key.asc | apt-key add - 
sudo echo "deb [arch=amd64] https://packages.irods.org/apt/ xenial main" > /etc/apt/sources.list.d/renci-irods.list
sudo apt-get update 
sudo apt-get install -y irods-icommands
```

## Install Docker

```
sudo apt-get update
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt update 
sudo apt install docker-ce

# test install
sudo docker --version
```

Add user to the Docker group

```
sudo groupadd docker
sudo usermod -aG docker <username>
```

Exit terminal or restart 

```
# test
docker --version
```

## Install Docker Compose

```
sudo curl -L https://github.com/docker/compose/releases/download/1.17.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

## Import this repository

```
cd
mkdir github
cd github
git clone https://github.com/cyverse-gis/suas-metadata
cd suas-metadata/Calliope-Server
```
