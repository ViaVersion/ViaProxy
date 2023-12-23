# ViaProxy
Standalone proxy which allows players to join EVERY Minecraft server version (Classic, Alpha, Beta, Release, Bedrock)

To download the latest version, go to the [Releases section](#executable-jar-file) and download the latest version.  
Using it is very simple, just run the jar file, and it will start a user interface where everything can be configured.  
For a full user guide go to the [Usage for Players](#usage-for-players-gui) section or the [Usage for Server Owners](#usage-for-server-owners-cli) section.

## Supported Server versions
- Release (1.0.0 - 1.20.4)
- Beta (b1.0 - b1.8.1)
- Alpha (a1.0.15 - a1.2.6)
- Classic (c0.0.15 - c0.30 including [CPE](https://wiki.vg/Classic_Protocol_Extension))
- April Fools (3D Shareware, 20w14infinite)
- Combat Snapshots (Combat Test 8c)
- Bedrock Edition 1.20.50 (In development)

## Supported Client versions
- Release (1.7.2 - 1.20.4)
- Bedrock Edition (Needs the [Geyser plugin](https://github.com/RaphiMC/ViaProxyGeyserPlugin))
- Classic, Alpha, Beta, Release 1.0 - 1.6.4 (Only passthrough)

ViaProxy supports joining to any of the listed server version from any of the listed client versions.

## Special Features
- Support for joining online mode servers
- Support for joining on servers which have chat signing enabled from all listed client versions.

## Releases
### Executable Jar File
If you want the executable jar file you can download a stable release from [GitHub Releases](https://github.com/ViaVersion/ViaProxy/releases/latest) or the latest dev version from [GitHub Actions](https://github.com/RaphiMC/ViaProxy/actions/workflows/build.yml) or the [ViaVersion Jenkins](https://ci.viaversion.com/view/All/job/ViaProxy/).

### Gradle/Maven
To use ViaProxy with Gradle/Maven you can use the ViaVersion maven server:
```groovy
repositories {
    maven { url "https://repo.viaversion.com" }
}

dependencies {
    implementation("net.raphimc:ViaProxy:x.x.x") // Get latest version from releases
}
```

```xml
<repositories>
    <repository>
        <id>viaversion</id>
        <url>https://repo.viaversion.com</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.raphimc</groupId>
        <artifactId>ViaProxy</artifactId>
        <version>x.x.x</version> <!-- Get latest version from releases -->
    </dependency>
</dependencies>
```

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

## Usage for Server owners (CLI)
1. Download the latest version from the [Releases section](#executable-jar-file)
2. Put the jar file into a folder (ViaProxy will generate config files and store some data there)
3. Run the jar file (Using java -jar ViaProxy-whateverversion.jar --help)
4. Look at the available config options and use those you need just like you would in the GUI
5. Start the proxy using the start command and test whether it works (Join using the server's public address and the bind port you configured)
6. Have fun!

Here is an example command to allow players to join on yourserverip:25568 and connect to a beta 1.7.3 server running on port 25565:
``java -jar ViaProxy-whateverversion.jar --bind_address 0.0.0.0:25568 --target_ip 127.0.0.1:25565 --version b1.7-b1.7.3``

### Configuring the protocol translation
To change the protocol translation settings/features you can look into the ViaLoader folder.
You will find 5 config files there:
- viaversion.yml (ViaVersion)
- viabackwards.yml (ViaBackwards)
- viarewind.yml (ViaRewind)
- vialegacy.yml (ViaLegacy)
- viabedrock.yml (ViaBedrock)

### Developer Plugin API
ViaProxy has a plugin API which allows you to create plugins for ViaProxy.  
Documentation and examples:
- [NoLocalConnections](https://github.com/ViaVersionAddons/NoLocalConnections)
- [ViaProxyMultiLaunch](https://github.com/ViaVersionAddons/ViaProxyMultiLaunch)
- [ViaProxyGeyserPlugin](https://github.com/ViaVersionAddons/ViaProxyGeyserPlugin)
- [ViaProxyRakNetProviders](https://github.com/ViaVersionAddons/ViaProxyRakNetProviders)

## Contributing
Contributions in the form of pull requests are always welcome.
Please make sure to keep your code style consistent with the rest of the project and that your code is easily maintainable.
If you plan to make a large scale changes, please open an issue first or join my discord to discuss it.

### Translations
If you want to help translating ViaProxy you can do so by creating a pull request with your language file.
The language files are located in the [language folder](/src/main/resources/assets/language).
You can find the guidelines for creating a language file in the [en_US](/src/main/resources/assets/language/en_US.properties) language file.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/ViaVersion/ViaProxy/issues).  
If you just want to talk or need help using ViaProxy feel free to join the ViaVersion
[Discord](https://discord.gg/viaversion).
