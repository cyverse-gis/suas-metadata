# Calliope Index Server Deployment

Step 1: Launch a new Ubuntu 18.04 instance on OpenStack

Right now, we're using a 4-core, 32GB RAM, 60 GB root size. We have a 500GB external volume attached. 

Update and Upgrade
```
sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y 
```

Add dependencies
```
sudo apt-get install -y less vim htop libpq-dev lsb wget gnupg apt-transport-https curl \
    && sudo apt-get clean \
    && sudo rm -rf /usr/lib/apt/lists/*
```

Install iCommands 
```
sudo wget -qO - https://packages.irods.org/irods-signing-key.asc | apt-key add - \
    && echo "deb [arch=amd64] https://packages.irods.org/apt/ bionic main" > /etc/apt/sources.list.d/renci-irods.list \
    && sudo apt-get update \
    && sudo apt-get install -y irods-icommands
```
