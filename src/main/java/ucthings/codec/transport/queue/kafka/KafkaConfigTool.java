package ucthings.codec.transport.queue.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * kafka 配置工具
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/1/6 21:57
 */
public class KafkaConfigTool {

	/**
	 * 生产者config配置
	 */
	public static final Map<String, Object> producers_default_config;

	/**
	 * 消费者config配置
	 */
	public static final Map<String, Object> consumers_default_config;

	static {
		/*----------------------------------producers--------------------------------------*/
		producers_default_config = new HashMap<>();
		//kv默认序列化
		producers_default_config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producers_default_config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        /*
         acks：指定了必须要有多少个分区副本收到消息，生产者才会认为写入消息是成功的，这个参数对消息丢失的可能性有重大影响。
         acks=0：生产者在写入消息之前不会等待任何来自服务器的响应，容易丢消息，但是吞吐量高。
         acks=1：只要集群的首领节点收到消息，生产者会收到来自服务器的成功响应。如果消息无法到达首领节点（比如首领节点崩溃，新首领没有选举出来），生产者会收到一个错误响应，为了避免数据丢失，生产者会重发消息。不过，如果一个没有收到消息的节点成为新首领，消息还是会丢失。默认使用这个配置。
         acks=all：只有当所有参与BUFFER_MEMORY_CONFIG复制的节点都收到消息，生产者才会收到一个来自服务器的成功响应。延迟高。
         */
		producers_default_config.put(ProducerConfig.ACKS_CONFIG, "1");

		/*
		 * buffer.memory:设置生产者内存缓冲区的大小，生产者用它缓冲要发送到服务器的消息。
		 */
		producers_default_config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

		/*
		 * 默认情况下，消息发送时不会被压缩。该参数可以设置为 snappy、gzip 或lz4，它指定了消息被发送给 broker 之前使用哪一种压缩算法进行压缩。
		 *
		 * snappy 压缩算法由 Google 发明，占用较少的 CPU，却能提供较好的性能和相当可观的压缩比，如果比较关注性能和网络带宽，可以使用这种算法。
		 * gzip 压缩算法一般会占用较多的 CPU，但会提供更高的压缩比，所以如果网络带宽比较有限，可以使用这种算法。
		 */
		producers_default_config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

		/*
		 * 重试次数
		 * 当前机器cpu核数右移1位
		 */
		producers_default_config.put(ProducerConfig.RETRIES_CONFIG, Runtime.getRuntime().availableProcessors() << 1);
		/*
		 *重试时间间隔
		 * 3s 秒
		 */
		producers_default_config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 3000);

		/*
		 * 2 MB
		 */
		producers_default_config.put(ProducerConfig.BATCH_SIZE_CONFIG, 2097140);

		/*
		 * 每次发送完毕，间隔时间
		 */
		producers_default_config.put(ProducerConfig.LINGER_MS_CONFIG, 10);

		/*
		 * 生产者在收到服务器响应之前可以发送多少个消息。它的值越高，就会占用越多的内存，不过也会提升吞吐量。
		 * 把它设为 1 可以保证消息是按照发送的顺序写入服务器的，即使发生了重试。
		 */
		producers_default_config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

