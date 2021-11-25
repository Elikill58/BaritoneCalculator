package eli.baritone.api.nms;

public abstract class AbstractContext {

	protected final Object obj;
	
	public AbstractContext(Object obj) {
		this.obj = obj;
	}
	
	public Object getObject() {
		return obj;
	}
	
	protected <T> T invoke(String name) {
		return ReflectionUtils.invoke(obj, name);
	}
	
	protected <T> T invoke(Object o, String name) {
		return ReflectionUtils.invoke(o, name);
	}
	
	protected <T> T field(String name) {
		return ReflectionUtils.field(obj, name);
	}
	
	protected <T> T field(Object o, String name) {
		return ReflectionUtils.field(o, name);
	}
}
