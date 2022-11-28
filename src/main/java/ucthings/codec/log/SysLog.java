package ucthings.codec.log;

/**
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/28 18:28
 */
public class SysLog extends UcKvLog {

	private static final String GROUP = "system";
	private static final String TYPE = "console";
	private static final String LOG_DEBUG = "debug";
	private static final String LOG_INFO = "info";
	private static final String LOG_WARN = "warn";
	private static final String LOG_ERROR = "error";
	private static final String LOG_FATAL = "fatal";
	private static final String LOG_ALL = "all";
	private static final String LOG_TRACE = "trace";

	private SysLog(String group, String type, String level, int kvSize, String msg) {
		super(group, type, level, kvSize, msg);
	}

	public static SysLog debug(String msg) {
		return new SysLog(GROUP, TYPE, LOG_DEBUG, 16, msg);
	}

	public static SysLog info(String msg) {
		return new SysLog(GROUP, TYPE, LOG_INFO, 16, msg);
	}

	public static SysLog warn(String msg) {
		return new SysLog(GROUP, TYPE, LOG_WARN, 16, msg);
	}

	public static SysLog error(String msg) {
		return new SysLog(GROUP, TYPE, LOG_ERROR, 16, msg);
	}

	public static SysLog fatal(String msg) {
		return new SysLog(GROUP, TYPE, LOG_FATAL, 16, msg);
	}

	public static SysLog all(String msg) {
		return new SysLog(GROUP, TYPE, LOG_ALL, 16, msg);
	}

	public static SysLog trac(String msg) {
		return new SysLog(GROUP, TYPE, LOG_TRACE, 16, msg);
	}

}
