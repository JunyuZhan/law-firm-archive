<script setup lang="ts">
/**
 * 档案封面预览组件
 * 按照标准律师事务所业务档案卷宗封面格式显示
 * 根据案件类型和诉讼阶段动态调整显示内容
 */
import type { ArchiveDTO } from '#/api/archive/types';
import type { MatterDTO } from '#/api/matter/types';

import { computed } from 'vue';

interface Props {
  archive: ArchiveDTO;
  matter?: MatterDTO;
  firmName?: string;
  scale?: number; // 缩放比例，默认1
}

const props = withDefaults(defineProps<Props>(), {
  scale: 1,
  firmName: '律师事务所',
});

// 是否刑事案件
const isCriminal = computed(() => {
  return props.matter?.caseType === 'CRIMINAL';
});

// 是否仲裁案件
const isArbitration = computed(() => {
  const stage = props.matter?.litigationStage;
  const caseType = props.matter?.caseType;
  return (
    stage === 'ARBITRATION' ||
    caseType === 'LABOR_ARBITRATION' ||
    caseType === 'COMMERCIAL_ARBITRATION'
  );
});

// 年份转中文
function toChineseYear(year: number): string {
  const cnNumbers = [
    '〇',
    '一',
    '二',
    '三',
    '四',
    '五',
    '六',
    '七',
    '八',
    '九',
  ];
  return String(year)
    .split('')
    .map((c) => cnNumbers[Number.parseInt(c)])
    .join('');
}

// 获取年度字号（只显示档案编号）
const yearNo = computed(() => {
  const year = props.matter?.createdAt
    ? new Date(props.matter.createdAt).getFullYear()
    : new Date().getFullYear();
  const yearCn = toChineseYear(year);
  const archiveNo = props.archive.archiveNo || '    ';
  return `${yearCn}年度        字第${archiveNo}号`;
});

// 项目编号（合同编号）
const matterNo = computed(() => {
  return props.archive.matterNo || props.matter?.matterNo || '';
});

