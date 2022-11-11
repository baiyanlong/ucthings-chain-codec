import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * @author byl
 * @since 2022/11/11 15:39
 */
public class V8Case {

	public static  V8Case newInstance() {
		return  new V8Case();
	}

	public static void main(String[] args) {

		V8Case.newInstance().jsToJavaTwo();

	}

	public void javaToJs(){
		V8 runtime = V8.createV8Runtime(); // 创建 js 运行时
		int result = runtime.executeIntegerScript("" // 执行一段 js 代码
				+ "var hello = 'hello, ';\n"
				+ "var world = 'world!';\n"
				+ "hello.concat(world).length;\n");
		System.out.println(result);
		runtime.release(true); // 为 true 则会检查并抛出内存泄露错误（如果存在的话）便于及时发现
	}

	public void javaToJsFunc(){
		V8 runtime = V8.createV8Runtime(); // 创建 js 运行时
		runtime.executeScript("" +
				"function add(a, b){\n" +
				"   return a + b\n" +
				"}");
		var arg = new V8Array(runtime).push(12).push(21); // 创建参数数组 arg为 V8Array[通过构造创建V8Array 根据前面规则最后需要手动释放]
		var r = runtime.executeIntegerFunction("add", arg); // 调用函数
		System.out.println(r);
		arg.release(); //别忘记释放对象
		runtime.release(true);
	}

	public void jsToJava(){
		V8 runtime = V8.createV8Runtime(); // 创建 js 运行时

		V8Object v8Object = Console.newInstance().registerConsoleLog(runtime);

		runtime.executeScript("console.log('测试java 方法xxxxx')");

		runtime.executeScript("console.err('测试java 方法xxxxx')");

		v8Object.release();
		runtime.release(true);
	}

	public void jsToJavaTwo(){
		V8 runtime = V8.createV8Runtime(); // 创建 js 运行时
		PersonPrinter personPrinter = new PersonPrinter();
		runtime.registerJavaMethod(personPrinter, "print");
		runtime.executeScript("print('hello, world');");
		runtime.release(true);

	}





}
