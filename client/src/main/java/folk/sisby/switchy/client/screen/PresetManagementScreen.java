package folk.sisby.switchy.client.screen;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.presets.SwitchyDisplayPresets;
import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static folk.sisby.switchy.SwitchyClientServerNetworking.C2S_REQUEST_DISPLAY_PRESETS;
import static folk.sisby.switchy.api.presets.SwitchyPresetsData.KEY_PRESET_LIST;
import static folk.sisby.switchy.api.presets.SwitchyPresetsData.KEY_PRESET_MODULE_ENABLED;
import static folk.sisby.switchy.util.Feedback.*;


public class PresetManagementScreen extends BaseUIModelScreen<FlowLayout> implements SwitchyDisplayScreen {

	private FlowLayout root;
	private ScrollContainer<VerticalFlowLayout> presetsTab;
	private HorizontalFlowLayout modulesTab;
	private VerticalFlowLayout dataTab;
	private VerticalFlowLayout loadingOverlay;
	private List<Identifier> includedModules = new ArrayList<>();
	private List<Identifier> availableModules = new ArrayList<>();
	private NbtCompound selectedFileNbt;
	private boolean isImporting;

	public PresetManagementScreen() {
		super(FlowLayout.class, DataSource.asset(new Identifier("switchy", "preset_management_model")));
	}

