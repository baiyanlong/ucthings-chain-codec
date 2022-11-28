package ucthings.codec.transport.queue.mem;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucthings.codec.common.InternalQueueManager;
import ucthings.codec.common.util.JsonUtil;
import ucthings.codec.log.LogService;
import ucthings.codec.log.QueueMessageLog;
import ucthings.codec.message.TransportMessage;

import java.util.Map;

/**
 * 默认内存发布器
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/8/29 11:51
 */
public class DefaultMemoryPublisher implements MemoryPublisher {

	/**
	 * 日志
	 */
	private final Logger logger;

	private final LogService logService;

	public DefaultMemoryPublisher(LogService logService) {
		this.logger = LoggerFactory.getLogger(getClass());
		this.logService = logService;
	}

	/**
	 * 发布消息
	 *
	 * @param topic   主题
	 * @param message 消息
	 */
	@Override
	public void send(String topic, TransportMessage<Map<String, Object>> message) {
		send(topic, (Object) message);
	}

	/**
	 * 发布消息
	 *
	 * @param name    队列
	 * @param message 消息
	 */
	@Override
	public void send(String name, Object message) {
		InternalQueueManager.get(name).offer(message);
		if (logger.isInfoEnabled()) {
			logger.info("默认Memory发布器,发布主题[{}]数据:{}", name, JsonUtil.toJSONString(message));
		}
		QueueMessageLog messageLog = new QueueMessageLog("transport-message", "request", "info", 10, "DefaultKafkaPublisher发送消息");
		messageLog.label("className", this.getClass().getName());
		messageLog.put("data", message);
		messageLog.put("topic", name);
		messageLog.put("transportType", "memory");
		logService.save(messageLog);
	}
}
