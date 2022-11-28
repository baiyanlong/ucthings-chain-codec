package ucthings.codec.log.appender;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import ucthings.codec.log.LogService;
import ucthings.codec.log.SysLog;
import ucthings.codec.log.UcKvLog;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * uc sys log
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/30 11:24
 */
@Plugin(name = "UcSys", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class UcSysLogAppender extends AbstractAppender {

	private final static Set<String> noLogClass = new HashSet<>();

	static {
		noLogClass.add("org.apache.kafka.common.utils.LogContext$LocationAwareKafkaLogger");
	}

	private LogService logService;

	private UcSysLogAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
							 final boolean ignoreExceptions, LogService logService, final Property[] properties) {
		super(name, filter, layout, ignoreExceptions, properties);
		this.logService = logService;
	}

	/**
	 * Creates a builder for a UcSysLogAppender.
	 *
	 * @return a builder for a UcSysLogAppender.
	 */
	@PluginBuilderFactory
	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder();
	}

	@Override
	public void append(final LogEvent event) {
		if (logService == null) {
			try {
				logService = SpringUtil.getBean(LogService.class);
			} catch (Exception ignored) {
				AbstractLifeCycle.LOGGER.error("UcSysLogAppender 还未获取到 logService，忽略该日志");
			}
		}
		if (logService == null) {
			AbstractLifeCycle.LOGGER.error("UcSysLogAppender 还未获取到 logService，忽略该日志");
			return;
		}
		try {
			if (noLogClass.contains(event.getLoggerFqcn())) {
				return;
			}
			tryAppend(event);
		} catch (final Exception e) {
			error("Unable to write to UcSysLog in appender [" + getName() + "]", event, e);
		}
	}

	@Override
	public void start() {
		super.start();
	}

	@Override
	public boolean stop(final long timeout, final TimeUnit timeUnit) {
		setStopping();
		boolean stopped = super.stop(timeout, timeUnit, false);
		setStopped();
		return stopped;
	}

	@Override
	public String toString() {
		return "UcSysLogAppender{" + "name=" + getName() + ", state=" + getState() + '}';
	}

	private void tryAppend(final LogEvent event) {
		final Layout<? extends Serializable> layout = getLayout();
		byte[] data;
		if (layout instanceof SerializedLayout) {
			final byte[] header = layout.getHeader();
			final byte[] body = layout.toByteArray(event);
			data = new byte[header.length + body.length];
			System.arraycopy(header, 0, data, 0, header.length);
			System.arraycopy(body, 0, data, header.length, body.length);
		} else {
			data = layout.toByteArray(event);
		}
		String msg = new String(data).replace("\r\n", "");
		UcKvLog kvLog = null;
		switch (event.getLevel().name().toLowerCase()) {
			case "fatal":
				kvLog = SysLog.fatal(msg);
				break;
			case "error":
				kvLog = SysLog.error(msg);
				break;
			case "warn":
				kvLog = SysLog.warn(msg);
				break;
			case "debug":
				kvLog = SysLog.debug(msg);
				break;
			case "trace":
				kvLog = SysLog.trac(msg);
				break;
			case "all":
				kvLog = SysLog.all(msg);
				break;
			case "info":
			default:
				kvLog = SysLog.info(msg);
				break;
		}
		kvLog.label("nodeId", "device-test");
		kvLog.label("logImpl", "log4j2");
		kvLog.label("logName", event.getLoggerName());
		kvLog.put("threadName", event.getThreadName());
		kvLog.put("timeMillis", event.getTimeMillis() + "");
		kvLog.put("loggerFqcn", event.getLoggerFqcn());
		if (event.getMarker() != null) {
			kvLog.put("marker", event.getMarker().getName());
		}
		String message = event.getMessage().getFormattedMessage();
		kvLog.put("formatMessage", message);
		if ((message.startsWith("{") || message.startsWith("[")) && (message.endsWith("}") || message.endsWith("]"))) {
			try {
				JSONValidator jsonValidator = JSONValidator.from(message);
				if (jsonValidator.validate()) {
					JSONObject parseObject = JSONObject.parseObject(message);
					UcKvLog finalKvLog = kvLog;
					parseObject.forEach((k, v) -> {
						finalKvLog.put("message_" + k, String.valueOf(v));
					});
				}
			} catch (Exception ignored) {
			}
		}
		Object[] parameters = event.getMessage().getParameters();
		if (parameters != null) {
			kvLog.put("messageParamNum", parameters.length + "");
			int i = 0;
			for (Object parameter : parameters) {
				if (parameter != null) {
					i = i + 1;
					kvLog.put("param_" + i, parameter + "_");
				}
			}
		}
		if (event.getMessage().getThrowable() != null) {
			kvLog.put("messageThrowable", ExceptionUtil.getMessage(event.getMessage().getThrowable()));
		}
		if (event.getMessage().getThrowable() != null) {
			kvLog.put("throwable", ExceptionUtil.getMessage(event.getMessage().getThrowable()));
		}
		kvLog.put("nanoTime", event.getNanoTime() + "");
		if (event.getSource() != null) {
			kvLog.put("method", event.getSource().getMethodName());
			kvLog.put("className", event.getSource().getClassName());
			kvLog.put("classFile", event.getSource().getFileName());
			kvLog.put("lineNumber", event.getSource().getLineNumber() + "");
			kvLog.put("moduleName", event.getSource().getModuleName());
			kvLog.put("moduleVersion", event.getSource().getModuleVersion());
			kvLog.put("classLoader", event.getSource().getClassLoaderName());
		}
		kvLog.put("threadId", event.getThreadId() + "");
		kvLog.put("threadPriority", event.getThreadPriority() + "");
		if (event.getThrown() != null) {
			kvLog.put("thrown", ExceptionUtil.getMessage(event.getThrown()));
		}
		kvLog.put("timeMillis", event.getTimeMillis() + "");
		logService.save(kvLog);
	}

	/**
	 * Builds UcSysLogAppender instances.
	 *
	 * @param <B> The type to build
	 */
	public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
			implements org.apache.logging.log4j.core.util.Builder<UcSysLogAppender> {

		@PluginAttribute(value = "syncSend", defaultBoolean = true)
		private boolean syncSend;

		@Override
		public UcSysLogAppender build() {
			final Layout<? extends Serializable> layout = getLayout();
			if (layout == null) {
				AbstractLifeCycle.LOGGER.error("No layout provided for UcSysLogAppender");
				return null;
			}
			LogService logService = null;
			try {
				logService = SpringUtil.getBean(LogService.class);
			} catch (Exception e) {
				AbstractLifeCycle.LOGGER.error("UcSysLogAppender 还未获取到 logService");
			}
			return new UcSysLogAppender(getName(), layout, getFilter(), isIgnoreExceptions(), logService,
					getPropertyArray());
		}

		public boolean isSyncSend() {
			return syncSend;
		}

		public B setSyncSend(final boolean syncSend) {
			this.syncSend = syncSend;
			return asBuilder();
		}
	}
}
