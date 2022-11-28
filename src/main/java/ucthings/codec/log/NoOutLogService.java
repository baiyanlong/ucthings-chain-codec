package ucthings.codec.log;

import java.util.List;

/**
 * 无日志输出
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/28 17:52
 */
public class NoOutLogService implements LogService {

	/**
	 * 保存日志
	 *
	 * @param log 日志消息
	 */
	@Override
	public void save(UcLog log) {

	}

	/**
	 * 批量保存
	 *
	 * @param logs 日志集
	 */
	@Override
	public void save(List<UcLog> logs) {

	}
}
