package net.raphimc.viaproxy.injection.transformer;

import com.viaversion.viaversion.protocols.protocol1_9to1_8.storage.MovementTracker;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTransformer;

@CTransformer(MovementTracker.class)
public abstract class MovementTrackerTransformer {

    @CShadow
    private boolean ground = false;

}
