package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;

/**
 * A collection of {@link SwitchyPreset}, holding data, and a reference to their {@link SwitchySerializable} modules.
 * All contained presets have identical modules (called "enabled" modules).
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public interface SwitchyPresetsData<Module extends SwitchySerializable, Preset extends SwitchyPresetData<Module>> extends SwitchySerializable {
	/**
	 * NBT key for the current preset.
	 */
	String KEY_PRESET_CURRENT = "current";
	/**
	 * NBT key for the list of enabled modules.
	 */
	String KEY_PRESET_MODULE_ENABLED = "enabled";
	/**
	 * NBT key for the list of disabled modules.
	 */
	String KEY_PRESET_MODULE_DISABLED = "disabled";
	/**
	 * NBT key for the list of presets.
	 */
	String KEY_PRESET_LIST = "list";

	/**
	 * Imports a set of presets, merging by replacing modules where preset names collide.
	 * Only registered, enabled modules will be imported.
	 *
	 * @param other a map of presets to import into this object.
	 */
	void importFromOther(Map<String, Preset> other);

	/**
	 * Adds a preset to this object.
	 *
	 * @param preset a named preset object.
	 * @throws IllegalStateException when a preset with the provided name already exists
	 */
	void addPreset(Preset preset) throws IllegalStateException;

	/**
	 * Creates a new preset and adds it to this object.
	 *
	 * @param name the desired name for the new preset.
	 * @return the newly created preset.
	 * @throws IllegalStateException when a preset with the provided name already exists
	 */
	Preset newPreset(String name) throws IllegalStateException;

	/**
	 * Deletes a preset from this object.
	 * For use in situations where throwable-based validation is desired before confirming the action.
	 *
	 * @param name   the case-insensitive name of a preset.
	 * @param dryRun whether to skip deleting the preset.
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 * @throws IllegalStateException    when the preset with the specified name is the current preset
	 * @see SwitchyPresets#deletePreset(String)
	 */
	@ApiStatus.Internal
	void deletePreset(String name, boolean dryRun) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Deletes a preset from this object.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 * @throws IllegalStateException    when the preset with the specified name is the current preset
	 *                                  Deletes the specified preset, along with any associated data.
	 */
	void deletePreset(String name) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Safely changes the name of the specified preset.
	 *
	 * @param name    the case-insensitive name of a preset.
	 * @param newName the new name for the specified preset. a single word matching {@code azAZ09_-.+}.
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 * @throws IllegalStateException    when a preset with the provided name already exists
	 */
	void renamePreset(String name, String newName) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Disables a module, deleting its instances from every preset.
	 * For use in situations where throwable-based validation is desired before confirming the action.
	 *
	 * @param id     a module identifier.
	 * @param dryRun whether to skip disabling the module.
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is disabled
	 * @see SwitchyPresets#disableModule(Identifier)
	 */
	@ApiStatus.Internal
	void disableModule(Identifier id, boolean dryRun) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Disables a module, deleting its instances from every preset.
	 *
	 * @param id a module identifier.
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is disabled
	 */
	void disableModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Enables a module, creating empty instances in every preset.
	 *
	 * @param id a module identifier.
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is enabled
	 */
	void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Gets the current preset.
	 *
	 * @return the current preset.
	 */
	@ApiStatus.Internal
	Preset getCurrentPreset();

	/**
	 * Unsafely sets the current preset reference without switching any data.
	 * Causes data loss when the object is in use.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 */
	@ApiStatus.Internal
	void setCurrentPreset(String name) throws IllegalArgumentException;

	/**
	 * Gets the name of the current preset.
	 *
	 * @return the name of the current preset.
	 */
	String getCurrentPresetName();

	/**
	 * Gets all contained presets.
	 *
	 * @return all contained presets.
	 */
	@ApiStatus.Internal
	Map<String, Preset> getPresets();

	/**
	 * Gets the specified preset.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @return the specified preset.
	 */
	@ApiStatus.Internal
	Preset getPreset(String name);

	/**
	 * Gets a list of all preset names.
	 *
	 * @return a list of each preset name.
	 */
	List<String> getPresetNames();

	/**
	 * Whether a preset with the specified name exists.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @return true if the preset exists, false otherwise.
	 */
	boolean containsPreset(String name);

	/**
	 * Gets a map representing the enabled status of all modules.
	 *
	 * @return the enabled status of all modules.
	 */
	@ApiStatus.Internal
	Map<Identifier, Boolean> getModules();

	/**
	 * Gets every instance of a module mapped by preset name.
	 *
	 * @param id a module identifier.
	 * @return a map of each preset to the specified module.
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is disabled
	 */
	Map<String, Module> getAllOfModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Gets a list of all enabled modules as IDs.
	 *
	 * @return a list of all enabled module IDs.
	 */
	List<Identifier> getEnabledModules();

	/**
	 * * Gets a list of all disabled modules as IDs.
	 *
	 * @return a list of all disabled module IDs.
	 */
	List<Identifier> getDisabledModules();

	/**
	 * Whether a module is registered in this object.
	 * Registered modules can be enabled and disabled.
	 *
	 * @param id a module identifier.
	 * @return true if a module is registered, false otherwise.
	 */
	boolean containsModule(Identifier id);

	/**
	 * Whether a specified module is enabled.
	 *
	 * @param id a module identifier.
	 * @return true if the module is enabled, false otherwise.
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 */
	boolean isModuleEnabled(Identifier id) throws IllegalArgumentException;

	/**
	 * Gets a list of all enabled modules as ID paths.
	 *
	 * @return a list of all enabled module names (paths).
	 */
	List<String> getEnabledModuleNames();

	/**
	 * Creates a compact text representation of all modules.
	 *
	 * @return a text representation of all module IDs, showing paths with full IDs on hover.
	 */
	MutableText getEnabledModuleText();

	/**
	 * @return the names of every preset as a formatted list.
	 */
	@Override
	String toString();
}
