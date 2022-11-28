package ucthings.codec.common.util;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

/**
 * @author xigexb
 * @version 1.0.0
 * @since 2021/5/18 12:19
 */
public class ConversionService extends ApplicationConversionService {

	@Nullable
	private static volatile ConversionService SHARED_INSTANCE;

	public ConversionService() {
		this(null);
	}

	public ConversionService(@Nullable StringValueResolver embeddedValueResolver) {
		super(embeddedValueResolver);
		super.addConverter(new EnumToStringConverter());
		super.addConverter(new StringToEnumConverter());
	}

	/**
	 * Return a shared default application {@code ConversionService} instance, lazily
	 * building it once needed.
	 * <p>
	 * Note: This method actually returns an {@link ConversionService}
	 * instance. However, the {@code ConversionService} signature has been preserved for
	 * binary compatibility.
	 *
	 * @return the shared {@code ConversionService} instance (never{@code null})
	 */
	public static GenericConversionService getInstance() {
		ConversionService sharedInstance = ConversionService.SHARED_INSTANCE;
		if (sharedInstance == null) {
			synchronized (ConversionService.class) {
				sharedInstance = ConversionService.SHARED_INSTANCE;
				if (sharedInstance == null) {
					sharedInstance = new ConversionService();
					ConversionService.SHARED_INSTANCE = sharedInstance;
				}
			}
		}
		return sharedInstance;
	}

}
