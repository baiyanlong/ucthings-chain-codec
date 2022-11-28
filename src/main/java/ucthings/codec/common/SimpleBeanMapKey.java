package ucthings.codec.common;


/**
 * bean map key，提高性能
 *
 * @author L.cm
 */
public class SimpleBeanMapKey {
	private final Class type;
	private final int require;

	public SimpleBeanMapKey(Class type, int require) {
		this.type = type;
		this.require = require;
	}

	public Class getType() {
		return type;
	}

	public int getRequire() {
		return require;
	}
}
