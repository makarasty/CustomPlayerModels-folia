package com.tom.cpmoscc;

import com.tom.cpm.api.CPMPlugin;
import com.tom.cpm.api.ICPMPlugin;
import com.tom.cpm.api.IClientAPI;
import com.tom.cpm.api.ICommonAPI;
import com.tom.cpmoscc.gui.OSCSettingsPopup;
import com.tom.cpmoscc.gui.OSCWizardPopup;

@CPMPlugin
public class CPMOSCPlugin implements ICPMPlugin {

	@Override
	public void initClient(IClientAPI api) {
		CPMOSC.api = api;
		api.registerEditorGenerator("osc-button.cpmosc.oscSettings", "osc-tooltip.cpmosc.oscSettings", eg -> {
			eg.openPopup(new OSCSettingsPopup(eg));
		});

		api.registerEditorGenerator("osc-button.cpmosc.oscWizard", "osc-tooltip.cpmosc.oscWizard", eg -> {
			eg.openPopup(new OSCWizardPopup(eg));
		});
		api.registerClientGameTick(paused -> {
			if (paused)return;
			CPMOSC.tick(GetClientPlayer.get());
		});
		GetClientPlayer.init();
		CPMOSC.LOGGER.info("CPM OSC initialized");
	}

	@Override
	public void initCommon(ICommonAPI api) {
	}

	@Override
	public String getOwnerModId() {
		return CPMOSC.MOD_ID;
	}

}
