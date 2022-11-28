package ucthings.codec.common.executor;

import cn.hutool.core.thread.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * System  Executor
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2021/9/30 23:44
 */
public class SystemExecutorService {

	/**
	 * 日志
	 */
	private final Logger logger = LoggerFactory.getLogger("system");

	/**
	 * time thread pool
	 */
	private final ScheduledThreadPoolExecutor scheduledExecutor;

	/**
	 * thread pool
	 */
	private final ThreadPoolExecutor executor;
	/**
	 * thread pool
	 */
	private final ThreadPoolExecutor executor2;

	/**
	 * CompletableFuture 回调
	 */
	private final ThreadPoolExecutor cfExecutor;

	/**
	 * CompletableFuture IO 耗时 线程池
	 */
	private final ThreadPoolExecutor cfIOExecutor;


	private SystemExecutorService() {
		executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
				Runtime.getRuntime().availableProcessors() * 3, 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(), new NamedThreadFactory("sys-exec", false));
		executor2 = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
				Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(), new NamedThreadFactory("sys-exec2", false));
		scheduledExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
				new NamedThreadFactory("sys-schedule-", false));
		cfExecutor = new ThreadPoolExecutor(4,
				4, 60L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(Integer.MAX_VALUE), new NamedThreadFactory("cf-", false));
		cfIOExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() / 2,
				Runtime.getRuntime().availableProcessors() / 2, 60L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(Integer.MAX_VALUE), new NamedThreadFactory("cfIO-", false));
	}


	/**
	 * add timed task
	 *
	 * @param task         task
	 * @param initialDelay delay time
	 * @param period       period time
	 * @param unit         time unit
	 */
	public static void scheduleAtFixedRate(Runnable task,
										   long initialDelay,
										   long period,
										   TimeUnit unit) {
		getInstance().scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
	}

	/**
	 * get ScheduledThreadPoolExecutor
	 */
	public static ScheduledThreadPoolExecutor getSchedule() {
		return getInstance().scheduledExecutor;
	}

	/**
	 * add timed task
	 * default time unit SECONDS. The class {@link TimeUnit}
	 * invoke {@link  SystemExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
	 *
	 * @param task         task
	 * @param initialDelay delay time
	 * @param period       period time
	 */
	public static void scheduleAtFixedRate(Runnable task,
										   long initialDelay,
										   long period) {
		scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
	}


	/**
	 * execute task
	 *
	 * @param task Runnable task
	 */
	public static void execute(Runnable task) {
		getInstance().executor.execute(task);
	}

	/**
	 * execute task
	 *
	 * @param task Runnable task
	 */
	public static void execute2(Runnable task) {
		getInstance().executor2.execute(task);
	}

	/**
	 * delay execute task
	 *
	 * @param task  Runnable task
	 * @param delay delay time
	 * @param unit  time unit
	 */
	public static void schedule(Runnable task, int delay, TimeUnit unit) {
		getInstance().scheduledExecutor.schedule(task, delay, unit);
	}

	/**
	 * submit task
	 *
	 * @param task Runnable task
	 */
	public static void submit(Runnable task) {
		getInstance().executor.submit(task);
	}

	/**
	 * submit task
	 *
	 * @param task Runnable task
	 */
	public static void remove(Runnable task) {
		getInstance().executor.remove(task);
	}

	/**
	 * submit task
	 *
	 * @param task Runnable task
	 */
	public static void removeSchedule(Runnable task) {
		getInstance().scheduledExecutor.remove(task);
	}


	/**
	 * get instance
	 *
	 * @return {@link SystemExecutorService}
	 */
	private static SystemExecutorService getInstance() {
		return SystemExecutorServiceHolder.TCP_EXECUTOR_SERVICE;
	}

	/**
	 * 获取cf线程池
	 * CPU密集
	 *
	 * @return ThreadPoolExecutor
	 */
	public static ThreadPoolExecutor cfExecutor() {
		return getInstance().cfExecutor;
	}

	/**
	 * 获取cf IO 线程池
	 * IO密集
	 *
	 * @return ThreadPoolExecutor
	 */
	public static ThreadPoolExecutor cfIOExecutor() {
		return getInstance().cfIOExecutor;
	}

	/**
	 * 获取cf IO 线程池
	 * IO密集
	 *
	 * @return ThreadPoolExecutor
	 */
	public static ThreadPoolExecutor executor() {
		return getInstance().executor;
	}

	/**
	 * log thread pool info
	 */
	private void printInfo() {
		logger.info("time thread pool SystemExecutorService: active：{}，core：{}，task：{}",
				scheduledExecutor.getActiveCount(),
				scheduledExecutor.getCorePoolSize(),
				scheduledExecutor.getQueue().size()
		);
		logger.info("thread pool2 SystemExecutorService: active：{}，core：{}，task：{}",
				executor.getActiveCount(),
				executor.getCorePoolSize(),
				executor.getQueue().size()
		);
		logger.info("thread pool2 SystemExecutorService: active：{}，core：{}，task：{}",
				executor2.getActiveCount(),
				executor2.getCorePoolSize(),
				executor2.getQueue().size()
		);
		logger.info("thread cfExecutor SystemExecutorService: active：{}，core：{}，task：{}",
				cfExecutor.getActiveCount(),
				cfExecutor.getCorePoolSize(),
				cfExecutor.getQueue().size()
		);
		logger.info("thread cfIOExecutor SystemExecutorService: active：{}，core：{}，task：{}",
				cfIOExecutor.getActiveCount(),
				cfIOExecutor.getCorePoolSize(),
				cfIOExecutor.getQueue().size()
		);
	}

	/**
	 * instace inner class
	 * 去除双重锁机制，采用加载类阶段来保证线程安全问题，
	 * ----
	 * 双重锁机制之所以不能正常运行是因为，在new对象的时候，是有三个步骤的：分配内存空间，
	 * 初始化对象，然后将内存地址赋值给变量；在这么三个步骤中，极有可能会在操作上进行重排序，在重排序的情况下，还没有初始化
	 * 对象，先将内存地址赋值给了变量（这种情况是可能存在的），当线程B进入时，发现变量不为null，就会直接返回这个实例，然而此时
	 * 可能拿到的是还没有初始化完成的对象。所以双重锁机制是不提倡使用的。
	 * 在新的内存模型下，实例字段使用volatile可以解决双重锁检查的问题，因为在新的内存模型下，volatile禁止了一些重排序，但是，
	 * 同时，使用volatile的性能开销也有所上升。所以又提出一种新的模式——Initialization on Demand Holder. 这种方法使用内
	 * 部类来做到延迟加载对象，在初始化这个内部类的时候。JLS(Java Language Sepcification)会保证这个类的线程安全
	 * ---
	 * 解释：http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
	 */
	private static class SystemExecutorServiceHolder {
		private static final SystemExecutorService TCP_EXECUTOR_SERVICE = new SystemExecutorService();
	}

}
