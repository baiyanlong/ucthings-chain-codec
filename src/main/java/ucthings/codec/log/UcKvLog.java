package ucthings.codec.log;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.Setter;
import ucthings.codec.common.util.DateTimeUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * uc kv log
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/28 14:28
 */
public class UcKvLog implements UcLog {

	/**
	 * 类型
	 */
	@Getter
	private final String type;
	/**
	 * 级别
	 */
	@Getter
	private final String level;
	@Getter
	private final String group;
	private final Map<String, Object> kv;
	private final Map<String, String> label;
	private final String timestamp;
	@Setter
	@Getter
	private String msg;

	public UcKvLog(String group, String type, String level, int kvSize, String msg) {
		this.group = group;
		this.type = type;
		this.level = level;
		this.msg = msg;
		this.kv = new HashMap<>(kvSize);
		this.label = new HashMap<>(kvSize);
		this.timestamp = DateTimeUtil.formatESDateTime(LocalDateTime.now()) + "+08";
	}

	/**
	 * 日志类型
	 *
	 * @return 日志类型
	 */
	@Override
	public String type() {
		return type;
	}

	/**
	 * 日志级别
	 *
	 * @return 日志级别
	 */
	@Override
	public String level() {
		return level;
	}

	/**
	 * 标签
	 *
	 * @param name  标签
	 * @param value 值
	 */
	@Override
	public void label(String name, String value) {
		label.put(name, value);
	}

	/**
	 * log group
	 *
	 * @return 日志组
	 */
	@Override
	public String group() {
		return group;
	}

	public void put(String key, Object value) {
		kv.put(key, value);
	}

	/**
	 * json log
	 *
	 * @return json log
	 */
	@Override
	public String jsonLog() {
		return JSONObject.toJSONString(toMap());
	}

	/**
	 * map
	 *
	 * @return json log
	 */
	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> data = new HashMap<>();
		data.put("type", type);
		data.put("level", level);
		data.put("kv", kv);
		data.put("label", label);
		data.put("timestamp", timestamp);
		data.put("msg", msg);
		return data;
	}
}
