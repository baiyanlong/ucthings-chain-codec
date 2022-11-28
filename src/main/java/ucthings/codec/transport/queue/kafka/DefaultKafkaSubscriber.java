package ucthings.codec.transport.queue.kafka;

import cn.hutool.core.thread.NamedThreadFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucthings.codec.common.InternalQueueManager;
import ucthings.codec.common.util.StringUtil;
import ucthings.codec.message.MessagePool;
import ucthings.codec.message.RequestMessage;
import ucthings.codec.message.ResponseMessage;
import ucthings.codec.message.TransportMessage;
import ucthings.codec.transport.QueueMessageHandler;
import ucthings.codec.transport.queue.QueueSubscriber;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * 默认kafka订阅器实现
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/9/2 14:11
 */
public class DefaultKafkaSubscriber implements KafkaSubscriber {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(KafkaSubscriber.class);

	private static final String NAME = "kafka订阅器";
	/**
	 * 缓存队列
	 */
	private static final Map<String, Queue<Object>> queues = new ConcurrentHashMap<>();
	/**
	 * handler
	 */
	private static final Map<String, KafkaTask> tasks = new ConcurrentHashMap<>();
	/**
	 * kafka listen pool
	 */
	private static final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new NamedThreadFactory("kafka-listen-", false));	/**
	 * 响应消息handler
	 */
	private final QueueMessageHandler responseHandler = new QueueMessageHandler() {

		private final AtomicBoolean state = new AtomicBoolean(false);

		/**
		 * handler id
		 *
		 * @return id
		 */
		@Override
		public String id() {
			return NAME + "-responseHandler";
		}

		@Override
		public void init(String topic) {
			if (logger.isInfoEnabled()) {
				logger.info("[{}]初始化主题[{}]处理器", NAME, topic);
			}
			state.set(true);
		}

		@Override
		public Object onMessage(String topic, Object message) {
			if (state.get()) {
				if (message instanceof ResponseMessage) {
					pool.response((ResponseMessage) message);
				}
			} else {
				logger.warn("[{}],尚未初始化", NAME);
			}
			return null;
		}

		@Override
		public void onExceptionCaught(Throwable throwable, QueueMessageHandler handler, Object message) {
			if (state.get()) {
				logger.error("[{}]发生异常,忽略响应消息 {}", NAME, message.toString(), throwable);
			} else {
				logger.warn("[{}]尚未初始化", NAME);
			}
		}

		@Override
		public QueueMessageHandler next() {
			return null;
		}

		@Override
		public void destroy() {
			if (responseHandler != this) {
				state.set(false);
			}
		}

		/**
		 * 运行状态
		 */
		@Override
		public boolean runState() {
			return state.get();
		}
	};
	/**
	 * kafka work pool
	 */
	private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2 - 1, Runtime.getRuntime().availableProcessors() * 2 - 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("kafkaSub-", false));	/**
	 * 本地队列转发
	 */
	private final QueueMessageHandler localForwardHandler = new QueueMessageHandler() {

		private final AtomicBoolean state = new AtomicBoolean(false);

		/**
		 * handler id
		 *
		 * @return id
		 */
		@Override
		public String id() {
			return NAME + "-localForwardHandler";
		}

		@Override
		public void init(String topic) {
			if (logger.isInfoEnabled()) {
				logger.info("[{}]初始化主题[{}]处理器", NAME, topic);
			}
			state.set(true);
			QueueMessageHandler handler = next();
			while (handler != null) {
				handler.init(topic);
				handler = handler.next();
			}
		}

		@Override
		public Object onMessage(String topic, Object message) {
			if (state.get()) {
				queues.computeIfAbsent(topic, InternalQueueManager::get).offer(message);
				if (logger.isInfoEnabled()) {
					logger.info("[{}]-主题[{}]转发数据 {}", NAME, topic, JSON.toJSONString(message));
				}
			} else {
				logger.warn("[{}],尚未初始化", NAME);
			}
			return message;
		}

		@Override
		public void onExceptionCaught(Throwable throwable, QueueMessageHandler handler, Object message) {
			if (state.get()) {
				logger.error("[{}]发生异常,忽略转发消息 {}", NAME, message.toString(), throwable);
			} else {
				logger.warn("[{}]尚未初始化", NAME);
			}
		}

		@Override
		public QueueMessageHandler next() {
			return responseHandler;
		}

		@Override
		public void destroy() {
			if (localForwardHandler != this) {
				state.set(false);
			}
		}

		/**
		 * 运行状态
		 */
		@Override
		public boolean runState() {
			return state.get();
		}
	};
	private final Map<String, Object> config;
	private final MessagePool<TransportMessage<Map<String, Object>>, ResponseMessage> pool;


	public DefaultKafkaSubscriber(Map<String, Object> config, MessagePool<TransportMessage<Map<String, Object>>, ResponseMessage> memoryMessagePool) {
		this.pool = memoryMessagePool;
		localForwardHandler.init("default");
		Map<String, Object> cfg = new HashMap<>();
		config.forEach((k, v) -> {
			cfg.put(k.replace("-", "."), v);
		});
		this.config = KafkaConfigTool.mergeMap(KafkaConfigTool.consumers_default_config, cfg);
	}

	/**
	 * 开始订阅
	 *
	 * @param topic   主题
	 * @param handler 处理器
	 * @param workers 工作者数量
	 */
	@Override
	public boolean subscribe(String topic, QueueMessageHandler handler, int workers) {
		if (StringUtil.hasText(topic) && handler != null) {
			//移除历史
			unsubscribe(topic);
			//初始化
			if (handler != localForwardHandler) {
				QueueMessageHandler h1 = handler;
				while (h1 != null) {
					h1.init(topic);
					h1 = h1.next();
				}
			}
			//启动任务
			if (workers == 0) {
				workers = 1;
			}
			int end = workers + 1;
			List<Consumer<String, String>> consumers = new ArrayList<>(workers);
			for (int i = 1; i < end; i++) {
				Map<String, Object> map = new HashMap<>(this.config);
				map.put("group.instance.id", topic.replace("/", "_") + "-" + i);
				map.put("group.id", "-" + topic);
				Consumer<String, String> kafkaConsumer = new KafkaConsumer<>(map);
				consumers.add(kafkaConsumer);
			}
			KafkaTask task = new KafkaTask(topic, handler, consumers);
			tasks.put(topic, task);
			scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS);
			if (logger.isInfoEnabled()) {
				logger.info("[{}]订阅主题[{}]工作者数量[{}]消息处理器[{}]", NAME, topic, workers, handler.id());
			}
			return true;
		}
		return false;
	}

	/**
	 * 开始订阅
	 *
	 * @param topic topic
	 */
	@Override
	public void subscribe(String topic) {
		this.subscribe(topic, localForwardHandler, 1);
	}

	/**
	 * 停止订阅
	 *
	 * @param topic topic
	 */
	@Override
	public void unsubscribe(String topic) {
		if (topic != null) {
			KafkaTask task = tasks.remove(topic);
			if (task != null) {
				task.destroy();
			}
			queues.remove(topic);
		}
	}

	/**
	 * 停止订阅并移除相关handler
	 *
	 * @param topic 主题
	 */
	@Override
	public QueueMessageHandler remove(String topic) {
		KafkaTask kafkaTaskOld = tasks.remove(topic);
		if (kafkaTaskOld != null) {
			scheduledExecutor.remove(kafkaTaskOld);
			kafkaTaskOld.destroy();
			return kafkaTaskOld.getHandler();
		}
		queues.remove(topic);
		return null;
	}

	/**
	 * 获取订阅主题handler
	 *
	 * @param topic 主题
	 */
	@Override
	public QueueMessageHandler handler(String topic) {
		KafkaTask task = tasks.get(topic);
		if (task != null && task.getHandler() != localForwardHandler) {
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
		localForwardHandler.destroy();
		Set<String> topics = tasks.keySet();
		for (String topic : topics) {
			KafkaTask task = tasks.remove(topic);
			if (task != null) {
				task.destroy();
			}
		}
		Set<String> queueNames = queues.keySet();
		for (String queueName : queueNames) {
			Queue<Object> queue = queues.remove(queueName);
			if (queue != null) {
				queue.clear();
			}
		}
		config.clear();
	}

	@Getter
	public static class KafkaTask implements Runnable {

		private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
		//获取写锁
		private final Lock wlock = rwLock.writeLock();
		//获取读锁
		private final Lock rLock = rwLock.readLock();
		private AtomicBoolean state = new AtomicBoolean(false);
		/**
		 * 主题
		 */
		private String topic;

		/**
		 * handler
		 */
		@Setter
		private QueueMessageHandler handler;

		/**
		 * kafka 消费者
		 */
		private List<Consumer<String, String>> consumers;

		public KafkaTask(String topic, QueueMessageHandler handler, List<Consumer<String, String>> consumers) {
			if (topic == null) {
				throw new NullPointerException("topic is null");
			}
			this.topic = topic;
			this.handler = handler;
			this.consumers = consumers;
			//初始化 kafka consumer
			this.consumers.forEach(consumer -> {
				consumer.subscribe(Pattern.compile(topic));
			});
			this.state.set(true);
		}


		@Override
		public void run() {
			if (state.get() && handler != null && handler.runState()) {
				consumers.forEach(consumer -> {
					if (!state.get()) {
						return;
					}
					try {
						rLock.lock();
						ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
						for (ConsumerRecord<String, String> record : records) {
							String value = record.value();
							try {
								Object v = value;
								if ((value.startsWith("{") || value.startsWith("[")) && (value.endsWith("}") || value.endsWith("]")) && JSONValidator.from(value).validate()) {
									JSONObject jsonObject = JSON.parseObject(value);
									String id = jsonObject.getString("id");
									String code = jsonObject.getString("code");
									Boolean state = jsonObject.getBoolean("state");
									String topic = jsonObject.getString("topic");
									JSONObject data = jsonObject.getJSONObject("data");
									if (jsonObject.getString("code").startsWith("request")) {
										RequestMessage message = new RequestMessage(id, code, topic, data);
										message.getState().set(state);
										v = message;
									}
									if (jsonObject.getString("code").startsWith("response")) {
										ResponseMessage message = new ResponseMessage(id, code, topic, data);
										message.getState().set(state);
										v = message;
									}
								}
								//判断是否为传输层数据对象
								QueueSubscriber.processMessage(v, executor, handler, state, topic, logger, NAME);
							} catch (Exception e) {
								logger.error("kafka订阅器[{}]主题[{}]异常", NAME, topic, e);
							}
						}
					} catch (Exception e) {
						logger.error("[{}]-[{}]任务运行异常", NAME, topic, e);
					} finally {
						rLock.unlock();
					}

				});
				scheduledExecutor.schedule(this, 10, TimeUnit.MILLISECONDS);
			}
		}

		public void destroy() {
			try {
				wlock.lock();
				if (state != null) {
					state.set(false);
				}
				if (consumers != null) {
					consumers.forEach(Consumer::unsubscribe);
					logger.info("topic[{}]停止订阅", topic);
					consumers.forEach(Consumer::close);
					consumers.clear();
					consumers = null;
				}
				queues.remove(topic);
				this.topic = null;
				this.state = null;
				if (handler != null) {
					handler.destroy();
					setHandler(null);
				}
			} catch (Exception e) {
				logger.error("[{}]-[{}]任务关闭异常", NAME, topic, e);
			} finally {
				wlock.unlock();
			}
		}
	}




}
