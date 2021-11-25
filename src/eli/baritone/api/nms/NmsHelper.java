package eli.baritone.api.nms;

public class NmsHelper {

	public static int floor(double d) {
		// net.minecraft.server.v1_16_R3.MathHelper
		try {
			Class<?> mathHelper = NmsUtils.getNmsClass("MathHelper");
			return (int) mathHelper.getMethod("floor", double.class).invoke(null, d);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public static int getX(Object blockPos) {
		return get(blockPos, "getX");
	}
	
	public static int getY(Object blockPos) {
		return get(blockPos, "getY");
	}
	
	public static int getZ(Object blockPos) {
		return get(blockPos, "getZ");
	}
	
	private static int get(Object blockPos, String method) {
		try {
			return (int) blockPos.getClass().getMethod(method).invoke(blockPos);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
}
