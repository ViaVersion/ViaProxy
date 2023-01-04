package net.raphimc.viaproxy.plugins.events.types;

public interface ITyped {

    Type getType();


    enum Type {
        PRE, POST
    }

}
