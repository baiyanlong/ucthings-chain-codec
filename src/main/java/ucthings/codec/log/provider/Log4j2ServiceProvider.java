package ucthings.codec.log.provider;

import org.apache.logging.slf4j.Log4jLoggerFactory;
import org.apache.logging.slf4j.Log4jMDCAdapter;
import org.apache.logging.slf4j.Log4jMarkerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.Util;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * 适配 slf4j 2.x
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/8/30 19:19
 */
public class Log4j2ServiceProvider implements SLF4JServiceProvider {

	public static String REQUESTED_API_VERSION = "2.0";

	private ILoggerFactory loggerFactory;
	private IMarkerFactory markerFactory;
	private MDCAdapter mdcAdapter;

	public Log4j2ServiceProvider() {
		try {
		} catch (NoSuchFieldError error) {
			Util.report("This version of SLF4J requires log4j version 1.2.12 or later. See also http://www.slf4j.org/codes.html#log4j_version");
		}
	}

	@Override
	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	@Override
	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	@Override
	public MDCAdapter getMDCAdapter() {
		return mdcAdapter;
	}

	@Override
	public String getRequestedApiVersion() {
		return REQUESTED_API_VERSION;
	}

	@Override
	public void initialize() {
		loggerFactory = new Log4jLoggerFactory(new Log4jMarkerFactory());
		markerFactory = new BasicMarkerFactory();
		mdcAdapter = new Log4jMDCAdapter();
	}

}