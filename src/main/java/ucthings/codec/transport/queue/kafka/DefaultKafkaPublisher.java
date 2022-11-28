package ucthings.codec.transport.queue.kafka;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucthings.codec.common.util.JsonUtil;
import ucthings.codec.common.util.StringUtil;
import ucthings.codec.log.LogService;
import ucthings.codec.log.QueueMessageLog;
import ucthings.codec.message.TransportMessage;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 默认 kafka 发布器
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/1/6 21:41
 */
public class DefaultKafkaPublisher implements KafkaPublisher {

	/**
	 * 日志
	 */
	private final Logger logger;

	private final Map<String, Object> config;

	/**
	 * kafka Producer
	 */
	private final List<Producer<String, String>> kafkaProducers;
	/**
	 * 日志服务
	 */
	private final LogService logService;

	private final int clientNum = Runtime.getRuntime().availableProcessors() / 2 == 0 ? 1 : Runtime.getRuntime().availableProcessors();

	public DefaultKafkaPublisher(Map<String, Object> config, LogService logService) {
		this.logService = logService;
		this.logger = LoggerFactory.getLogger(getClass());
		Map<String, Object> cfg = new HashMap<>();
		config.forEach((k, v) -> {
			cfg.put(k.replace("-", "."), v);
		});
		this.config = KafkaConfigTool.mergeMap(KafkaConfigTool.producers_default_config, cfg);
		this.kafkaProducers = makeClient();
	}

	/**
	 * 发布消息
	 *
	 * @param topic 主题
	 * @param str   消息
	 */
	@Override
	public void send(String topic, String str) {
		String msgId = String.valueOf(IdUtil.nanoId(32));
		ProducerRecord<String, String> kafkaMessage = createKafkaMessage(msgId, topic, str, null);
		kafkaProducers.get(ThreadLocalRandom.current().nextInt(clientNum) % clientNum).send(kafkaMessage, (meta, e) -> {
			QueueMessageLog messageLog = new QueueMessageLog("transport-message", "request", "info", 10, "DefaultKafkaPublisher发送消息");
			messageLog.label("className", this.getClass().getName());
			messageLog.put("data", str);
			messageLog.put("msgId", msgId);
			messageLog.put("topic", topic);
			messageLog.put("transportType", "kafka");
			//meta
			messageLog.put("offset", meta.offset());
			messageLog.put("timestamp", meta.timestamp());
			messageLog.put("serializedKeySize", meta.serializedKeySize());
			messageLog.put("serializedValueSize", meta.serializedValueSize());
			messageLog.put("topicPartition", meta.partition());
			try {
				JSONObject jsonObject = JSONObject.parseObject(str);
				jsonObject.forEach(messageLog::put);
			} catch (Exception e2) {
				logger.error("kafka 发布器发送记录日志异常 ", e);
			}
			if (e == null) {
				messageLog.label("status", "success");
			} else {
				//发送失败
				messageLog.label("status", "fail");
			}
			logService.save(messageLog);
		});
		if (logger.isInfoEnabled()) {
			logger.info("默认Kafka发布器发布主题[{}]数据:{}", topic, str);
		}
	}

	/**
	 * 构造消息
	 *
	 * @param messageId 消息ID
	 * @param topic     主题
	 * @param data      数据
	 * @param headers
	 * @return
	 */
	public ProducerRecord<String, String> createKafkaMessage(String messageId, String topic, Serializable data, Map<String, Object> headers) {
		String value = data instanceof String ? (String) data : JsonUtil.toJSONString(data);
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, messageId, value);
		if (headers != null && !headers.isEmpty()) {
			headers.forEach((key, val) -> {
				if (StringUtil.isBlank(key) && val != null) {
					producerRecord.headers().add(new RecordHeader(key, val.toString().getBytes(StandardCharsets.UTF_8)));
				}
			});
		}
		return producerRecord;
	}

	/**
	 * 发布消息
	 *
	 * @param topic   主题
	 * @param message 消息
	 */
	@Override
	public void send(String topic, TransportMessage<Map<String, Object>> message) {
		send(topic, JsonUtil.toJSONString(message));
	}


	public List<Producer<String, String>> makeClient() {
		List<Producer<String, String>> clients = new ArrayList<>(clientNum);
		int end = clientNum + 1;
		for (int i = 1; i < end; i++) {
			HashMap<String, Object> map = new HashMap<>(this.config);
			map.put("client.id", "kafkaPub-" + i);
			clients.add(new KafkaProducer<>(map));
		}
		if (logger.isInfoEnabled()) {
			logger.info("make kafka pub " + clients.size());
		}
		return clients;
	}

}
