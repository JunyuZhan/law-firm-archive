package com.lawfirm.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * IP 地理位置服务
 * 
 * 使用 ip2region 离线数据库查询 IP 地址的地理位置
 * 支持国内 IP 精确到城市级别
 * 
 * @author system
 */
@Slf4j
@Service
public class IpLocationService {

    @Value("${lawfirm.security.ip-location.enabled:true}")
    private boolean enabled;

    private Searcher searcher;
    private byte[] cBuff;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("IP地理位置服务已禁用");
            return;
        }

        try {
            // 从 classpath 加载 ip2region.xdb 数据库文件（优先使用 IPv4 数据库）
            ClassPathResource resource = new ClassPathResource("ip2region/ip2region.xdb");
            if (!resource.exists()) {
                // 尝试新版文件名
                resource = new ClassPathResource("ip2region/ip2region_v4.xdb");
            }
            if (!resource.exists()) {
                log.warn("ip2region 数据库文件不存在，IP地理位置服务将不可用。请下载数据库文件到 src/main/resources/ip2region/");
                return;
            }
            
            // 使用完全基于内存的查询（最快）
            try (InputStream inputStream = resource.getInputStream()) {
                cBuff = inputStream.readAllBytes();
                searcher = Searcher.newWithBuffer(cBuff);
                log.info("IP地理位置服务初始化成功，数据库大小: {} KB", cBuff.length / 1024);
            }
        } catch (Exception e) {
            log.error("IP地理位置服务初始化失败", e);
        }
    }

    /**
     * 查询 IP 地址的地理位置
     * 
     * @param ip IP地址
     * @return 位置信息
     */
    public IpLocation getLocation(String ip) {
        if (searcher == null || ip == null || ip.isEmpty()) {
            return IpLocation.unknown();
        }

        // 处理本地地址
        if (isLocalIp(ip)) {
            return IpLocation.local();
        }

        try {
            // ip2region 返回格式：国家|区域|省份|城市|ISP
            // 例如：中国|0|北京|北京|联通
            String region = searcher.search(ip);
            return parseRegion(region);
        } catch (Exception e) {
            log.warn("查询IP地理位置失败: ip={}, error={}", ip, e.getMessage());
            return IpLocation.unknown();
        }
    }

    /**
     * 判断是否为本地 IP
     */
    private boolean isLocalIp(String ip) {
        return ip.equals("127.0.0.1") 
            || ip.equals("0:0:0:0:0:0:0:1")
            || ip.equals("::1")
            || ip.startsWith("192.168.")
            || ip.startsWith("10.")
            || ip.startsWith("172.16.")
            || ip.startsWith("172.17.")
            || ip.startsWith("172.18.")
            || ip.startsWith("172.19.")
            || ip.startsWith("172.2")
            || ip.startsWith("172.30.")
            || ip.startsWith("172.31.");
    }

    /**
     * 解析 ip2region 返回的区域字符串
     */
    private IpLocation parseRegion(String region) {
        if (region == null || region.isEmpty()) {
            return IpLocation.unknown();
        }

        // 格式：国家|区域|省份|城市|ISP
        String[] parts = region.split("\\|");
        
        IpLocation location = new IpLocation();
        location.setRaw(region);
        
        if (parts.length >= 1 && !"0".equals(parts[0])) {
            location.setCountry(parts[0]);
        }
        if (parts.length >= 3 && !"0".equals(parts[2])) {
            location.setProvince(parts[2]);
        }
        if (parts.length >= 4 && !"0".equals(parts[3])) {
            location.setCity(parts[3]);
        }
        if (parts.length >= 5 && !"0".equals(parts[4])) {
            location.setIsp(parts[4]);
        }

        return location;
    }

    /**
     * 比较两个位置是否为同一地区（省级）
     */
    public boolean isSameRegion(IpLocation loc1, IpLocation loc2) {
        if (loc1 == null || loc2 == null) {
            return false;
        }
        
        // 本地地址视为同一地区
        if (loc1.isLocal() || loc2.isLocal()) {
            return true;
        }
        
        // 未知地址无法比较
        if (loc1.isUnknown() || loc2.isUnknown()) {
            return false;
        }

        // 比较省份
        String province1 = loc1.getProvince();
        String province2 = loc2.getProvince();
        
        if (province1 == null || province2 == null) {
            return false;
        }
        
        return province1.equals(province2);
    }

    /**
     * IP 位置信息
     */
    @Data
    public static class IpLocation {
        private String country;   // 国家
        private String province;  // 省份
        private String city;      // 城市
        private String isp;       // 运营商
        private String raw;       // 原始字符串
        private boolean local;    // 是否本地地址
        private boolean unknown;  // 是否未知

        public static IpLocation unknown() {
            IpLocation loc = new IpLocation();
            loc.setUnknown(true);
            return loc;
        }

        public static IpLocation local() {
            IpLocation loc = new IpLocation();
            loc.setLocal(true);
            loc.setCountry("本地网络");
            loc.setProvince("本地");
            loc.setCity("本地");
            return loc;
        }

        /**
         * 获取简短的位置描述
         */
        public String getShortDescription() {
            if (local) {
                return "本地网络";
            }
            if (unknown) {
                return "未知位置";
            }
            
            StringBuilder sb = new StringBuilder();
            if (province != null) {
                sb.append(province);
            }
            if (city != null && !city.equals(province)) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(city);
            }
            
            return sb.length() > 0 ? sb.toString() : "未知位置";
        }
    }
}
