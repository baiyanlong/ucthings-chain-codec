package ucthings.codec.log.help;

import cn.hutool.core.util.IdUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.WaitForActiveShards;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucthings.codec.common.util.DateTimeUtil;
import ucthings.codec.common.util.StringUtil;
import ucthings.codec.log.UcLog;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ElasticsearchTool
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/3/15 18:07
 */
public enum ElasticsearchTool {

	/**
	 * instance
	 */
	instance;

	/**
	 * 定义一个枚举的元素,它代表此类的一个实例
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<Thread, ElasticsearchClient> clients = new ConcurrentHashMap<>(16);
	/**
	 * client
	 */
	private ElasticsearchClient client;
	/**
	 * 前缀
	 */
	private String prefix;
	private String hosts;
	private String username;
	private String password;
	private String scheme;
	private String sslFilePath;

	public void init(String hosts, String username, String password, String scheme, String sslFilePath) {
		this.hosts = hosts;
		this.username = username;
		this.password = password;
		this.scheme = scheme;
		this.sslFilePath = sslFilePath;
		this.client = createEsClient();
	}

	/**
	 * 销毁
	 */
	public void destroy() {
		if (client != null) {
			client.shutdown();
		}
		this.clients.forEach((t, e) -> {
			e.shutdown();
		});
		this.clients.clear();
	}

	public boolean createIndex(String index) {
		try {
			BooleanResponse exists = client.indices().exists(c -> c.index(processIndex(index)));
			if (!exists.value()) {
				return Boolean.TRUE.equals(client.indices().create(r -> r.index(processIndex(index))).acknowledged());
			} else {
				return true;
			}
		} catch (IOException e) {
			logger.error("create index error ", e);
		}
		return false;
	}

	public boolean existsIndex(String index) {
		try {
			BooleanResponse exists = client.indices().exists(c -> c.index(processIndex(index)));
			return exists.value();
		} catch (IOException e) {
			logger.error("query index error ", e);
		}
		return false;
	}


	public boolean deleteIndex(String index) {
		try {
			DeleteIndexResponse response = client.indices().delete(r -> r.index(processIndex(index)));
			return response.acknowledged();
		} catch (IOException e) {
			logger.error("delete index error ", e);
		}
		return false;
	}

	/**
	 * 批量保存
	 *
	 * @param logs 日志
	 * @return 是否成功
	 */
	public boolean saveBatch(List<UcLog> logs) {
		try {
			ElasticsearchClient elasticsearchClient = clients.get(Thread.currentThread());
			if (elasticsearchClient == null) {
				elasticsearchClient = createEsClient();
				clients.put(Thread.currentThread(), elasticsearchClient);
			}
			List<BulkOperation> operations = new ArrayList<>(logs.size());
			logs.forEach(log -> {
				String index = processIndex(log.group());
				if (index != null) {
					Map<String, Object> map = insertBefore(log);
					if (!map.isEmpty()) {
						operations.add(BulkOperation.of(builder -> builder.<Map<String, Object>>index(
								indexBuilder -> indexBuilder
										.id(IdUtil.objectId())
										.index(index).document(map))));
					}
				}
			});
			if (!operations.isEmpty()) {
				BulkResponse response = elasticsearchClient.bulk(builder -> builder.refresh(Refresh.False)
						.source(pb -> pb.fetch(false))
						.operations(operations).waitForActiveShards(WaitForActiveShards.of(builder2
								-> builder2.count(0))));
				boolean errors = response.errors();
				if (errors) {
					logger.error("elasticsearch log save error {}", response);
				}
				return errors;
			}
			return false;
		} catch (Exception e) {
			logger.error("save batch error ", e);
		}
		return false;
	}

	private ElasticsearchClient createEsClient() {

		HttpHost[] httpHosts = parseHost(hosts, scheme);
		SSLContext sslContext = null;
		try {
			if (StringUtil.hasText(sslFilePath)) {
				Path caCertificatePath = Paths.get(sslFilePath);
				CertificateFactory factory =
						CertificateFactory.getInstance("X.509");
				Certificate trustedCa;
				try (InputStream is = Files.newInputStream(caCertificatePath)) {
					trustedCa = factory.generateCertificate(is);
				}
				KeyStore trustStore = KeyStore.getInstance("pkcs12");
				trustStore.load(null, null);
				trustStore.setCertificateEntry("ca", trustedCa);
				SSLContextBuilder sslContextBuilder = SSLContexts.custom()
						.loadTrustMaterial(trustStore, null);
				sslContext = sslContextBuilder.build();
			}
		} catch (Exception e) {
			logger.error("ElasticsearchTool init error", e);
		}
		CredentialsProvider auth = new BasicCredentialsProvider();
		auth.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
		SSLContext finalSslContext = sslContext;
		return new ElasticsearchClient(new RestClientTransport(
				RestClient.builder(httpHosts).setHttpClientConfigCallback(c -> {
							c.setDefaultCredentialsProvider(auth);
							if (finalSslContext != null) {
								c.setSSLContext(finalSslContext);
							}
							return c;
						})
						.build(), new JacksonJsonpMapper()));
	}

	/**
	 * 处理索引
	 *
	 * @param index 主题
	 * @return 索引
	 */
	private String processIndex(String index) {
		if (index != null && !"".equals(index) && index.length() > 0) {
			if (prefix != null && !"".equals(prefix)) {
				return prefix + "-" + index + "-" + LocalDate.now().getMonthValue();
			} else {
				return index + "-" + LocalDate.now().getMonthValue();
			}
		} else {
			return null;
		}
	}

	/**
	 * 插入之前
	 *
	 * @param log log
	 * @return data
	 */
	private Map<String, Object> insertBefore(UcLog log) {
		Map<String, Object> map = log.toMap();
		map.putIfAbsent("logTime", DateTimeUtil.getCurrentDateTimeMsStr());
		Set<String> keySet = map.keySet();
		for (String key : keySet) {
			Object o = map.get(key);
			if (o instanceof String) {
				String str = o.toString().replace("\"", "'");
				map.put(key, str);
				continue;
			}
			if (o instanceof Map) {
				Map<String, Object> map2 = (Map<String, Object>) o;
				Set<String> keys = map2.keySet();
				for (String s : keys) {
					Object o1 = map2.get(s);
					if (o1 instanceof String) {
						String str = o1.toString().replace("\"", "'");
						map2.put(s, str);
					}
					if (o1 instanceof Map) {
						Map<String, Object> map3 = (Map<String, Object>) o1;
						Set<String> keys2 = map3.keySet();
						for (String s2 : keys2) {
							Object o2 = map3.get(s2);
							if (o2 instanceof String) {
								String str = o2.toString().replace("\"", "'");
								map3.put(s2, str);
							}
						}
					}
				}
			}
		}
		return map;
	}


	/**
	 * 解析 es host
	 *
	 * @param hosts  hosts
	 * @param scheme 表格
	 * @return 主机地址
	 */
	private HttpHost[] parseHost(String hosts, String scheme) {
		String[] hostArray = hosts.split(",");
		List<HttpHost> httpHosts = new ArrayList<>();
		for (String host : hostArray) {
			String[] split = host.split(":");
			if (split.length == 2) {
				httpHosts.add(new HttpHost(split[0], Integer.parseInt(split[1]), scheme));
			} else {
				httpHosts.add(new HttpHost(split[0], -1, scheme));
			}
		}
		HttpHost[] hs = new HttpHost[httpHosts.size()];
		for (int i = 0; i < httpHosts.size(); i++) {
			hs[i] = httpHosts.get(i);
		}
		return hs;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

}