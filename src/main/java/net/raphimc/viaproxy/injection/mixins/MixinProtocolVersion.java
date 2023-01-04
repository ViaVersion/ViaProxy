package net.raphimc.viaproxy.injection.mixins;

import com.google.common.collect.ImmutableSet;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionRange;
import com.viaversion.viaversion.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = ProtocolVersion.class, remap = false)
public abstract class MixinProtocolVersion {

    @Unique
    private static Set<String> skips;

    @Unique
    private static Map<String, Pair<String, VersionRange>> remaps;

    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void initMaps(CallbackInfo ci) {
        skips = ImmutableSet.of("1.4.6/7", "1.5.1", "1.5.2", "1.6.1", "1.6.2", "1.6.3", "1.6.4");
        remaps = new HashMap<>();
        remaps.put("1.7-1.7.5", new Pair<>("1.7.2-1.7.5", new VersionRange("1.7", 2, 5)));
        remaps.put("1.9.3/4", new Pair<>("1.9.3-1.9.4", null));
        remaps.put("1.11.1/2", new Pair<>("1.11.1-1.11.2", null));
        remaps.put("1.16.4/5", new Pair<>("1.16.4-1.16.5", null));
        remaps.put("1.18/1.18.1", new Pair<>("1.18-1.18.1", null));
        remaps.put("1.19.1/2", new Pair<>("1.19.1-1.19.2", null));
    }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;register(ILjava/lang/String;)Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;"))
    private static ProtocolVersion unregisterAndRenameVersions(int version, String name) {
        if (skips.contains(name)) return null;
        final Pair<String, VersionRange> remapEntry = remaps.get(name);
        if (remapEntry != null) {
            if (remapEntry.key() != null) name = remapEntry.key();
        }

        return ProtocolVersion.register(version, name);
    }

    @SuppressWarnings({"UnresolvedMixinReference", "MixinAnnotationTarget", "InvalidInjectorMethodSignature"}) // Optional injection
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;register(IILjava/lang/String;)Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;"), require = 0)
    private static ProtocolVersion unregisterAndRenameVersions(int version, int snapshotVersion, String name) {
        if (skips.contains(name)) return null;
        final Pair<String, VersionRange> remapEntry = remaps.get(name);
        if (remapEntry != null) {
            if (remapEntry.key() != null) name = remapEntry.key();
        }

        return ProtocolVersion.register(version, snapshotVersion, name);
    }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;register(ILjava/lang/String;Lcom/viaversion/viaversion/api/protocol/version/VersionRange;)Lcom/viaversion/viaversion/api/protocol/version/ProtocolVersion;"))
    private static ProtocolVersion unregisterAndRenameVersions(int version, String name, VersionRange versionRange) {
        if (skips.contains(name)) return null;
        final Pair<String, VersionRange> remapEntry = remaps.get(name);
        if (remapEntry != null) {
            if (remapEntry.key() != null) name = remapEntry.key();
            if (remapEntry.value() != null) versionRange = remapEntry.value();
        }

        return ProtocolVersion.register(version, name, versionRange);
    }

}
