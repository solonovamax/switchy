package folk.sisby.switchy.client.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.datafixers.util.Pair;
import com.mojang.util.UUIDTypeAdapter;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModuleRegistry;
import folk.sisby.switchy.modules.FabricTailorCompatData;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.Base64;
import java.util.UUID;

@ClientOnly
public class FabricTailorCompatDisplay extends FabricTailorCompatData implements SwitchyDisplayModule {
	@Override
	public Pair<Component, SwitchySwitchScreenPosition> getDisplayComponent() {
		if (skinValue == null) return null;
		MinecraftClient client = MinecraftClient.getInstance();

		Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
		MinecraftTexturesPayload payload = gson.fromJson(new String(Base64.getDecoder().decode(skinValue)), MinecraftTexturesPayload.class);
		MinecraftProfileTexture skinTexture = payload.getTextures().get(MinecraftProfileTexture.Type.SKIN);

		Identifier skinId = client.getSkinProvider().loadSkin(skinTexture, MinecraftProfileTexture.Type.SKIN);

		EntityComponent<AbstractClientPlayerEntity> skinPreview = Components.entity(Sizing.fixed(60), new AbstractClientPlayerEntity(client.world, client.getSession().getProfile(), null) {
			@Override
			public Identifier getSkinTexture() {
				return skinId;
			}
		});

		skinPreview.scale(0.5F);

		return Pair.of(skinPreview, SwitchySwitchScreenPosition.SIDE_RIGHT);
	}

	public static void touch() {}

	static {
		SwitchyDisplayModuleRegistry.registerModule(ID, FabricTailorCompatDisplay::new);
	}
}