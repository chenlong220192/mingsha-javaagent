package site.mingsha.javaagent.methodtime.enhancer;

import org.junit.jupiter.api.Test;
import java.lang.instrument.ClassFileTransformer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MethodTimeTransformer 单元测试（mock版）。
 * Unit test for MethodTimeTransformer (with mockito).
 * mock所有依赖，验证transform逻辑。
 * Mock all dependencies, verify transform logic.
 *
 * @author mingsha
 */
public class MethodTimeTransformerTest {
    @Test
    public void testTransformNoMatch() {
        MethodTimeTransformer transformer = new MethodTimeTransformer();
        byte[] result = transformer.transform(null, "java/lang/String", null, null, new byte[0]);
        assertNull(result); // 不在采集包范围应返回null
    }
    @Test
    public void testTransformWithMock() {
        MethodTimeTransformer transformer = spy(new MethodTimeTransformer());
        // mock config静态方法可用PowerMockito等高级mock工具，简单场景下只测分支
        assertNull(transformer.transform(null, "not/match/Package", null, null, new byte[0]));
    }
} 