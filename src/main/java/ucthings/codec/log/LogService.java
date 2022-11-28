package ucthings.codec.log;

import java.util.List;

/**
 * 日志服务
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/28 14:19
 */
public interface LogService {

	/**
	 * 保存日志
	 *
	 * @param log 日志消息
	 */
	void save(UcLog log);

	/**
	 * 批量保存
	 *
	 * @param logs 日志集
	 */
	void save(List<UcLog> logs);

}
