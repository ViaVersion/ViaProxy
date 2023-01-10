package net.raphimc.viaproxy.cli.options;

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;
import net.raphimc.viaprotocolhack.util.VersionEnum;

public class VersionEnumConverter implements ValueConverter<VersionEnum> {

    @Override
    public VersionEnum convert(String s) {
        for (VersionEnum version : VersionEnum.getAllVersions()) {
            if (version.getName().equalsIgnoreCase(s)) return version;
        }
        throw new ValueConversionException("Unable to find version '" + s + "'");
    }

    @Override
    public Class<VersionEnum> valueType() {
        return VersionEnum.class;
    }

    @Override
    public String valuePattern() {
        StringBuilder s = new StringBuilder();
        for (VersionEnum version : VersionEnum.getAllVersions()) {
            s.append((s.length() == 0) ? "" : ", ").append(version.getName());
        }
        return "[" + s + "]";
    }

}
