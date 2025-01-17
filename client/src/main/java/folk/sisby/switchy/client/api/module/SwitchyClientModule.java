package folk.sisby.switchy.client.api.module;

import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.module.SwitchyModuleTransferable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * Holds a client-appropriate representation of the contents of a Switchy Module.
 * The represented module must implement {@link SwitchyModuleTransferable}.
 *
 * @author Sisby folk
 * @see SwitchySerializable
 * @see folk.sisby.switchy.api.module.SwitchyModule
 * @since 2.0.0
 */
@ClientOnly
public interface SwitchyClientModule extends SwitchySerializable {
}
