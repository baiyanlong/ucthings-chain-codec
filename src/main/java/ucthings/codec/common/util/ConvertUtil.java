package ucthings.codec.common.util;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.lang.Nullable;

/**
 * @author xigexb
 * @version 1.0.0
 * @since 2021/5/18 12:18
 */
public class ConvertUtil {

	/**
	 * Convenience operation for converting a source object to the specified targetType.
	 * {@link TypeDescriptor#forObject(Object)}.
	 *
	 * @param source     the source object
	 * @param targetType the target type
	 * @param <T>        泛型标记
	 * @return the converted value
	 * @throws IllegalArgumentException if targetType is {@code null},
	 *                                  or sourceType is {@code null} but source is not {@code null}
	 */
	@Nullable
	public static <T> T convert(@Nullable Object source, Class<T> targetType) {
		if (source == null) {
			return null;
		}
		if (ClassUtil.isAssignableValue(targetType, source)) {
			return (T) source;
		}
		GenericConversionService conversionService = ConversionService.getInstance();
		return conversionService.convert(source, targetType);
	}

	/**
	 * Convenience operation for converting a source object to the specified targetType,
	 * where the target type is a descriptor that provides additional conversion context.
	 * {@link TypeDescriptor#forObject(Object)}.
	 *
	 * @param source     the source object
	 * @param sourceType the source type
	 * @param targetType the target type
	 * @param <T>        泛型标记
	 * @return the converted value
	 * @throws IllegalArgumentException if targetType is {@code null},
	 *                                  or sourceType is {@code null} but source is not {@code null}
	 */
	@Nullable
	public static <T> T convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		GenericConversionService conversionService = ConversionService.getInstance();
		return (T) conversionService.convert(source, sourceType, targetType);
	}

	/**
	 * Convenience operation for converting a source object to the specified targetType,
	 * where the target type is a descriptor that provides additional conversion context.
	 * Simply delegates to {@link #convert(Object, TypeDescriptor, TypeDescriptor)} and
	 * encapsulates the construction of the source type descriptor using
	 * {@link TypeDescriptor#forObject(Object)}.
	 *
	 * @param source     the source object
	 * @param targetType the target type
	 * @param <T>        泛型标记
	 * @return the converted value
	 * @throws IllegalArgumentException if targetType is {@code null},
	 *                                  or sourceType is {@code null} but source is not {@code null}
	 */
	@Nullable
	public static <T> T convert(@Nullable Object source, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		GenericConversionService conversionService = ConversionService.getInstance();
		return (T) conversionService.convert(source, targetType);
	}

}
