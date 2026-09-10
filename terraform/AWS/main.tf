### Create the Jenkins system in AWS ###

# 1. Configure the AWS Provider
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.63"
    }
  }

  required_version = ">= 1.2"
}

provider "aws" {
  region = "ap-southeast-2" 
}

# ==============================================================================
# 1. NETWORK INFRASTRUCTURE (VPC)
# ==============================================================================

# Create Custom VPC
resource "aws_vpc" "custom_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "JenkinsTestVPC"
  }
}

# Create Public Subnet
resource "aws_subnet" "public_subnet" {
  vpc_id                  = aws_vpc.custom_vpc.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true # Gives the VM a public IP automatically
  availability_zone       = "ap-southeast-2a"

  tags = {
    Name = "JenkinsTestSubnet"
  }
}

# Create Internet Gateway
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.custom_vpc.id

  tags = {
    Name = "JenkinsTestVPC-IGW"
  }
}

# Create Route Table
resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.custom_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = {
    Name = "JenkinsTestRouteTable"
  }
}

# Associate Route Table with Subnet
resource "aws_route_table_association" "public_assoc" {
  subnet_id      = aws_subnet.public_subnet.id
  route_table_id = aws_route_table.public_rt.id
}

# ==============================================================================
# 2. SECURITY GROUP (Firewall Rules)
# ==============================================================================

resource "aws_security_group" "vm_sg" {
  name        = "ubuntu-vm-sg"
  description = "Allow inbound SSH access and all outbound traffic"
  vpc_id      = aws_vpc.custom_vpc.id

  # Inbound Rules: Allow SSH from anywhere
  ingress {
    description = "SSH access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # Change to your IP (e.g. "203.0.113.5/32") for better security
  }

  # Outbound Rules: Allow all traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "JenkinsTestSecurityGroup"
  }
}

# ==============================================================================
# 3. SSH KEY PAIR GENERATION
# ==============================================================================

# Generate a secure private key locally via Terraform
resource "tls_private_key" "vm_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

# Upload public key part to AWS
resource "aws_key_pair" "aws_vm_key" {
  key_name   = "ubuntu-deployer-key"
  public_key = tls_private_key.vm_key.public_key_openssh
}

# Save the private key into a local .pem file to use with your SSH client
resource "local_file" "private_key" {
  content         = tls_private_key.vm_key.private_key_pem
  filename        = "${path.module}/ubuntu-deployer-key.pem"
  file_permission = "0400" # Sets read-only permissions for safety
}

# ==============================================================================
# 4. EC2 INSTANCE (Ubuntu VM)
# ==============================================================================

# Find the latest Ubuntu 24.04 LTS AMI
data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"] # Canonical
}

# Provision the instance
resource "aws_instance" "ubuntu_vm" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.public_subnet.id
  vpc_security_group_ids = [aws_security_group.vm_sg.id]
  key_name               = aws_key_pair.aws_vm_key.key_name
  
  # Reads and passes the content of init.sh to EC2 on launch
  user_data = file("${path.module}/init.sh")

  tags = {
    Name = "JenkinsTestVM"
  }
}

# ==============================================================================
# 5. OUTPUTS
# ==============================================================================

output "public_ip" {
  description = "The public IP address of the Ubuntu VM"
  value       = aws_instance.ubuntu_vm.public_ip
}
