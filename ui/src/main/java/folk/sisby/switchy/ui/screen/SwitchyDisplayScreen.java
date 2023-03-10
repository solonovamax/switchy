package folk.sisby.switchy.ui.screen;

import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.SwitchyFeedback;
import folk.sisby.switchy.ui.util.FeedbackToast;
import net.minecraft.client.MinecraftClient;

public interface SwitchyDisplayScreen {
	void updatePresets(SwitchyClientPresets displayPresets);

	static void updatePresetScreens(SwitchyFeedback feedback, SwitchyClientPresets presets) {
		MinecraftClient client = MinecraftClient.getInstance();
		client.execute(() -> {
			if (client.currentScreen instanceof SwitchyDisplayScreen displayScreen) {
				displayScreen.updatePresets(presets);
				FeedbackToast.report(feedback, 4000);
			}
		});
	}
}