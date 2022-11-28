package ucthings.codec.transport.queue.mem;

import cn.hutool.core.thread.NamedThreadFactory;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucthings.codec.common.InternalQueueManager;
import ucthings.codec.common.util.StringUtil;
import ucthings.codec.transport.QueueMessageHandler;
import ucthings.codec.transport.queue.QueueSubscriber;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认内存订阅
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/9/2 16:54
 */
public class DefaultMemorySubscriber implements MemorySubscriber {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(MemorySubscriber.class);

	/**
	 * name
	 */
	private static final String NAME = "Memory订阅器";

	/**
	 * Memory listen pool
	 */
	private static final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
			new NamedThreadFactory("mem-listen-", false));

	/**
	 * Memory work pool
	 */
	private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2 - 1,
			Runtime.getRuntime().availableProcessors() * 2 - 1, 60L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(), new NamedThreadFactory("memorySub-", false));

	/**
	 * handler
	 */
	private static final Map<String, MemoryTask> tasks = new ConcurrentHashMap<>();

	/**
	 * 开始订阅
	 *
	 * @param topic   主题
	 * @param handler 处理器
	 */
	@Override
	public boolean subscribe(String topic, QueueMessageHandler handler, int workers) {
		if (StringUtil.hasText(topic) && handler != null) {
			//移除历史
			unsubscribe(topic);
			//启动任务
			MemoryTask task = new MemoryTask(topic, handler, InternalQueueManager.get(topic));
			tasks.put(topic, task);
            scheduledExecutor.schedule(task, 100, TimeUnit.MILLISECONDS);
			return true;
		}
		return false;
	}

	/**
	 * 开始订阅
	 *
	 * @param topic 主题
	 */
	@Override
	public void subscribe(String topic) {
        throw new RuntimeException("不支持的操作,请使用[subscribe(String,QueueMessageHandler,int)]");
//		MemoryTask task = tasks.get(topic);
//		if (task != null) {
//			scheduledExecutor.schedule(task, 1000, TimeUnit.MILLISECONDS);
//		}
    }

	/**
	 * 停止订阅
	 *
	 * @param topic 主题
	 */
	@Override
	public void unsubscribe(String topic) {
        MemoryTask task = tasks.remove(topic);
		if (task != null) {
            task.destroy();
        }
	}

	/**
	 * 停止订阅并移除相关handler
	 *
	 * @param topic 主题
	 */
	@Override
	public QueueMessageHandler remove(String topic) {
        MemoryTask task = tasks.remove(topic);
		if (task != null) {
			task.destroy();
			QueueMessageHandler handler = task.getHandler();
			task.setHandler(null);
			if (handler != null) {
				handler.destroy();
			}
			return handler;
		}
		return null;
	}

	/**
	 * 获取订阅主题handler
	 *
	 * @param topic 主题
	 */
	@Override
	public QueueMessageHandler handler(String topic) {
		MemoryTask task = tasks.get(topic);
		if (task != null) {
			return task.getHandler();
		}
		return null;
	}

	/**
	 * 订阅主题
	 *
	 * @return 订阅主题列表
	 */
	@Override
	public List<String> subscribeTopics() {
		return new ArrayList<>(tasks.keySet());
	}

	/**
	 * 关闭
	 */
	@Override
	public void close() {
		Set<String> keySet = tasks.keySet();
		for (String key : keySet) {
			MemoryTask task = tasks.remove(key);
			task.destroy();
			task.setHandler(null);
		}
	}


	@Getter
	public static class MemoryTask implements Runnable {

		private final AtomicBoolean state = new AtomicBoolean(false);

		/**
		 * 主题
		 */
		private final String topic;

		/**
		 * handler
		 */
		@Setter
		private QueueMessageHandler handler;

		@Setter
		private Queue<Object> queue;


		public MemoryTask(String topic, QueueMessageHandler handler, Queue<Object> queue) {
            this.topic = topic;
            this.handler = handler;
            this.queue = queue;
            this.state.set(true);
        }


		@Override
		public void run() {
			if (state.get() && handler.runState()) {
				while (!queue.isEmpty()) {
					Object value = queue.poll();
					if (value != null) {
						QueueSubscriber.processMessage(value, executor, handler, state, topic, logger, NAME);
					}
				}
				scheduledExecutor.schedule(this, 10, TimeUnit.MILLISECONDS);
			}
		}

		public void destroy() {
            state.set(false);
            if (this.handler != null) {
                this.handler.destroy();
            }
        }
	}
}