		/*
		 * 控制生产者发送的请求大小。它可以指能发送的单个消息的最大值，也可以指单个请求里所有消息总的大小。
		 * 例如，假设这个值为 1MB，那么可以发送的单个最大消息为 1MB，或者生产者可以在单个请求里发送一个批次，
		 * 该批次包含了 1000 个消息，每个消息大小为 1KB。
		 * 另外，broker 对可接收的消息最大值也有自己的限制（message.max.bytes），
		 * 所以两边的配置最好可以匹配，避免生产者发送的消息被 broker 拒绝。
		 *
		 */
		producers_default_config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1048576);

		/*
		 * 发送数据时等待服务器返回响应的时间
		 */
		producers_default_config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);

		/*
		 * TCP socket 接收和发送数据包的缓冲区大小。如果它们被设为 -1，就使用操作系统的默认值
		 * 。如果生产者或消费者与 broker 处于不同的数据中心，那么可以适当增大这些值，
		 * 因为跨数据中心的网络一般都有比较高的延迟和比较低的带宽。
		 *
		 * 3 mb
		 */
		producers_default_config.put(ProducerConfig.RECEIVE_BUFFER_CONFIG, 3145710);
		producers_default_config.put(ProducerConfig.SEND_BUFFER_CONFIG, 3145710);


		/*-------------------------------------consumers-----------------------------------*/
		consumers_default_config = new HashMap<>();
		consumers_default_config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumers_default_config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		/*
		 *设置在broker configuration中的group.min.session.timeout.ms 与 group.max.session.timeout.ms之间。
		 * 其默认值是：10000 （10 s）
		 */
		consumers_default_config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);

		/*
		 * 心跳间隔。心跳是在consumer与coordinator之间进行的。心跳是确定consumer存活，加入或者退出group的有效手段。
		 * 这个值必须设置的小于session.timeout.ms，因为：当Consumer由于某种原因不能发Heartbeat到coordinator时，
		 * 并且时间超过session.timeout.ms时，就会认为该consumer已退出，它所订阅的partition会分配到同一group 内的其它的consumer上。
		 * 通常设置的值要低于session.timeout.ms的1/3。
		 * 默认值是：3000 （3s）
		 */
		consumers_default_config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 6000);

		/*
		 * 自动提交确认消息
		 * 默认值是true。
		 */
		consumers_default_config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

		/*
		 *
		 * 自动提交间隔。范围：[0,Integer.MAX]，默认值是 5000 （5 s）
		 */
		consumers_default_config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 2000);

		/*
		 * Kafka Broker在发现kafka在没有初始offset，或者当前的offset是一个不存在的值（如果一个record被删除，就肯定不存在了）时，该如何处理。它有4种处理方式：
		 * 1） earliest：自动重置到最早的offset。
		 * 2） latest：看上去重置到最晚的offset。
		 * 3） none：如果边更早的offset也没有的话，就抛出异常给consumer，告诉consumer在整个consumer group中都没有发现有这样的offset。
		 * 4） 如果不是上述3种，只抛出异常给consumer。
		 * 默认值 latest
		 */
		consumers_default_config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

		/*
		 * 连接空闲超时时间。因为consumer只与broker有连接（coordinator也是一个broker），
		 * 所以这个配置的是consumer到broker之间的。
		 * 默认值 540000  9分钟
		 */
		consumers_default_config.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);

		/*
		 * Fetch请求发给broker后，在broker中可能会被阻塞的（当topic中records的总size小于fetch.min.bytes时），此时这个fetch请求耗时
		 * 就会比较长。这个配置就是来配置consumer最多等待response多久。
		 */
		consumers_default_config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 10000);

		/*
		 * 一次fetch请求，从一个broker中取得的records最大大小。如果在从topic中第一个非空的partition取消息时，
		 * 如果取到的第一个record的大小就超过这个配置时，仍然会读取这个record，也就是说在这片情况下，只会返回这一条record。
		 * broker、topic都会对producer发给它的message size做限制。所以在配置这值时，
		 * 可以参考broker的message.max.bytes 和 topic的max.message.bytes的配置。
		 * 取值范围是：[0, Integer.Max]，默认值是：52428800 （5 MB）
		 */
		consumers_default_config.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800);

		/*
		 * 向一个broker发起fetch请求时，broker返回的records的大小最小值。如果broker中数据量不够的话会wait，
		 * 直到数据大小满足这个条件。取值范围是：[0, Integer.Max]，默认值是1。
		 * 默认值设置为1的目的是：使得consumer的请求能够尽快的返回。
		 */
		consumers_default_config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);

		/*
		 * 一次fetch请求，从一个partition中取得的records最大大小。如果在从topic中第一个非空的partition取消息时，
		 * 如果取到的第一个record的大小就超过这个配置时，仍然会读取这个record，也就是说在这片情况下，只会返回这一条record。
		 * broker、topic都会对producer发给它的message size做限制。所以在配置这值时，可以参考broker
		 * 的message.max.bytes 和 topic的max.message.bytes的配置。
		 * 默认值 1 MB
		 */
		consumers_default_config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048570);

		/**
		 * 如果长时间没有调用poll，且间隔超过这个值时，就会认为这个consumer失败了。
		 * 9分钟
		 */
		consumers_default_config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 540000);

		/**
		 * 每次拉去最大数据量
		 * 1024条
		 */
		consumers_default_config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1024);

		/**
		 * Consumer receiver buffer （SO_RCVBUF）的大小。这个值在创建Socket连接时会用到。
		 * 取值范围是：[-1, Integer.MAX]。默认值是：65536 （64 KB）
		 * 如果值设置为-1，则会使用操作系统默认的值。
		 */
		consumers_default_config.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, 1048570);
		consumers_default_config.put(ConsumerConfig.SEND_BUFFER_CONFIG, 1048570);

		/**
		 * 请求发起后，并不一定会很快接收到响应信息。这个配置就是来配置请求超时时间的。默认值是：305000 （305 s）
		 */
		consumers_default_config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 305000);

		/**
		 * Metadata数据的刷新间隔。即便没有任何的partition订阅关系变更也行执行。
		 * 范围是：[0, Integer.MAX]，默认值是：300000 （5 min）
		 */
		consumers_default_config.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 60000);
	}

	/**
	 * 合并多个map，位置靠后的map的key会覆盖前面map中的key
	 * {@link Map<String,Object>}
	 *
	 * @param maps maps
	 */
	public static <K, V> Map<K, V> mergeMap(Map<K, V>... maps) {
		if (maps != null && maps.length > 0) {
			int size = 0;
			for (Map<K, V> map : maps) {
				size += map.size();
			}
			Map<K, V> data = new HashMap<>(size);
			for (Map<K, V> map : maps) {
				map.forEach((key, value) -> {
					if (key != null && !"".equals(key) && value != null && !"".equals(value)) {
						data.put(key, value);
					}
				});
			}
			return data;
		} else {
			throw new IllegalArgumentException("合并Map不能为空");
		}
	}


	public static Properties mapToProperties(Map<String, ?> config) {
		Properties properties = new Properties();
		properties.putAll(config);
		return properties;
	}


}
