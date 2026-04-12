package com.archivesystem.repository;

import com.archivesystem.entity.DigitalFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 电子文件Mapper接口.
 * @author junyuzhan
 */
@Mapper
public interface DigitalFileMapper extends BaseMapper<DigitalFile> {

    /**
     * 根据档案ID查询文件列表.
     */
    @Select("SELECT * FROM arc_digital_file WHERE archive_id = #{archiveId} AND deleted = false ORDER BY COALESCE(volume_no, 1), COALESCE(section_type, file_category, 'MAIN'), sort_order, created_at")
    List<DigitalFile> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据多个档案ID查询文件列表.
     */
    @Select({
            "<script>",
            "SELECT * FROM arc_digital_file",
            "WHERE deleted = false",
            "  AND archive_id IN",
            "  <foreach collection='archiveIds' item='archiveId' open='(' separator=',' close=')'>",
            "    #{archiveId}",
            "  </foreach>",
            "ORDER BY archive_id, COALESCE(volume_no, 1), COALESCE(section_type, file_category, 'MAIN'), sort_order, created_at",
            "</script>"
    })
    List<DigitalFile> selectByArchiveIds(@Param("archiveIds") List<Long> archiveIds);

    /**
     * 批量关联文件到档案并写入顺序.
     */
    @Update({
            "<script>",
            "UPDATE arc_digital_file",
            "SET archive_id = #{archiveId},",
            "    sort_order = CASE id",
            "      <foreach collection='items' item='item'>",
            "        WHEN #{item.fileId} THEN #{item.sortOrder}",
            "      </foreach>",
            "      ELSE sort_order",
            "    END",
            "WHERE deleted = false",
            "  AND archive_id IS NULL",
            "  AND id IN",
            "  <foreach collection='items' item='item' open='(' separator=',' close=')'>",
            "    #{item.fileId}",
            "  </foreach>",
            "</script>"
    })
    int batchAssociateToArchive(@Param("archiveId") Long archiveId, @Param("items") List<Map<String, Object>> items);

    /**
     * 根据扫描批次号查询关联档案ID.
     */
    @Select("SELECT DISTINCT archive_id FROM arc_digital_file WHERE scan_batch_no = #{scanBatchNo} AND archive_id IS NOT NULL AND deleted = false")
    List<Long> selectArchiveIdsByScanBatchNo(@Param("scanBatchNo") String scanBatchNo);

    /**
     * 查询扫描批次汇总.
     */
    @Select({
            "<script>",
            "SELECT scan_batch_no AS scanBatchNo,",
            "       COUNT(*) AS fileCount,",
            "       COUNT(DISTINCT archive_id) AS archiveCount,",
            "       SUM(CASE WHEN file_source_type = 'SCANNED' THEN 1 ELSE 0 END) AS scannedFileCount,",
            "       SUM(CASE WHEN scan_check_status = 'PASSED' THEN 1 ELSE 0 END) AS passedCount,",
            "       SUM(CASE WHEN scan_check_status = 'PENDING' THEN 1 ELSE 0 END) AS pendingCount,",
            "       SUM(CASE WHEN scan_check_status = 'FAILED' THEN 1 ELSE 0 END) AS failedCount,",
            "       MAX(scan_time) AS latestScanTime",
            "FROM arc_digital_file",
            "WHERE deleted = false",
            "  AND scan_batch_no IS NOT NULL",
            "  AND scan_batch_no != ''",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND scan_batch_no LIKE CONCAT('%', #{keyword}, '%')",
            "</if>",
            "GROUP BY scan_batch_no",
            "ORDER BY latestScanTime DESC, scan_batch_no DESC",
            "</script>"
    })
    List<Map<String, Object>> selectScanBatchSummaries(@Param("keyword") String keyword);

    /**
     * 根据哈希值查询.
     */
    @Select("SELECT * FROM arc_digital_file WHERE hash_value = #{hashValue} AND deleted = false LIMIT 1")
    DigitalFile selectByHashValue(@Param("hashValue") String hashValue);

    /**
     * 统计档案的文件数量和总大小.
     */
    @Select("SELECT COUNT(*) as count, COALESCE(SUM(file_size), 0) as total_size FROM arc_digital_file WHERE archive_id = #{archiveId} AND deleted = false")
    java.util.Map<String, Object> countByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 统计全部文件数量和总大小.
     */
    @Select("SELECT COUNT(*) AS fileCount, COALESCE(SUM(file_size), 0) AS totalSize FROM arc_digital_file WHERE deleted = false")
    Map<String, Object> selectStorageSummary();
}
