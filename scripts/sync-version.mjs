/**
 * 自动从后端 pom.xml 同步版本号到前端 package.json
 * 在构建前自动执行
 */
import { readFileSync, writeFileSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

// 路径（脚本位于 scripts/ 目录）
const pomPath = resolve(__dirname, '../backend/pom.xml');
const packagePath = resolve(__dirname, '../frontend/apps/web-antd/package.json');

try {
  // 读取 pom.xml
  const pomContent = readFileSync(pomPath, 'utf-8');
  
  // 提取项目版本号（匹配 law-firm-management 后面的 version）
  const versionMatch = pomContent.match(/<artifactId>law-firm-management<\/artifactId>\s*<version>([^<]+)<\/version>/);
  
  if (!versionMatch) {
    console.log('⚠️  未能从 pom.xml 提取版本号，跳过同步');
    process.exit(0);
  }
  
  const backendVersion = versionMatch[1];
  
  // 读取 package.json
  const packageJson = JSON.parse(readFileSync(packagePath, 'utf-8'));
  
  // 如果版本号不同，则更新
  if (packageJson.version !== backendVersion) {
    packageJson.version = backendVersion;
    writeFileSync(packagePath, JSON.stringify(packageJson, null, 2) + '\n');
    console.log(`✅ 版本号已同步: ${backendVersion}`);
  } else {
    console.log(`✅ 版本号已是最新: ${backendVersion}`);
  }
} catch (error) {
  console.log('⚠️  同步版本号失败，继续构建:', error.message);
  process.exit(0);
}

