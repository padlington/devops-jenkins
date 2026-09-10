### Create the Jenkins system in AWS ###

# 1. Configure the Azure Provider
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# ==============================================================================
# 2. Create a Resource Group
# ==============================================================================
resource "azurerm_resource_group" "rg" {
  name     = "JenkinsTestRG"
  location = "Australia Southeast"
}

# ==============================================================================
# NETWORK INFRASTRUCTURE 
# ==============================================================================

# 3. Create a Virtual Network

resource "azurerm_virtual_network" "vnet" {
  name                = "JenkinsTest-vnet"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
}


# 4. Create a Subnet
resource "azurerm_subnet" "subnet" {
  name                 = "JenkinsTest-subnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.vnet.name
  address_prefixes     = ["10.0.2.0/24"]
}

# 5. Create a Public IP Address
resource "azurerm_public_ip" "public_ip" {
  name                = "JenkinsTest-ip"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  allocation_method   = "Dynamic"
}

# 6. Create a Network Security Group (NSG) to allow inbound SSH
resource "azurerm_network_security_group" "nsg" {
  name                = "JenkinsTest-nsg"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  security_rule {
    name                       = "SSH"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*" # Replace with your actual local IP (e.g., "192.168.1.1/32") for better security
    destination_address_prefix = "*"
  }
}

# 7. Create a Network Interface (NIC)
resource "azurerm_network_interface" "nic" {
  name                = "JenkinsTest-nic"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  ip_configuration {
    name                          = "internal"
    subnet_id                     = azurerm_subnet.subnet.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.public_ip.id
  }
}

# Associate the NSG to the NIC
resource "azurerm_network_interface_security_group_association" "nsg_assoc" {
  network_interface_id      = azurerm_network_interface.nic.id
  network_security_group_id = azurerm_network_security_group.nsg.id
}

# ==============================================================================
# VIRTUAL MACHINE (Ubuntu VM)
# ==============================================================================

# 8. Create the Ubuntu Linux Virtual Machine
resource "azurerm_linux_virtual_machine" "vm" {
  name                = "JenkinsTestVM"
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  size                = "Standard_D4s_v3" 
  admin_username      = "fujvmadmin"

  network_interface_ids = [
    azurerm_network_interface.nic.id,
  ]

# Specify SSH key yo connect to this VM
# Adjust path if your key is located elsewhere
# Default was public_key = file("~/.ssh/id_rsa_fujvmadmin.pub")
  admin_ssh_key {
    username   = "fujvmadmin"
	public_key = file("${path.module}/fujvmadmin-key.pub")
  }

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
  }

  # Configured for Ubuntu 24.04 LTS
  source_image_reference {
    publisher = "Canonical"
    offer     = "ubuntu-24_04-lts"
    sku       = "server"
    version   = "latest"
  }
  
  # This passes your script safely to the Ubuntu cloud-init agent
  custom_data = base64encode(file("${path.module}/init.sh"))
  
}

# ==============================================================================
# 5. OUTPUTS
# Output the Public IP Address so you can SSH into it easily
# ==============================================================================

output "public_ip_address" {
  value       = azurerm_linux_virtual_machine.vm.public_ip_address
  description = "The public IP address of the newly provisioned Ubuntu VM."
}
