# ViaProxy
Standalone proxy which uses ViaVersion to translate the protocol between minecraft versions.

ViaProxy allows you to join to essentially any minecraft version server from a modern client.  
To download the latest version, go to the [Releases section](#executable-jar-file) and download the latest version.  
Using it is very simple, just run the jar file, and it will start a user interface where everything can be configured.  
For a full user guide go to the [Usage for Players](#usage-for-players-gui) section or the [Usage for Server Owners](#usage-for-server-owners-cli) section.

## Supported Server versions
- Classic (c0.0.15 - c0.30 including [CPE](https://wiki.vg/Classic_Protocol_Extension))
- Alpha (a1.0.15 - a1.2.6)
- Beta (b1.0 - b1.8.1)
- Release (1.0.0 - 1.19.3)
- April Fools (3D Shareware, 20w14infinite)
- Combat Snapshots (Combat Test 8c)

## Supported Client versions
- Release (1.7.2 - 1.19.3)

## Releases
### Executable Jar File
If you want the executable jar file you can download a stable release from [GitHub](https://github.com/RaphiMC/ViaProxy/releases/latest) or the latest dev version from this [Jenkins](https://build.lenni0451.net/job/ViaProxy/).

### Gradle/Maven
To use ViaProxy with Gradle/Maven you can use this [Maven server](https://maven.lenni0451.net/#/releases/net/raphimc/ViaProxy) or [Jitpack](https://jitpack.io/#RaphiMC/ViaProxy).  
You can also find instructions how to implement it into your build script there.

## Usage for Players (GUI)
![ViaProxy GUI](https://i.imgur.com/iN7cmSB.png)
1. Download the latest version from the [Releases section](#executable-jar-file)
2. Put the jar file into a folder (ViaProxy will generate config files and store some data there)
3. Run the jar file
4. Fill in the required fields like server address and version
5. Click on "Start"
6. Join with your minecraft client on the displayed address
7. Have fun!

## Usage for Server owners (CLI)
1. Download the latest version from the [Releases section](#executable-jar-file)
2. Put the jar file into a folder (ViaProxy will generate config files and store some data there)
3. Run the jar file (Using java -jar ViaProxy-whateverversion.jar --help)
4. Look at the available config options and use those you need just like you would in the GUI
5. Start the proxy using the start command and test whether it works (Join using the server's public address and the bind port you configured)
6. Have fun!

Here is an example command to allow players to join on yourserverip:25568 and connect to a beta 1.7.3 server running on port 25565:
``java -jar ViaProxy-whateverversion.jar --bind_port 25568 --target_ip 127.0.0.1 --target_port 25565 --version b1.7-b1.7.3``

### Configuring the protocol translation
To change the protocol translation settings/features you can look into the ViaProtocolHack folder.
You will find 4 config files there:
- viaversion.yml (ViaVersion)
- config.yml (ViaBackwards)
- viarewind.yml (ViaRewind)
- vialegacy.yml (ViaLegacy)

### Developer Plugin API
ViaProxy has a plugin API which allows you to create plugins for ViaProxy.
You can find the documentation and examples [here](https://github.com/Lenni0451/NoLocalConnections).

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/ViaProxy/issues).  
If you just want to talk or need help using ViaProxy feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).