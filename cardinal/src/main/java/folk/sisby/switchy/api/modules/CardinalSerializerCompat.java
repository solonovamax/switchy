package folk.sisby.switchy.api.modules;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author Sisby folk
 * @since 1.8.0
 * @see SwitchyModule
 * A generic module for switching cardinal entity component data using {@link Component#readFromNbt(NbtCompound)} and {@link Component#writeToNbt(NbtCompound)}
 */
public class CardinalSerializerCompat implements SwitchyModule {
	private record ComponentConfig<T1 extends Component>(ComponentKey<T1> registryKey, BiConsumer<ComponentKey<T1>, PlayerEntity> preApplyClear, BiConsumer<ComponentKey<T1>, PlayerEntity> postApplySync) {
		void invokePreApplyClear(PlayerEntity player) {
			preApplyClear.accept(registryKey, player);
		}

		void invokePostApplySync(PlayerEntity player) {
			postApplySync.accept(registryKey, player);
		}
	}

	// Generic Fields
	private final Map<Identifier, ComponentConfig<? extends Component>> componentConfigs;

	// Module Data
	private NbtCompound moduleNbt = new NbtCompound();

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		moduleNbt = new NbtCompound();
		componentConfigs.forEach((id, componentConfig) -> {
			NbtCompound componentCompound = new NbtCompound();
			Component component = componentConfig.registryKey.get(player);
			component.writeToNbt(componentCompound);
			moduleNbt.put(id.toString(), componentCompound);
		});
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		componentConfigs.forEach((id, componentConfig) -> {
			componentConfig.invokePreApplyClear(player);
			componentConfig.registryKey.get(player).readFromNbt(moduleNbt.getCompound(id.toString()));
			componentConfig.invokePostApplySync(player);
			componentConfig.registryKey.sync(player);
		});
	}

	@Override
	public NbtCompound toNbt() {
		return moduleNbt.copy();
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		moduleNbt.copyFrom(nbt);
	}

	private CardinalSerializerCompat(Map<Identifier, ComponentConfig<? extends Component>> componentConfigs) {
		this.componentConfigs = componentConfigs;
	}

	/**
	 * @param registryKey the key for the cardinal component
	 * @param preApplyClear operations to perform with the player before applying module data
	 * @param postApplySync operations to perform with the player after applying module data
	 * @param <T1> the component type
	 * @return a module instance for the specified cardinal component
	 * A generator for a module instance for a single cardinal component with additional arbitrary "clear" and "sync" callbacks.
	 * Intended for misbehaving cardinal modules that cause data leakage or desync when serialized/deserialized while in use.
	 */
	@SuppressWarnings("unused")
	public static <T1 extends Component> CardinalSerializerCompat from(ComponentKey<T1> registryKey, BiConsumer<ComponentKey<T1>, PlayerEntity> preApplyClear, BiConsumer<ComponentKey<T1>, PlayerEntity> postApplySync) {
		return new CardinalSerializerCompat(Map.of(registryKey.getId(), new ComponentConfig<>(registryKey, preApplyClear, postApplySync)));
	}

	/**
	 * @param id A unique identifier to associate with the module being registered.
	 * @param componentKeyIds a set of cardinal component key IDs to create the module for
	 * @param isDefault whether the module should be enabled by default
	 * @param editable permissions for cold-editing the module, see {@link SwitchyModuleEditable}
	 * @see SwitchyModuleRegistry
	 * Register a variant of this type of module using a supplier specific to the specified cardinal component.
	 * Equivalent to data-driven json modules loaded using {@link folk.sisby.switchy.CardinalModuleLoader}
	 */
	public static void register(Identifier id, Set<Identifier> componentKeyIds, Boolean isDefault, SwitchyModuleEditable editable) {
			SwitchyModuleRegistry.registerModule(id, () -> {
				Map<Identifier, ComponentConfig<?>> map = new HashMap<>();
				for (Identifier identifier : componentKeyIds) {
					ComponentKey<?> componentKey = ComponentRegistry.get(identifier);
					if (componentKey == null) {
						Switchy.LOGGER.warn("[Switchy] cardinal module {} failed to instantiate, as its component isn't created yet.", id);
						return null;
					}
					map.put(identifier, new ComponentConfig<>(componentKey, (k, p) -> {}, (k, p) -> {}));
				}
				return new CardinalSerializerCompat(map);
			}, new SwitchyModuleInfo(isDefault, editable, Set.of(), componentKeyIds));
	}
}
