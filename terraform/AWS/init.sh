#!/bin/bash
# Update packages and install required SW

apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk \
    ant \
    zip \
    unzip \
    ca-certificates \
    curl \
    git 
	
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" 
unzip awscliv2.zip 
./aws/install 

rm -rf awscliv2.zip ./aws 

apt-get clean 

rm -rf /var/lib/apt/lists/*

# Download the stable Google Chrome .deb file
#wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb

# Install Chrome along with its dependencies
#apt install -y ./google-chrome-stable_current_amd64.deb

# Install the python3-venv package if you don't have it
#sudo apt install python3-venv python3-pip -y

# Create a project directory and navigate into it
#mkdir selenium-project && cd selenium-project

# Create a virtual environment named 'venv'
#python3 -m venv venv

# Activate the virtual environment
#source venv/bin/activate

#pip install selenium



