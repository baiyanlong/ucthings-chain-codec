package ucthings.codec.common.util;


/**
 * copy key
 *
 * @author L.cm
 */
public class BeanCopierKey {
	private final Class<?> source;
	private final Class<?> target;
	private final boolean useConverter;
	private final boolean nonNull;

	public BeanCopierKey(Class<?> source, Class<?> target, boolean useConverter, boolean nonNull) {
		this.source = source;
		this.target = target;
		this.useConverter = useConverter;
		this.nonNull = nonNull;
	}

	public Class<?> getSource() {
		return source;
	}

	public Class<?> getTarget() {
		return target;
	}

	public boolean isUseConverter() {
		return useConverter;
	}

	public boolean isNonNull() {
		return nonNull;
	}
}
