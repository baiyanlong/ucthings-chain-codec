package ucthings.codec.log;

import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.thread.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ucthings.codec.common.InternalQueueManager;
import ucthings.codec.log.help.ElasticsearchTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * es 日志服务
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/28 15:03
 */
public class ElasticSearchLogService implements LogService, InitializingBean, DisposableBean {

	private static final Integer ucLogSaveMaxBatchSize = 2048;
	/**
	 * 默认日志
	 */
	private final Logger logger = LoggerFactory.getLogger("es-logger");
	/**
	 * 日志队列
	 */
	private final Queue<Object> queue;
	/**
	 * 线程池
	 */
	private final ThreadPoolExecutor executor;
	private final boolean outFile;

	private final Map<String, Object> config;
	private final AtomicBoolean state = new AtomicBoolean(false);
	private LogService logService;
	private ElasticsearchTool tool;


	public ElasticSearchLogService(Integer threadNum, Boolean outFile, Map<String, Object> config) {
		this.config = config;
		this.outFile = outFile;
		if (this.outFile) {
			Log4j2LogService log4j2LogService = new Log4j2LogService(16, 1);
			try {
				log4j2LogService.afterPropertiesSet();
				this.logService = log4j2LogService;
			} catch (Exception e) {
				logger.error("初始化es日志-文件输出日志异常 ", e);
			}
		}
		queue = InternalQueueManager.get("sys-log-es");
		executor = new ThreadPoolExecutor(threadNum, threadNum, 60L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(), new NamedThreadFactory("es-log-", false));
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
			String group = log.group();
			if (group != null && !"".equals(group)) {
				queue.offer(log);
				if (outFile && logService != null) {
					logService.save(log);
				}
			}
		}
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		init();
		this.state.set(true);
		logger.info("elasticSearch uc log started");
		new Thread(this::run, "es-log").start();
	}

	/**
	 * 任务
	 */
	private void run() {
		List<UcLog> logs = new ArrayList<>(ucLogSaveMaxBatchSize);
		while (state.get()) {
			while (!queue.isEmpty()) {
				UcLog log = (UcLog) queue.poll();
				if (tool != null) {
					if (log != null && logs.size() < ucLogSaveMaxBatchSize) {
						logs.add(log);
					} else {
						List<UcLog> saveLogs = logs;
						logs = new ArrayList<>(ucLogSaveMaxBatchSize);
						executor.execute(() -> {
							tool.saveBatch(saveLogs);
							saveLogs.clear();
						});
					}
				}
			}

			if (!logs.isEmpty()) {
				List<UcLog> saveLogs = logs;
				logs = new ArrayList<>(logs.size());
				if (tool != null) {
					executor.submit(() -> {
						tool.saveBatch(saveLogs);
						saveLogs.clear();
					});
				}
			} else {
				//此处为没有日志数据时。防止空循
				ThreadUtil.safeSleep(100);
			}
		}
	}


	/**
	 * init
	 */
	public void destroy() {
		this.state.set(false);
		if (tool != null) {
			tool.destroy();
			tool = null;
		}
		this.logService = null;
		executor.shutdownNow();
	}


	/**
	 * init
	 */
	public void init() {
		ElasticsearchTool.instance.destroy();
		//初始化es客户端
		String cert = config.get("cert").toString();
		String hosts = config.get("hosts").toString();
		String username = config.get("username").toString();
		String password = config.get("password").toString();
		String scheme = config.get("scheme").toString();
		Object prefix = config.get("prefix");
		if (prefix != null) {
			prefix = prefix.toString();
			if (!"".equals(prefix)) {
				ElasticsearchTool.instance.setPrefix((String) prefix);
			}
		}
		ElasticsearchTool.instance.init(hosts, username, password, scheme, cert);
		tool = ElasticsearchTool.instance;
	}
}
