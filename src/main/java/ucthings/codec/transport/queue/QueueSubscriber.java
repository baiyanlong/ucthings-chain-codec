package ucthings.codec.transport.queue;

import org.slf4j.Logger;
import ucthings.codec.transport.QueueMessageHandler;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 队列订阅器
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/4/3 17:19
 */
public interface QueueSubscriber {

	static void processMessage(Object value, ThreadPoolExecutor executor, QueueMessageHandler handler,
							   AtomicBoolean state, String topic, Logger logger, String name) {
		executor.execute(() -> {
			QueueMessageHandler messageHandler = handler;
			Object msg = value;
			while (messageHandler != null && state.get() && handler.runState()) {
				try {
					msg = messageHandler.onMessage(topic, msg);
					messageHandler = messageHandler.next();
				} catch (Exception e) {
					logger.error("[{}]-{}主题[{}]处理异常 {} ", name, messageHandler.id(), topic, value, e);
					messageHandler.onExceptionCaught(e, messageHandler, value);
				}
			}
		});
	}

	/**
	 * 开始订阅
	 */
	boolean subscribe(String topic, QueueMessageHandler handler, int workers);

	/**
	 * 开始订阅
	 */
	void subscribe(String topic);

	/**
	 * 停止订阅
	 */
	void unsubscribe(String topic);

	/**
	 * 停止订阅并移除相关handler
	 *
	 * @param topic 主题
	 */
	QueueMessageHandler remove(String topic);

	/**
	 * 获取订阅主题handler
	 *
	 * @param topic 主题
	 */
	QueueMessageHandler handler(String topic);

	/**
	 * 订阅主题
	 *
	 * @return 订阅主题列表
	 */
	List<String> subscribeTopics();

	/**
	 * 关闭
	 */
	void close();
}
