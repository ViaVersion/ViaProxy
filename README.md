# ViaProxy
Standalone proxy which allows players to join EVERY Minecraft server version (Classic, Alpha, Beta, Release, Bedrock)

To download the latest version, go to the [Releases section](#executable-jar-file) and download the latest version.  
Using it is very simple, just run the jar file, and it will start a user interface where everything can be configured.  
For a full user guide go to the [Usage for Players](#usage-for-players-gui) section or the [Usage for Server Owners](#usage-for-server-owners-config) section.

## Supported Server versions
- Release (1.0.0 - 1.21.11)
- Beta (b1.0 - b1.8.1)
- Alpha (a1.0.15 - a1.2.6)
- Classic (c0.0.15 - c0.30 including [CPE](https://wiki.vg/Classic_Protocol_Extension))
- April Fools (3D Shareware, 20w14infinite, 25w14craftmine)
- Combat Snapshots (Combat Test 8c)
- Bedrock Edition 1.26.0 ([Some features are missing](https://github.com/RaphiMC/ViaBedrock#features))

## Supported Client versions
- Release (1.7.2 - 1.21.11)
- April Fools (3D Shareware, 25w14craftmine)
- Bedrock Edition (Requires the [Geyser plugin](https://geysermc.org/download))
- Beta 1.7.3 (Requires the [Beta2Release plugin](https://github.com/ViaVersionAddons/ViaProxyBeta2Release))
- Classic, Alpha, Beta, Release 1.0 - 1.6.4 (Only passthrough)

ViaProxy supports joining to any of the listed server version from any of the listed client versions.

## Special Features
- Support for joining online mode servers
- Support for joining on servers which have chat signing enabled from all listed client versions
- Supports transfer and cookies for <=1.20.4 clients on 1.20.5+ servers
- Allows joining Minecraft Realms with any supported client version
- Supports Simple Voice Chat mod

## Releases
### Executable Jar File
If you want the executable jar file you can download a stable release from [GitHub Releases](https://github.com/ViaVersion/ViaProxy/releases/latest) or the latest dev version from [GitHub Actions](https://github.com/RaphiMC/ViaProxy/actions/workflows/build.yml) or the [ViaVersion Jenkins](https://ci.viaversion.com/view/Platforms/job/ViaProxy/).

### Docker Image
ViaProxy docker images can be found on [GitHub Packages](https://github.com/ViaVersion/ViaProxy/pkgs/container/viaproxy).

To run the latest version of ViaProxy you can use the following command:
```bash
docker run -it -v /path/to/run:/app/run -p 25568:25568 ghcr.io/viaversion/viaproxy:latest
```
where ``/path/to/run`` is the path where the ViaProxy data should be stored and ``25568`` is the port ViaProxy should listen on.

## Usage for Players (GUI)
![ViaProxy GUI](https://i.imgur.com/RaDWkbK.png)
1. Download the latest version from the [Releases section](#executable-jar-file)
2. Put the jar file into a folder (ViaProxy will generate config files and store some data there)
3. Run the jar file
4. Fill in the required fields like server address and version
5. If you want to join online mode servers, add your Minecraft account in the Accounts tab
6. Click on "Start"
7. Join with your Minecraft client on the displayed address
8. Have fun!

## Usage for Server owners (Config)
1. Download the latest version from the [Releases section](#executable-jar-file)
2. Put the jar file into a folder (ViaProxy will generate config files and store some data there)
3. Run the jar file (Using ``java -jar ViaProxy-whateverversion.jar config viaproxy.yml``)
4. ViaProxy now generates a config file called ``viaproxy.yml`` in the same folder and exits
5. Open the config file and configure the proxy (Most important options are at the top)
6. Start the proxy using the start command and test whether it works (Join using the server's public address and the bind port you configured)
7. Have fun!

## Usage for Server owners (CLI)
1. Download the latest version from the [Releases section](#executable-jar-file)
2. Put the jar file into a folder (ViaProxy will generate config files and store some data there)
3. Run the jar file (Using ``java -jar ViaProxy-whateverversion.jar cli --help``)
4. ViaProxy will print the CLI usage and exit
5. Configure the proxy and optionally put the finished start command into a script
6. Start the proxy using the start command and test whether it works (Join using the server's public address and the bind port you configured)
7. Have fun!

### Configuring the protocol translation
To change ViaProxy settings you can check out the ``viaproxy.yml`` config file. Most of the settings are configurable via the GUI.  
To change the protocol translation settings/features you can look into the ``ViaLoader`` folder.
You will find 5 config files there:
- viaversion.yml (ViaVersion)
- viabackwards.yml (ViaBackwards)
- viarewind.yml (ViaRewind)
- vialegacy.yml (ViaLegacy)
- viabedrock.yml (ViaBedrock)

### Developer Plugin API
ViaProxy has a plugin API which allows you to extend and modify the behavior of ViaProxy.  
Documentation for the plugin API can be found [here](https://github.com/ViaVersion/ViaProxy/wiki/Creating-plugins).  
A list of plugins can be found [here](https://github.com/ViaVersion/ViaProxy/wiki/Plugins).

## Contributing
Contributions in the form of pull requests are always welcome.
Please make sure to keep your code style consistent with the rest of the project and that your code is easily maintainable.
If you plan to make a large scale changes, please open an issue first or join my discord to discuss it.

### Translations
If you want to help translating ViaProxy you can do so on [Crowdin](https://crowdin.com/project/viaproxy).

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/ViaVersion/ViaProxy/issues).  
If you just want to talk or need help using ViaProxy feel free to join the ViaVersion
[Discord](https://discord.gg/viaversion).
