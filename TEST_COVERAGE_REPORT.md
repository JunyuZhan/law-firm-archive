# Quality Snapshot

本文件用于说明测试与质量治理的方向，不在根目录展示冗长的历史统计明细。

## Current Focus

- 保持核心业务链路具备可执行测试
- 覆盖开放接口、档案接收、检索、借阅、备份恢复等关键场景
- 将发布前验证纳入固定清单，而不是依赖临时口头确认

## Recommended Commands

Backend tests:

```bash
cd backend
mvn test
```

Frontend build:

```bash
cd frontend
npm install
npm run build
```

## Quality Documents

- [发布前验收清单](./docs/release-checklist.md)
- [测试用例清单](./docs/test-cases.md)
- [测试台账](./docs/test-ledger.md)
- [集成测试说明](./docs/integration-test.md)

## Rule

对外文档优先强调质量门槛、验证路径和发布纪律，而不是堆叠一次性的历史报表。
