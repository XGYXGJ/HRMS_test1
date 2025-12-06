-- 添加档案号字段并设唯一约束（若已存在可跳过）
ALTER TABLE `T_Personnel_File`
    ADD COLUMN `Archive_No` VARCHAR(64) NULL;
-- 为防止重复可选添加唯一索引（执行前请确认业务上允许唯一）
-- 若并发风险较高，不建议立即加唯一索引，先测试触发器再决定
ALTER TABLE T_Personnel_File
    ADD UNIQUE INDEX UK_Personnel_Archive_No (Archive_No);

-- 设置 T_User.Password_Hash 默认为 '123'（如果当前表允许修改）
ALTER TABLE T_User
    MODIFY COLUMN Password_Hash VARCHAR(255) NOT NULL DEFAULT '123';

-- 创建触发器：在插入 T_User 前自动生成 Username（若未提供）
-- 账号格式：YYYYMMDD + 四位序号（当天已有同前缀的最大序号 + 1）
DELIMITER $$
DROP TRIGGER IF EXISTS trg_user_before_insert$$
CREATE TRIGGER trg_user_before_insert
    BEFORE INSERT ON T_User
    FOR EACH ROW
BEGIN
    DECLARE pfx VARCHAR(8);
    DECLARE maxseq INT DEFAULT 0;
    DECLARE candidate VARCHAR(64);

    -- 如果调用方已经提供了 Username 且不为空，则不覆盖
    IF NEW.Username IS NULL OR NEW.Username = '' THEN
        SET pfx = DATE_FORMAT(CURDATE(), '%Y%m%d');

        -- 从已有 Username 中取当天前缀的最大序号（假设 username 格式为 YYYYMMDDxxxx）
        SELECT COALESCE(MAX(CAST(SUBSTRING(Username, 9) AS UNSIGNED)), 0)
        INTO maxseq
        FROM T_User
        WHERE Username LIKE CONCAT(pfx, '%');

        SET candidate = CONCAT(pfx, LPAD(maxseq + 1, 4, '0'));
        SET NEW.Username = candidate;
    END IF;

    -- 如果没有提供密码，则默认 '123'
    IF NEW.Password_Hash IS NULL OR NEW.Password_Hash = '' THEN
        SET NEW.Password_Hash = '123';
    END IF;
END$$
DELIMITER ;

-- 创建触发器：在插入 T_Personnel_File 前自动填充 Archive_No（若未提供）
-- 优先使用关联 User 的 Username（若 User_ID 已存在并有 Username），否则按日期+序号生成
DELIMITER $$
DROP TRIGGER IF EXISTS trg_personnelfile_before_insert$$
CREATE TRIGGER trg_personnelfile_before_insert
    BEFORE INSERT ON T_Personnel_File
    FOR EACH ROW
BEGIN
    DECLARE uname VARCHAR(64);
    DECLARE pfx VARCHAR(8);
    DECLARE maxseq INT DEFAULT 0;
    DECLARE candidate VARCHAR(64);

    IF NEW.Archive_No IS NULL OR NEW.Archive_No = '' THEN
        IF NEW.User_ID IS NOT NULL THEN
            SELECT Username INTO uname FROM T_User WHERE User_ID = NEW.User_ID LIMIT 1;
            IF uname IS NOT NULL THEN
                SET NEW.Archive_No = uname;
            ELSE
                -- fallback to date+seq
                SET pfx = DATE_FORMAT(CURDATE(), '%Y%m%d');
                SELECT COALESCE(MAX(CAST(SUBSTRING(Archive_No, 9) AS UNSIGNED)), 0)
                INTO maxseq
                FROM T_Personnel_File
                WHERE Archive_No LIKE CONCAT(pfx, '%');
                SET candidate = CONCAT(pfx, LPAD(maxseq + 1, 4, '0'));
                SET NEW.Archive_No = candidate;
            END IF;
        ELSE
            -- 没有关联 user，按人员表中当天已有 Archive_No 最大 +1
            SET pfx = DATE_FORMAT(CURDATE(), '%Y%m%d');
            SELECT COALESCE(MAX(CAST(SUBSTRING(Archive_No, 9) AS UNSIGNED)), 0)
            INTO maxseq
            FROM T_Personnel_File
            WHERE Archive_No LIKE CONCAT(pfx, '%');
            SET candidate = CONCAT(pfx, LPAD(maxseq + 1, 4, '0'));
            SET NEW.Archive_No = candidate;
        END IF;
    END IF;
END$$
DELIMITER ;

-- 可选：当插入用户之后，如果已存在对应人员档案但 Archive_No 为空，则自动用用户用户名填充档案号
DELIMITER $$
DROP TRIGGER IF EXISTS trg_user_after_insert$$
CREATE TRIGGER trg_user_after_insert
    AFTER INSERT ON T_User
    FOR EACH ROW
BEGIN
    -- 如果已有 Personnel_File 且 Archive_No 为空，则更新为新用户名（常见场景：先插档案再插用户）
    UPDATE T_Personnel_File
    SET Archive_No = NEW.Username
    WHERE User_ID = NEW.User_ID AND (Archive_No IS NULL OR Archive_No = '');
END$$
DELIMITER ;

-- 注意：触发器中使用的 SUBSTRING/CAST 假设 Archive_No/Username 格式为 'YYYYMMDDxxxx'
-- 若系统中已有 Username/Archive_No 不匹配该格式，触发器的 MAX 查询可能返回 NULL 或错误值，
-- 请在测试环境先清洗或确认现有数据格式。