package baritone.api.nms;

@SuppressWarnings("unchecked")
public class ReflectionUtils {
	
	public static <T> T invoke(Object o, String name) {
		try {
			return (T) o.getClass().getMethod(name).invoke(o);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static <T> T field(Object o, String name) {
		try {
			return (T) o.getClass().getDeclaredField(name).get(o);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
