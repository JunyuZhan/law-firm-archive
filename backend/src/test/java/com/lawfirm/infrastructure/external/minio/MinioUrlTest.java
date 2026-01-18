import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MinioUrlTest {
    private static final String bucketName = "law-firm";

    public static void main(String[] args) {
        // 模拟生产环境日志中的 URL (带有查询参数和可能的编码)
        // 注意：这里模拟的是从数据库取出或前端传递给后端的原始 URL
        // 假设原始 URL 已经是编码过一次的（因为 OnlyOffice 日志里最终是 %25E8，说明后端生成前拿到的是 %E8）
        String inputUrl = "http://minio:9000/law-firm/matters/101/5.%E8%B5%B7%E8%AF%89%E7%8A%B6.doc?X-Amz-Algorithm=AWS4-HMAC-SHA256";

        System.out.println("Input URL: " + inputUrl);

        String objectName = extractObjectName(inputUrl);
        System.out.println("Extracted ObjectName: " + objectName);

        // 验证结果是否为纯中文（无编码）
        // 如果 output 是 "matters/101/5.起诉状.doc"，则通过
        // 如果 output 是 "matters/101/5.%E8%B5%B7%E8%AF%89%E7%8A%B6.doc"，则失败（会导致 MinIO SDK
        // 再次编码变成 %25E8）
    }

    public static String extractObjectName(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains(bucketName + "/")) {
            return null;
        }
        int index = fileUrl.indexOf(bucketName + "/");
        String objectName = fileUrl.substring(index + bucketName.length() + 1);

        // 1. 先进行 URL 解码 (处理 %3F, %25E8 等)
        try {
            objectName = URLDecoder.decode(objectName, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. 解码后再去除查询参数 (此时 %3F 已变成 ?)
        if (objectName.contains("?")) {
            objectName = objectName.substring(0, objectName.indexOf("?"));
        }

        return objectName;
    }
}
