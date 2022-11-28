package ucthings.codec.common;

import cn.hutool.core.thread.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 内部队列管理器
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2021/12/23 22:23
 */
public class InternalQueueManager {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(InternalQueueManager.class);

	/**
	 * 数据队列池
	 */
	private static final Map<String, Queue<Object>> queuePool = new ConcurrentHashMap<>();

	private static final AtomicBoolean run = new AtomicBoolean(false);

	private static final Map<String, Lock> locks = new ConcurrentHashMap<>(32);

	static {
		run.set(true);
		new Thread(InternalQueueManager::checkQueue, "sys-queue-check").start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			run.set(false);
			if (logger.isInfoEnabled()) {
				logger.info("退出队列检测");
			}
		}, "queue-check"));
	}


	public InternalQueueManager() {
	}

	/**
	 * 获取队列
	 *
	 * @param queueKey 队列key --》 为了适配kafka topic 故而这样实现
	 * @return 队列
	 */
	public static Queue<Object> get(String queueKey) {
		Lock lock = null;
		Queue<Object> queue = queuePool.get(queueKey);
		try {
			if (queue == null) {
				lock = locks.computeIfAbsent(queueKey, value -> new ReentrantLock());
				lock.lock();
				queue = queuePool.get(queueKey);
				if (queue == null) {
					queue = new ConcurrentLinkedQueue<>();
					queuePool.put(queueKey, queue);
				}
			}
			return queue;
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}


	/**
	 * 获取队列
	 *
	 * @param queueKey 队列key --》 为了适配kafka topic 故而这样实现
	 * @return 队列
	 */
	public static Queue<Object> remove(String queueKey) {
		Lock lock = null;
		Queue<Object> queue = null;
		try {
			queue = queuePool.get(queueKey);
			if (queue == null) {
				return null;
			}
			lock = locks.computeIfAbsent(queueKey, value -> new ReentrantLock());
			lock.lock();
			queue = queuePool.remove(queueKey);
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
		return queue;
	}

	private static void checkQueue() {
		if (logger.isInfoEnabled()) {
			logger.info("开启队列检测");
		}
		while (run.get()) {
			queuePool.forEach((id, queue) -> {
				if (queue != null && queue.size() > 1024 * 100 * 5) {
					logger.error("系统检测告警:队列[{}]大小[{}]自动清除", id, queue.size());
					queue.clear();
				}
			});
			ThreadUtil.safeSleep(1000);
		}
	}

	/**
	 * 获取队列
	 *
	 * @param queueKey 队列key --》 为了适配kafka topic 故而这样实现
	 * @return 队列
	 */
	public Queue<Object> getQueue(String queueKey) {
		return get(queueKey);
	}

	/**
	 * 获取队列任务数
	 *
	 * @return queue size
	 */
	public Map<String, Integer> getQueueSize() {
		Set<String> keySet = queuePool.keySet();
		Map<String, Integer> qs = new HashMap<>(keySet.size());
		keySet.forEach(key -> {
			qs.put(key, queuePool.get(key).size());
		});
		return qs;
	}
}
