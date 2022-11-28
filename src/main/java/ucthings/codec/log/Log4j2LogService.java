package ucthings.codec.log;

import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.thread.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import ucthings.codec.common.InternalQueueManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/28 15:32
 */
public class Log4j2LogService implements LogService, InitializingBean {

	/**
	 * 默认日志
	 */
	private final Logger defLog = LoggerFactory.getLogger("file-logger");

	/**
	 * 日志 map
	 */
	private final Map<String, Logger> loggerMap;

	/**
	 * 日志
	 */
	private final Queue<Object> queue;

	/**
	 * 线程池
	 */
	private final ThreadPoolExecutor executor;

	public Log4j2LogService(int loggerMapCapacity, int threadNum) {
		loggerMap = new ConcurrentHashMap<>(loggerMapCapacity);
		queue = InternalQueueManager.get("file-log");
		executor = new ThreadPoolExecutor(threadNum, threadNum, 60L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(), new NamedThreadFactory("log-file-", false));
	}

	/**
	 * 发送
	 *
	 * @param log 日志
	 */
	@Override
	public void save(UcLog log) {
		String group = log.group();
		if (group != null && !"".equals(group)) {
			queue.offer(log);
		}
	}

	/**
	 * 批量保存
	 *
	 * @param logs 日志集
	 */
	@Override
	public void save(List<UcLog> logs) {
		for (UcLog log : logs) {
			String group = log.group();
			if (group != null && !"".equals(group)) {
				queue.offer(log);
			}
		}
	}


	private void write(Logger logger, String level, String text) {
		switch (level.toLowerCase(Locale.ROOT)) {
			case "trace":
				if (logger.isTraceEnabled()) {
					logger.trace(text);
				}
				break;
			case "debug":
				if (logger.isDebugEnabled()) {
					logger.debug(text);
				}
				break;
			case "info":
				if (logger.isInfoEnabled()) {
					logger.info(text);
				}
				break;
			case "warn":
				if (logger.isWarnEnabled()) {
					logger.warn(text);
				}
				break;
			case "error":
				if (logger.isErrorEnabled()) {
					logger.error(text);
				}
				break;
			default:
				if (defLog.isInfoEnabled()) {
					defLog.info("默认 {} ", text);
				}
				break;
		}
	}

	private void run() {
		while (true) {
			while (!queue.isEmpty()) {
				UcLog log = (UcLog) queue.poll();
				if (log != null) {
					String group = log.group();
					Logger logger = loggerMap.get(group);
					if (logger == null) {
						logger = LoggerFactory.getLogger(group);
						loggerMap.put(group, logger);
					}
					Logger finalLogger = logger;
					executor.execute(() -> {
						write(finalLogger, log.level(), log.jsonLog());
					});
				}
			}
			ThreadUtil.safeSleep(100);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		new Thread(this::run, "file-log").start();
	}
}
