package ucthings.codec.log;

import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ucthings.codec.common.InternalQueueManager;
import ucthings.codec.common.util.StringUtil;
import ucthings.codec.message.RequestMessage;
import ucthings.codec.transport.TransportApi;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * kafka 日志服务
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/28 15:03
 */
public class KafkaLogService implements LogService, InitializingBean, DisposableBean {

	/**
	 * 默认日志
	 */
	private final Logger logger = LoggerFactory.getLogger("kafka-logger");

	private final TransportApi transportApi;

	/**
	 * 日志
	 */
	private final Queue<Object> queue;

	/**
	 * 线程池
	 */
	private final ThreadPoolExecutor executor;
	private final boolean outFile;
	private final AtomicBoolean state = new AtomicBoolean(false);
	private LogService logService;

	public KafkaLogService(Integer threadNum, Boolean outFile, TransportApi transportApi) {
		this.transportApi = transportApi;
		this.outFile = outFile;
		if (this.outFile) {
			try {
				Log4j2LogService log4j2LogService = new Log4j2LogService(16, 1);
				log4j2LogService.afterPropertiesSet();
				this.logService = log4j2LogService;
			} catch (Exception e) {
				logger.error("初始化kafka日志-文件输出日志异常 ", e);
			}
		}
		queue = InternalQueueManager.get("sys-kafka-log");
		executor = new ThreadPoolExecutor(threadNum, threadNum, 60L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(), new NamedThreadFactory("log-kafka-", false));
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
			if (outFile && logService != null) {
				logService.save(log);
			}
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
			save(log);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.state.set(true);
		new Thread(this::run, "kafka-log").start();
		logger.info("kafka super log started");
	}


	private void run() {
		while (state.get()) {
			while (!queue.isEmpty()) {
				UcLog log = (UcLog) queue.poll();
				if (log != null) {
					String group = log.group();
					if (StringUtil.hasText(group)) {
						executor.execute(() -> {
							transportApi.sendMessage(makeKafkaLog(group, log));
						});
					}
				}
			}
			ThreadUtil.safeSleep(100);
		}
	}

	private RequestMessage makeKafkaLog(String group, UcLog log) {
		return new RequestMessage(IdUtil.nanoId(32), "log", group, log.toMap());
	}

	@Override
	public void destroy() throws Exception {
		this.state.set(false);
		this.logService = null;
		executor.shutdownNow();
	}
}