	private DropdownComponent dropdownButton(DropdownComponent contextMenu, Text text) {
		DropdownComponent selectorButton = Components.dropdown(Sizing.content());
		selectorButton.text(text);
		selectorButton.mouseDown().subscribe((x, y, b) -> {
			if (!contextMenu.hasParent()) {
				root.child(contextMenu.positioning(Positioning.absolute(selectorButton.x(), selectorButton.y() + selectorButton.height())));
			} else {
				root.removeChild(contextMenu);
			}
			return true;
		});
		return selectorButton;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.root = rootComponent;
		// Preset Tab
		presetsTab = model.expandTemplate(ScrollContainer.class, "presets-tab", Map.of("id", "presetsTab"));
		VerticalFlowLayout presetsFlow = presetsTab.childById(VerticalFlowLayout.class, "presetsFlow");
		presetsFlow.gap(2);


		// Modules Tab
		modulesTab = model.expandTemplate(HorizontalFlowLayout.class, "modules-tab", Map.of());

		// Data Tab
		dataTab = model.expandTemplate(VerticalFlowLayout.class, "data-tab", Map.of());
		ButtonComponent importToggle = dataTab.childById(ButtonComponent.class, "importToggleButton");
		ButtonComponent exportToggle = dataTab.childById(ButtonComponent.class, "exportToggleButton");
		importToggle.onPress(b -> {
			isImporting = true;
			importToggle.active(false);
			exportToggle.active(true);
		});
		exportToggle.onPress(b -> {
			isImporting = false;
			exportToggle.active(false);
			importToggle.active(true);
		});


		VerticalFlowLayout sourceSelectorPlaceholder = dataTab.childById(VerticalFlowLayout.class, "sourceSelectorPlaceholder");
		sourceSelectorPlaceholder.child(dropdownButton(dropdownMaker(sourceSelectorPlaceholder, List.of(Text.of("File")), text -> {
		}), Text.of("File")));

		// Header
		VerticalFlowLayout panel = root.childById(VerticalFlowLayout.class, "panel");
		ButtonComponent backButton = root.childById(ButtonComponent.class, "backButton");
		ButtonComponent presetsTabButton = root.childById(ButtonComponent.class, "presetsTabButton");
		ButtonComponent modulesTabButton = root.childById(ButtonComponent.class, "modulesTabButton");
		ButtonComponent dataTabButton = root.childById(ButtonComponent.class, "dataTabButton");
		backButton.onPress(buttonComponent -> {
			client.setScreen(new SwitchScreen());
			ClientPlayNetworking.send(C2S_REQUEST_DISPLAY_PRESETS, PacketByteBufs.empty());
		});
		presetsTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(presetsTab);
			presetsTabButton.active(false);
			modulesTabButton.active(true);
			dataTabButton.active(true);

		});
		modulesTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(modulesTab);
			presetsTabButton.active(true);
			modulesTabButton.active(false);
			dataTabButton.active(true);
		});
		dataTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(dataTab);
			presetsTabButton.active(true);
			modulesTabButton.active(true);
			dataTabButton.active(false);
		});

		panel.child(presetsTab); // Default Tab
		presetsTabButton.active(false);
		lockScreen();
	}

	private void refreshPresetFlow(VerticalFlowLayout presetsFlow, SwitchyDisplayPresets displayPresets) {
		presetsFlow.clearChildren();
		displayPresets.getPresets().forEach((name, preset) -> {
			HorizontalFlowLayout presetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			LabelComponent presetName = Components.label(Text.literal(name));
			presetName.horizontalSizing(Sizing.fill(54));
			ButtonComponent renameButton = Components.button(Text.literal("Rename"), b -> {
				presetFlow.clearChildren();
				presetFlow.child(getRenameLayout(presetsFlow, name, displayPresets));
			});
			renameButton.horizontalSizing(Sizing.fill(22));
			Consumer<ButtonComponent> deleteAction = b -> {
				openDialog(
						"OK",
						"Cancel",
						200,
						okButton -> {
							displayPresets.deletePreset(name);
							refreshPresetFlow(presetsFlow, displayPresets);
							lockScreen();
							SwitchyClientApi.deletePreset(name);
						},
						cancel -> {
						},
						List.of(Text.translatable("commands.switchy_client.delete.confirm", name), Text.translatable("commands.switchy.delete.warn"), Text.translatable("commands.switchy.list.modules", displayPresets.getEnabledModuleText()))
				);
			};
			ButtonComponent deleteButton = Components.button(Text.literal("Delete"), (displayPresets.getCurrentPresetName().equals(name)) ? b -> {
			} : deleteAction);
			deleteButton.horizontalSizing(Sizing.fill(22));
			deleteButton.active(!displayPresets.getCurrentPresetName().equals(name));
			presetFlow.child(presetName);
			presetFlow.child(renameButton);
			presetFlow.child(deleteButton);
			presetFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
			presetFlow.gap(2);
			presetsFlow.child(presetFlow);
		});
	}


	void refreshDataModulesFlow(VerticalFlowLayout availableModulesFlow, VerticalFlowLayout includedModulesFlow, List<Identifier> availableModules, List<Identifier> includedModules, SwitchyDisplayPresets displayPresets) {
		availableModulesFlow.clearChildren();
		includedModulesFlow.clearChildren();
		int labelSize = 100;
		availableModulesFlow.child(getModuleFlow(
				new Identifier("placeholder", "placeholder"),
				Text.literal(""),
				(b, i) -> {
				},
				Text.literal("Add"),
				Text.literal(""),
				labelSize
		).verticalSizing(Sizing.fixed(0)));
		includedModulesFlow.child(getModuleFlow(
				new Identifier("placeholder", "placeholder"),
				Text.literal(""),
				(b, i) -> {
				},
				Text.literal("Remove"),
				Text.literal(""),
				labelSize
		).verticalSizing(Sizing.fixed(0)));

		// Available Modules
		availableModules.forEach(module -> {
			availableModulesFlow.child(getModuleFlow(module,
					displayPresets.getModuleInfo().get(module).description(),
					(b, id) -> {
						includedModules.add(module);
						availableModules.remove(module);
						refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules, displayPresets);
					},
					Text.literal("Add"),
					displayPresets.getModuleInfo().get(module).descriptionWhenEnabled(),
					labelSize
			));
		});
		// Included Modules
		includedModules.forEach(module -> {
			includedModulesFlow.child(getModuleFlow(
					module,
					displayPresets.getModuleInfo().get(module).description(),
					(b, id) -> {
						availableModules.add(module);
						includedModules.remove(module);
						refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules, displayPresets);
					},
					Text.literal("Remove"),
					displayPresets.getModuleInfo().get(module).descriptionWhenEnabled(),
					labelSize
			));
		});
	}

	private void refreshModulesFlow(VerticalFlowLayout disabledModulesFlow, VerticalFlowLayout enabledModulesFlow, SwitchyDisplayPresets displayPresets) {
		disabledModulesFlow.clearChildren();
		enabledModulesFlow.clearChildren();
		int labelSize = 100;
		disabledModulesFlow.child(getModuleFlow(
				new Identifier("placeholder", "placeholder"),
				Text.literal(""),
				(b, i) -> {
				},
				Text.literal("Enable"),
				Text.literal(""),
				labelSize
		).verticalSizing(Sizing.fixed(0)));
		enabledModulesFlow.child(getModuleFlow(
				new Identifier("placeholder", "placeholder"),
				Text.literal(""),
				(b, i) -> {
				},
				Text.literal("Disable"),
				Text.literal(""),
				labelSize
		).verticalSizing(Sizing.fixed(0)));

		// Disabled Modules
		displayPresets.getDisabledModules().forEach(module -> {
			disabledModulesFlow.child(getModuleFlow(module,
					displayPresets.getModuleInfo().get(module).description(),
					(b, id) -> {
						displayPresets.enableModule(id);
						refreshModulesFlow(disabledModulesFlow, enabledModulesFlow, displayPresets);
						lockScreen();
						SwitchyClientApi.enableModule(id);
					},
					Text.literal("Enable"),
					displayPresets.getModuleInfo().get(module).descriptionWhenEnabled(),
					labelSize
			));
		});
		// Enabled Modules
		displayPresets.getEnabledModules().forEach(module -> {
			enabledModulesFlow.child(getModuleFlow(
					module,
					displayPresets.getModuleInfo().get(module).description(),
					(b, id) -> {
						openDialog(
								"OK",
								"Cancel",
								200,
								okButton -> {
									displayPresets.disableModule(id);
									refreshModulesFlow(disabledModulesFlow, enabledModulesFlow, displayPresets);
									lockScreen();
									SwitchyClientApi.disableModule(id);
								},
								cancel -> {
								},
								List.of(Text.translatable("commands.switchy_client.disable.confirm", id.toString()), Text.translatable("commands.switchy.module.disable.warn", displayPresets.getModuleInfo().get(id).deletionWarning()))
						);
					},
					Text.literal("Disable"),
					displayPresets.getModuleInfo().get(module).descriptionWhenDisabled(),
					labelSize
			));
		});
	}

	private HorizontalFlowLayout getRenameLayout(VerticalFlowLayout presetsFlow, @Nullable String presetName, SwitchyDisplayPresets displayPresets) {
		HorizontalFlowLayout renamePresetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		TextBoxComponent nameEntry = Components.textBox(Sizing.fill(54), (presetName != null) ? presetName : "newPreset");
		nameEntry.setTextPredicate(s -> s.chars().mapToObj(i -> (char) i).allMatch(StringReader::isAllowedInUnquotedString));
		this.setInitialFocus(nameEntry);
		renamePresetFlow.child(nameEntry);
		ButtonComponent confirmButton = Components.button(Text.literal("Confirm"), (presetName != null) ? b -> {
			if (!presetName.equals(nameEntry.getText())) {
				if (displayPresets.getPresetNames().stream().noneMatch(s -> s.equalsIgnoreCase(nameEntry.getText()))) {
					displayPresets.renamePreset(presetName, nameEntry.getText());
					refreshPresetFlow(presetsFlow, displayPresets);
					lockScreen();
					SwitchyClientApi.renamePreset(presetName, nameEntry.getText());
				}
			} else {
				refreshPresetFlow(presetsFlow, displayPresets);
			}

		} : b -> {
			if (displayPresets.getPresetNames().stream().noneMatch(s -> s.equalsIgnoreCase(nameEntry.getText()))) {
				displayPresets.newPreset(nameEntry.getText());
				refreshPresetFlow(presetsFlow, displayPresets);
				lockScreen();
				SwitchyClientApi.newPreset(nameEntry.getText());
			}

		});
		confirmButton.horizontalSizing(Sizing.fill(22));
		ButtonComponent cancelButton = Components.button(Text.literal("Cancel"), b -> {
			refreshPresetFlow(presetsFlow, displayPresets);
		});
		cancelButton.horizontalSizing(Sizing.fill(22));
		renamePresetFlow.child(confirmButton);
		renamePresetFlow.child(cancelButton);
		renamePresetFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
		renamePresetFlow.gap(2);
		return renamePresetFlow;
	}

	VerticalFlowLayout openDialog(String leftButtonText, String rightButtonText, int hSize, Consumer<ButtonComponent> leftButtonAction, Consumer<ButtonComponent> rightButtonAction, List<Text> messages) {
		VerticalFlowLayout dialog = model.expandTemplate(VerticalFlowLayout.class, "dialog-box", Map.of("leftText", leftButtonText, "rightText", rightButtonText, "hSize", String.valueOf(hSize)));
		VerticalFlowLayout messageFlow = dialog.childById(VerticalFlowLayout.class, "messageFlow");
		ButtonComponent leftButton = dialog.childById(ButtonComponent.class, "leftButton");
		ButtonComponent rightButton = dialog.childById(ButtonComponent.class, "rightButton");
		leftButton.onPress(leftB -> {
			leftButtonAction.accept(leftB);
			root.removeChild(dialog);
		});
		rightButton.onPress(rightB -> {
			rightButtonAction.accept(rightB);
			root.removeChild(dialog);
		});
		messageFlow.gap(2);
		messages.forEach(m ->
		{
			LabelComponent message = Components.label(m);
			message.horizontalSizing(Sizing.fill(90));
			messageFlow.child(message);
		});

		root.child(dialog);
		return dialog;
	}

	HorizontalFlowLayout getModuleFlow(
			Identifier id,
			@Nullable Text labelTooltip,
			BiConsumer<ButtonComponent, Identifier> buttonAction,
			Text buttonText,
			@Nullable Text buttonTooltip,
			int labelSize
	) {
		HorizontalFlowLayout moduleFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		LabelComponent moduleName = Components.label(Text.literal(id.toString()));
		moduleName.tooltip(labelTooltip);
		moduleName.horizontalSizing(Sizing.fixed(labelSize));
		ButtonComponent enableButton = Components.button(buttonText, b -> {
			buttonAction.accept(b, id);
		});
		enableButton.tooltip(buttonTooltip);
		enableButton.horizontalSizing(Sizing.content());
		moduleFlow.child(moduleName);
		moduleFlow.child(enableButton);
		moduleFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
		moduleFlow.gap(2);
		return moduleFlow;
	}

	void lockScreen() {
		if (loadingOverlay == null) {
			loadingOverlay = model.expandTemplate(VerticalFlowLayout.class, "loading-overlay", Map.of());
			root.child(loadingOverlay);
		}
	}

	@Override
	public void updatePresets(SwitchyDisplayPresets displayPresets) {
		// Presets Tab
		VerticalFlowLayout presetsFlow = presetsTab.childById(VerticalFlowLayout.class, "presetsFlow");
		refreshPresetFlow(presetsFlow, displayPresets);

		//Modules Tab
		VerticalFlowLayout disabledModulesFlow = modulesTab.childById(VerticalFlowLayout.class, "leftModulesFlow");
		VerticalFlowLayout enabledModulesFlow = modulesTab.childById(VerticalFlowLayout.class, "rightModulesFlow");
		refreshModulesFlow(disabledModulesFlow, enabledModulesFlow, displayPresets);

		//Data Tab
		VerticalFlowLayout availableModulesFlow = dataTab.childById(VerticalFlowLayout.class, "leftModulesFlow");
		VerticalFlowLayout includedModulesFlow = dataTab.childById(VerticalFlowLayout.class, "rightModulesFlow");
		ButtonComponent importButton = dataTab.childById(ButtonComponent.class, "importButton");
		importButton.onPress(b -> {
			openDialog(
					"Confirm",
					"Cancel",
					200,
					confirmButton -> {
						lockScreen();
						SwitchyClientApi.importPresets(selectedFileNbt, availableModules, includedModules);
					},
					cancelButton -> {
					},
					List.of(
							Text.translatable("commands.switchy.import.warn.info", literal(String.valueOf(selectedFileNbt.getCompound(KEY_PRESET_LIST).getKeys().size())), literal(String.valueOf(includedModules.size()))),
							Text.translatable("commands.switchy.list.presets", getHighlightedListText(selectedFileNbt.getCompound(KEY_PRESET_LIST).getKeys().stream().sorted().toList(), List.of(new Pair<>(displayPresets.getPresetNames()::contains, Formatting.DARK_RED)))),
							Text.translatable("commands.switchy.import.warn.collision"),
							Text.translatable("commands.switchy.list.modules", getIdListText(includedModules))
					)
			);
		});

		presetsTab.childById(ButtonComponent.class, "newPreset").onPress(buttonComponent -> {
			presetsFlow.child(getRenameLayout(presetsFlow, null, displayPresets));
		});
		if (loadingOverlay != null) {
			root.removeChild(loadingOverlay);
			loadingOverlay = null;
		}

		// Data tab
		Map<String, NbtCompound> importFiles = new HashMap<>();
		File[] fileArray = new File(SwitchyClient.EXPORT_PATH).listFiles((dir, name) -> FileNameUtils.getExtension(name).equalsIgnoreCase("dat"));
		if (fileArray != null) {
			for (File file : fileArray) {
				try {
					NbtCompound nbt = NbtIo.readCompressed(file);
					nbt.putString("filename", FilenameUtils.getBaseName(file.getName()));

					String name = file.getName();
					String baseName = FileNameUtils.getBaseName(name);
					importFiles.put(baseName, nbt);
				} catch (IOException ignored) {
				}
			}
		}

		VerticalFlowLayout fileSelectorPlaceholder = dataTab.childById(VerticalFlowLayout.class, "fileSelectorPlaceholder");
		List<Text> fileNames = importFiles.keySet().stream().map(Text::of).toList();
		fileSelectorPlaceholder.clearChildren();
		fileSelectorPlaceholder.child(dropdownButton(dropdownMaker(fileSelectorPlaceholder, fileNames, text -> {
			selectedFileNbt = importFiles.get(text.getString());
			includedModules = selectedFileNbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).filter(id -> {
				SwitchyModuleInfo moduleInfo = displayPresets.getModuleInfo().get(id);
				if (moduleInfo == null) return false;
				return moduleInfo.editable() == SwitchyModuleEditable.ALLOWED || moduleInfo.editable() == SwitchyModuleEditable.ALWAYS_ALLOWED;
			}).collect(Collectors.toList());
			availableModules = selectedFileNbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).filter(id -> {
				SwitchyModuleInfo moduleInfo = displayPresets.getModuleInfo().get(id);
				if (moduleInfo == null) return false;
				return moduleInfo.editable() == SwitchyModuleEditable.OPERATOR;
			}).collect(Collectors.toList());

			refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules, displayPresets);
		}), Text.of("Select a file...")));

		refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules, displayPresets);
	}

	DropdownComponent dropdownMaker(FlowLayout placeholder, List<Text> entries, Consumer<Text> onSelected) {
		DropdownComponent dropdown = Components.dropdown(Sizing.content());

		for (Text entry : entries) {
			dropdown.button(entry, dropdownComponent -> {
				root.removeChild(dropdown);
				placeholder.clearChildren();
				placeholder.child(dropdownButton(dropdown, entry));
				onSelected.accept(entry);
			});
		}
		return dropdown;
	}
}
