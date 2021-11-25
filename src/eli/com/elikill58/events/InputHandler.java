package eli.com.elikill58.events;

import java.util.HashMap;

import eli.baritone.api.utils.Rotation;
import eli.baritone.api.utils.input.Input;

public class InputHandler {
	
	private HashMap<Input, Boolean> inputs = new HashMap<>();
	
	public void clearAllKeys() {
		inputs.clear();
		//EliPlugin.getInstance().debug("Clear keys");
	}

	public void stopBreakingBlock() {
		inputs.remove(Input.CLICK_LEFT);
		//EliPlugin.getInstance().debug("Stop break block");
	}

	public void setInputForceState(Input input, boolean b) {
		inputs.put(input, b);
		//EliPlugin.getInstance().debug("Set input " + input.name() + " as " + b);
	}

	public void updateTarget(Rotation rotate, boolean b) {
		
	}

	public boolean isInputForcedDown(Input sprint) {
		return inputs.getOrDefault(sprint, false);
	}
	
	
}
