package folk.sisby.switchy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.modules.CardinalSerializerModule;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.resource.loader.api.reloader.IdentifiableResourceReloader;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * A resource loader responsible for creating data-driven {@link SwitchyModule}s for swapping Cardinal Components API entity component data.
 * Will load json files in {@code data/{namespace}/switchy_cardinal/{path}.json} into modules with {@code namespace:path} IDs in the below format:
 * {@code
 * {
 * "default": boolean,
 * "editable": SwitchyModuleEditable,
 * "description": "A module that switches blargs from Chongo's blogging mod",
 * "ifModsLoaded": ["mod-id"],
 * "components": ["component-id"]
 * }
 * }
 *
 * @author Sisby folk
 * @see CardinalSerializerModule
 * @since 1.8.0
 */
public class CardinalModuleLoader extends JsonDataLoader implements IdentifiableResourceReloader {
	/**
	 * The global instance for this resource loader.
	 */
	public static final CardinalModuleLoader INSTANCE = new CardinalModuleLoader(new Gson());
	private static final Identifier ID = new Identifier(SwitchyCardinal.ID, "module_loader");
	private static final String KEY_DEFAULT = "default";
	private static final String KEY_EDITABLE = "editable";
	private static final String KEY_IF_MODS_LOADED = "ifModsLoaded";
	private static final String KEY_COMPONENTS = "components";
	private static final String KEY_PREVIEW = "preview";
	private static final String KEY_PREVIEW_CONDITION = "condition";
	private static final String KEY_PREVIEW_ICON = "icon";
	private static final String KEY_PREVIEW_VALUES = "values";

	CardinalModuleLoader(Gson gson) {
		super(gson, "switchy_cardinal");
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		prepared.forEach((moduleId, contents) -> {
			if (SwitchyModuleRegistry.containsModule(moduleId)) {
				return;
			}
			JsonObject moduleOptions = contents.getAsJsonObject();
			if (!moduleOptions.has(KEY_DEFAULT) || !moduleOptions.has(KEY_EDITABLE) || !moduleOptions.has(KEY_COMPONENTS)) {
				Switchy.LOGGER.warn("[Switchy] CCA module '{}' is missing options, skipping...", moduleId);
				return;
			}
			if (moduleOptions.has(KEY_IF_MODS_LOADED) && !StreamSupport.stream(moduleOptions.get(KEY_IF_MODS_LOADED).getAsJsonArray().spliterator(), true).map(JsonElement::getAsString).allMatch(QuiltLoader::isModLoaded)) {
				return;
			}
			try {
				SwitchyModuleEditable moduleEditable = SwitchyModuleEditable.valueOf(moduleOptions.get(KEY_EDITABLE).getAsString());
				boolean moduleDefault = moduleOptions.get(KEY_DEFAULT).getAsBoolean();
				Set<Identifier> componentIds = new HashSet<>();
				for (JsonElement componentEntry : moduleOptions.get(KEY_COMPONENTS).getAsJsonArray()) {
					Identifier componentId = Identifier.tryParse(componentEntry.getAsString());
					if (componentId == null) {
						Switchy.LOGGER.warn("[Switchy] Cardinal component '{}' from module {} is not a valid identifier, skipping...", componentEntry.getAsString(), moduleId);
						componentIds.clear();
						break;
					}
					componentIds.add(componentId);
				}
				if (!componentIds.isEmpty()) {
					try {
						SwitchyModuleInfo info = new SwitchyModuleInfo(moduleDefault, moduleEditable, Text.translatable("switchy.modules.%s.%s.description".formatted(moduleId.getNamespace(), moduleId.getPath())));
						info.withDescriptionWhenEnabled(Text.translatable(moduleOptions.get("switchy.modules.%s.%s.enabled".formatted(moduleId.getNamespace(), moduleId.getPath())).getAsString()));
						info.withDescriptionWhenDisabled(Text.translatable(moduleOptions.get("switchy.modules.%s.%s.disabled".formatted(moduleId.getNamespace(), moduleId.getPath())).getAsString()));
						info.withDeletionWarning(Text.translatable(moduleOptions.get("switchy.modules.%s.%s.warning".formatted(moduleId.getNamespace(), moduleId.getPath())).getAsString()));
						CardinalSerializerModule.register(moduleId, componentIds, info);
					} catch (IllegalStateException ignoredRegistrationEx) {
						Switchy.LOGGER.warn("[Switchy] CCA module {} tried to register a component that already has a module!, skipping...", moduleId);
					}
				}
			} catch (UnsupportedOperationException ignoredGetFromJsonEx) {
				Switchy.LOGGER.warn("[Switchy] CCA module '{}' has non-boolean options, skipping...", moduleId);
			} catch (IllegalArgumentException ignoredValueOfEx) {
				Switchy.LOGGER.warn("[Switchy] CCA module '{}' has invalid editable option, skipping...", moduleId);
			}
		});
	}

	@Override
	public @NotNull Identifier getQuiltId() {
		return ID;
	}
}
