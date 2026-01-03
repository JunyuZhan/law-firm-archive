-- 为matter表添加对方律师信息字段
ALTER TABLE matter 
ADD COLUMN IF NOT EXISTS opposing_lawyer_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS opposing_lawyer_license_no VARCHAR(50),
ADD COLUMN IF NOT EXISTS opposing_lawyer_firm VARCHAR(200),
ADD COLUMN IF NOT EXISTS opposing_lawyer_phone VARCHAR(20),
ADD COLUMN IF NOT EXISTS opposing_lawyer_email VARCHAR(100);

COMMENT ON COLUMN matter.opposing_lawyer_name IS '对方律师姓名';
COMMENT ON COLUMN matter.opposing_lawyer_license_no IS '对方律师执业证号';
COMMENT ON COLUMN matter.opposing_lawyer_firm IS '对方律师所在律所';
COMMENT ON COLUMN matter.opposing_lawyer_phone IS '对方律师联系电话';
COMMENT ON COLUMN matter.opposing_lawyer_email IS '对方律师邮箱';

CREATE INDEX IF NOT EXISTS idx_matter_opposing_lawyer_name ON matter(opposing_lawyer_name);
CREATE INDEX IF NOT EXISTS idx_matter_opposing_lawyer_firm ON matter(opposing_lawyer_firm);

