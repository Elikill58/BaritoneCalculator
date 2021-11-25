package eli.baritone.api.nms.player;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import eli.baritone.api.nms.AbstractContext;

public class InventoryContext extends AbstractContext {
	
	public InventoryContext(Inventory inv) {
		super(inv);
	}
	
	public Object getInventory() {
		return getObject();
	}
	
	public int getItemInHandIndex() {
		return field("itemInHandIndex");
	}
	
	public int getSlot(Object item) {
		try {
			return (int) obj.getClass().getDeclaredMethod("c", item.getClass()).invoke(obj, item);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public ItemStack getItem(int bestSlot) {
		return ((Inventory) obj).getItem(bestSlot);
	}
}
