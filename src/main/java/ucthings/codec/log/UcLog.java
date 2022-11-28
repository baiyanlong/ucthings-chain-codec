package ucthings.codec.log;

import java.util.Map;

/**
 * uc log
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/28 14:15
 */
public interface UcLog {

	/**
	 * 日志类型
	 *
	 * @return 日志类型
	 */
	default String type() {
		return "default";
	}

	/**
	 * json log
	 *
	 * @return json log
	 */
	String jsonLog();


	/**
	 * map
	 *
	 * @return json log
	 */
	Map<String, Object> toMap();

	/**
	 * 日志级别
	 *
	 * @return 日志级别
	 */
	default String level() {
		return "info";
	}


	/**
	 * 标签
	 */
	void label(String name, String value);

	/**
	 * log group
	 *
	 * @return 日志组
	 */
	String group();

}
