package folk.sisby.switchy.api.modules;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Optional;

public class CardinalAutoSyncCompat<T1 extends AutoSyncedComponent> implements PresetModule {
	private final Identifier ID;
	private final boolean isDefault;

	// Generic Fields
	private final ComponentKey<T1> registryKey;
	private final TriConsumer<ComponentKey<T1>, T1, PlayerEntity> preApplyClear;
	private final TriConsumer<ComponentKey<T1>, T1, PlayerEntity> postApplySync;

	// Module Data
	private NbtCompound componentTag = new NbtCompound();

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		T1 component = registryKey.get(player);
		this.componentTag = new NbtCompound();
		component.writeToNbt(componentTag);
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		T1 component = registryKey.get(player);
		preApplyClear.accept(registryKey, component, player);
		component.readFromNbt(componentTag);
		postApplySync.accept(registryKey, component, player);
		registryKey.sync(player);
	}

	@Override
	public NbtCompound toNbt() {
		return componentTag.copy();
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.componentTag.copyFrom(nbt);
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	public CardinalAutoSyncCompat(Identifier id, ComponentKey<T1> registryKey, Boolean isDefault, TriConsumer<ComponentKey<T1>, T1, PlayerEntity> preApplyClear, TriConsumer<ComponentKey<T1>, T1, PlayerEntity> postApplySync) {
		this.registryKey = registryKey;
		this.ID = id;
		this.isDefault = isDefault;
		this.preApplyClear = preApplyClear;
		this.postApplySync = postApplySync;
	}

	public CardinalAutoSyncCompat(Identifier id, ComponentKey<T1> registryKey, Boolean isDefault) {
		this(id, registryKey, isDefault, (k, c, p) -> {
		}, (k, c, p) -> {
		});
	}

	public static Optional<ComponentKey<? extends AutoSyncedComponent>> keyFromId(Identifier id) {
		try {
			return Optional.ofNullable((ComponentKey<? extends AutoSyncedComponent>) ComponentRegistry.get(id));
		} catch (ClassCastException e) {
			return Optional.empty();
		}
	}

	public static void tryRegister(Identifier moduleId, Identifier componentKeyId, Boolean isDefault) {
		keyFromId(componentKeyId).ifPresent((key) -> PresetModuleRegistry.registerModule(moduleId, () -> new CardinalAutoSyncCompat<>(moduleId, key, isDefault)));
	}
}
