package site.mingsha.javaagent.methodtime.collector;

/**
 * 单条方法耗时采集数据。
 * Single method time record for profiling.
 * 记录方法调用的类名、方法名、起止时间、耗时、线程名及额外信息。
 * Records class name, method name, start/end time, duration, thread name, and extra info for a method call.
 *
 * @author mingsha
 */
public class MethodTimeRecord {
    public final String className;    // 类名 | class name
    public final String methodName;   // 方法名 | method name
    public final long startTime;      // 开始时间 | start time
    public final long endTime;        // 结束时间 | end time
    public final long durationNs;     // 耗时（纳秒）| duration (ns)
    public final String threadName;   // 线程名 | thread name
    public final String extraInfo;    // 额外信息 | extra info

    /**
     * 构造方法，初始化所有字段。
     * Constructor to initialize all fields.
     * @param className 类名 | class name
     * @param methodName 方法名 | method name
     * @param startTime 开始时间 | start time
     * @param endTime 结束时间 | end time
     * @param durationNs 耗时（纳秒）| duration (ns)
     * @param threadName 线程名 | thread name
     * @param extraInfo 额外信息 | extra info
     */
    public MethodTimeRecord(String className, String methodName, long startTime, long endTime, long durationNs, String threadName, String extraInfo) {
        this.className = className;
        this.methodName = methodName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationNs = durationNs;
        this.threadName = threadName;
        this.extraInfo = extraInfo;
    }
} 