// 格式化日期
function formatDate(dateStr?: string): string {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.getFullYear()}年${String(d.getMonth() + 1).padStart(2, '0')}月${String(d.getDate()).padStart(2, '0')}日`;
}

// 归档日期
const archiveDateStr = computed(() => {
  if (props.archive.archiveDate) {
    return formatDate(props.archive.archiveDate);
  }
  return formatDate(props.archive.createdAt);
});

// 保管期限名称
const retentionPeriodName = computed(() => {
  const period = props.archive.retentionPeriod;
  if (!period) return '';
  const map: Record<string, string> = {
    PERMANENT: '永久',
    '30_YEARS': '30年',
    '20_YEARS': '20年',
    '10_YEARS': '10年',
    '5_YEARS': '5年',
    '3_YEARS': '3年',
    '1_YEAR': '1年',
  };
  return map[period] || period;
});

// 诉讼阶段名称
const litigationStageName = computed(() => {
  const stage = props.matter?.litigationStage;
  if (!stage) return '';
  const map: Record<string, string> = {
    FIRST_INSTANCE: '一审',
    SECOND_INSTANCE: '二审',
    RETRIAL: '再审',
    EXECUTION: '执行',
    ARBITRATION: '仲裁',
    CONSULTATION: '咨询',
    ALL_STAGES: '全程',
  };
  return map[stage] || stage;
});

// 根据诉讼阶段获取结果标签
const resultLabel = computed(() => {
  const stage = props.matter?.litigationStage;
  if (isArbitration.value) return '仲裁裁决';
  if (stage === 'FIRST_INSTANCE') return '一审结果';
  if (stage === 'SECOND_INSTANCE') return '二审结果';
  if (stage === 'RETRIAL') return '再审结果';
  if (stage === 'EXECUTION') return '执行结果';
  return '案件结果';
});

// 审理机关标签（法院或仲裁机构）
const judgeOrgLabel = computed(() => {
  return isArbitration.value ? '仲裁机构' : '审理法院';
});

// 截断字符串
function truncate(str?: string, maxLen = 20): string {
  if (!str) return '';
  return str.length > maxLen ? `${str.slice(0, maxLen)}...` : str;
}

// 计算容器样式（保持A4比例）
const containerStyle = computed(() => {
  const baseFontSize = 9 * props.scale;
  return {
    width: '100%',
    maxWidth: `${360 * props.scale}px`,
    aspectRatio: '1 / 1.414',
    fontSize: `${baseFontSize}px`,
    margin: '0 auto',
  };
});
</script>

<template>
  <div class="cover-container" :style="containerStyle">
    <div class="cover-content">
      <!-- 律所名称 -->
      <div class="firm-name">{{ firmName }}</div>

      <!-- 主标题 -->
      <div class="main-title">业 务 档 案 卷 宗</div>

      <!-- 副标题 -->
      <div v-if="isCriminal" class="sub-title">（刑事诉讼类）</div>
      <div v-else-if="isArbitration" class="sub-title">（仲裁类）</div>

      <!-- 年度字号（档案编号） -->
      <div class="year-no">{{ yearNo }}</div>

      <!-- 民事/行政/仲裁案件模板 -->
      <template v-if="!isCriminal">
        <table class="info-table">
          <tbody>
            <!-- 项目编号（合同编号） -->
            <tr>
              <td class="label">项目编号</td>
              <td class="value" colspan="3">{{ matterNo }}</td>
            </tr>
            <!-- 案由 -->
            <tr>
              <td class="label">案 由</td>
              <td class="value" colspan="3">
                {{ matter?.causeOfAction || '' }}
              </td>
            </tr>
            <!-- 委托人/承办人 -->
            <tr>
              <td class="label">委 托 人</td>
              <td class="value">{{ archive.clientName || '' }}</td>
              <td class="label">承 办 人</td>
              <td class="value">{{ archive.mainLawyerName || '' }}</td>
            </tr>
            <!-- 当事人 -->
            <tr>
              <td class="label party-label" rowspan="3">当<br />事<br />人</td>
              <td class="label small">{{ isArbitration ? '申请人' : '原告' }}</td>
              <td class="value" colspan="2">{{ archive.clientName || '' }}</td>
            </tr>
            <tr>
              <td class="label small">
                {{ isArbitration ? '被申请人' : '被告' }}
              </td>
              <td class="value" colspan="2">{{ matter?.opposingParty || '' }}</td>
            </tr>
            <tr>
              <td class="label small">第三人</td>
              <td class="value" colspan="2"></td>
            </tr>
            <!-- 审理机关 -->
            <tr>
              <td class="label">{{ judgeOrgLabel }}</td>
              <td class="value" colspan="3"></td>
            </tr>
            <!-- 收案/结案日期 -->
            <tr>
              <td class="label">收案日期</td>
              <td class="value">{{ formatDate(matter?.filingDate) }}</td>
              <td class="label">结案日期</td>
              <td class="value">{{ formatDate(archive.caseCloseDate) }}</td>
            </tr>
            <!-- 案件结果（根据诉讼阶段显示） -->
            <tr>
              <td class="label">{{ resultLabel }}</td>
              <td class="value">{{ truncate(matter?.outcome, 10) }}</td>
              <td class="label">代理阶段</td>
              <td class="value">{{ litigationStageName }}</td>
            </tr>
            <!-- 归档/保存期限 -->
            <tr>
              <td class="label">归档日期</td>
              <td class="value">{{ archiveDateStr }}</td>
              <td class="label">保存期限</td>
              <td class="value">{{ retentionPeriodName }}</td>
            </tr>
            <!-- 立卷人/备注 -->
            <tr>
              <td class="label">立 卷 人</td>
              <td class="value">{{ archive.mainLawyerName || '' }}</td>
              <td class="label">备 注</td>
              <td class="value">{{ truncate(archive.remarks, 10) }}</td>
            </tr>
          </tbody>
        </table>
      </template>

      <!-- 刑事案件模板 -->
      <template v-else>
        <table class="info-table">
          <tbody>
            <!-- 项目编号 -->
            <tr>
              <td class="label">项目编号</td>
              <td class="value" colspan="3">{{ matterNo }}</td>
            </tr>
            <!-- 被告人/罪名 -->
            <tr>
              <td class="label">被 告 人</td>
              <td class="value" colspan="3">{{ matter?.opposingParty || '' }}</td>
            </tr>
            <tr>
              <td class="label">罪 名</td>
              <td class="value" colspan="3">
                {{ matter?.causeOfAction || '' }}
              </td>
            </tr>
            <!-- 委托人/承办律师 -->
            <tr>
              <td class="label">委 托 人</td>
              <td class="value">{{ archive.clientName || '' }}</td>
              <td class="label">承办律师</td>
              <td class="value">{{ archive.mainLawyerName || '' }}</td>
            </tr>
            <!-- 审理法院 -->
            <tr>
              <td class="label">审理法院</td>
              <td class="value" colspan="3"></td>
            </tr>
            <!-- 收案/结案日期 -->
            <tr>
              <td class="label">收案日期</td>
              <td class="value">{{ formatDate(matter?.filingDate) }}</td>
              <td class="label">结案日期</td>
              <td class="value">{{ formatDate(archive.caseCloseDate) }}</td>
            </tr>
            <!-- 审理结果/审级 -->
            <tr>
              <td class="label">{{ resultLabel }}</td>
              <td class="value">{{ truncate(matter?.outcome, 10) }}</td>
              <td class="label">代理阶段</td>
              <td class="value">{{ litigationStageName }}</td>
            </tr>
            <!-- 归档/保存期限 -->
            <tr>
              <td class="label">归档日期</td>
              <td class="value">{{ archiveDateStr }}</td>
              <td class="label">保存期限</td>
              <td class="value">{{ retentionPeriodName }}</td>
            </tr>
            <!-- 立卷人/备注 -->
            <tr>
              <td class="label">立 卷 人</td>
              <td class="value">{{ archive.mainLawyerName || '' }}</td>
              <td class="label">备 注</td>
              <td class="value">{{ truncate(archive.remarks, 10) }}</td>
            </tr>
          </tbody>
        </table>
      </template>
    </div>
  </div>
</template>

<style scoped>
.cover-container {
  position: relative;
  box-sizing: border-box;
  padding: 10px;
  overflow: hidden;
  font-family: SimSun, STSong, 'Songti SC', serif;
  color: #3d2914;
  background-color: #d2b48c;
  border: 2px solid #5a3e1b;
  box-shadow:
    0 2px 8px rgb(0 0 0 / 15%),
    inset 0 0 30px rgb(139 90 43 / 10%);
}

.cover-content {
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 8px;
  border: 1px solid #5a3e1b;
}

.firm-name {
  margin-bottom: 6px;
  font-size: 1.2em;
  text-align: center;
  letter-spacing: 2px;
}

.main-title {
  margin-bottom: 4px;
  font-size: 1.8em;
  font-weight: bold;
  text-align: center;
  letter-spacing: 4px;
}

.sub-title {
  margin-bottom: 4px;
  font-size: 1.1em;
  text-align: center;
}

.year-no {
  margin-bottom: 10px;
  font-size: 1em;
  text-align: center;
  letter-spacing: 1px;
}

.info-table {
  flex: 1;
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}

.info-table td {
  padding: 3px 4px;
  line-height: 1.3;
  vertical-align: middle;
  border: 1px solid #5a3e1b;
}

.info-table .label {
  width: 22%;
  font-weight: normal;
  text-align: center;
  background-color: rgb(210 180 140 / 60%);
}

.info-table .label.small {
  width: 15%;
  font-size: 0.9em;
}

.info-table .label.party-label {
  width: 10%;
  padding: 2px;
  font-size: 0.85em;
  letter-spacing: 2px;
  writing-mode: vertical-lr;
  text-orientation: upright;
}

.info-table .value {
  padding-left: 6px;
  font-size: 0.95em;
  text-align: left;
  word-break: break-all;
  background-color: rgb(222 197 160 / 30%);
}

/* 牛皮纸纹理效果 */
.cover-container::before {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  content: '';
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.08'/%3E%3C/svg%3E");
}
</style>
