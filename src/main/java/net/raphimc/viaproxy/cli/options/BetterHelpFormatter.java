package net.raphimc.viaproxy.cli.options;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.internal.Classes;
import joptsimple.internal.Strings;

public class BetterHelpFormatter extends BuiltinHelpFormatter {

    public BetterHelpFormatter() {
        super(250, 4);
    }

    @Override
    protected String extractTypeIndicator(OptionDescriptor descriptor) {
        String indicator = descriptor.argumentTypeIndicator();
        if (indicator != null && indicator.startsWith("[")) return indicator.substring(1, indicator.length() - 1);
        return !Strings.isNullOrEmpty(indicator) && !String.class.getName().equals(indicator) ? Classes.shortNameOf(indicator) : "String";
    }

}
