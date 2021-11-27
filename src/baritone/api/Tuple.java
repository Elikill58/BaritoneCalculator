package baritone.api;

public class Tuple<T, R> {

	private final T obj1;
	private final R obj2;
	
	public Tuple(T obj1, R obj2) {
		this.obj1 = obj1;
		this.obj2 = obj2;
	}
	
	public T getObj1() {
		return obj1;
	}
	
	public T a() {
		return obj1;
	}
	
	public R getObj2() {
		return obj2;
	}
	
	public R b() {
		return obj2;
	}
}
