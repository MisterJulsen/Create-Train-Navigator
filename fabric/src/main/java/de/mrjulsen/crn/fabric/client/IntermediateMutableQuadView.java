package de.mrjulsen.crn.fabric.client;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.fabric.compat.IndiumMutableQuadView;
import dev.architectury.platform.Platform;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
public class IntermediateMutableQuadView extends MutableQuadViewImpl {
    private IntermediateMutableQuadView() {
        data = new int[EncodingFormat.TOTAL_STRIDE];
        clear();
    }

    @Override
    public void emitDirectly() {
        throw new NotImplementedException("IntermediateMutableQuadView.emitDirectly() is not implemented");
    }

    /**
     * Embeddium registers a stub for Indium but does not provide Indium classes,
     * so classes need to be checked for existence at runtime.
     */
    private static final AtomicBoolean indiumAvailable = new AtomicBoolean(true);

    public static MutableQuadView create() {
        if (!indiumAvailable.get()) {
            return new IntermediateMutableQuadView();
        }
        return Platform.getOptionalMod("indium").map(mod -> {
            try {
                return new IndiumMutableQuadView();
            } catch (NoClassDefFoundError t) {
                if (indiumAvailable.compareAndSet(true, false)) {
                    CreateRailwaysNavigator.LOGGER.warn("Failed to load Indium classes, falling back to Fabric API", t);
                }
            }
            return new IntermediateMutableQuadView();
        }).orElse(new IntermediateMutableQuadView());
    }
}
