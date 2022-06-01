package top.liujingyanghui.crypto.mybatis.model;

import lombok.Data;
import top.liujingyanghui.crypto.mybatis.rule.ICryptoRule;

/**
 * Mapper 加密Model
 *
 * @author : wdh
 * @since : 2022/4/15 16:14
 */
@Data
public class CryptKeyModel {

    /**
     * 加密字段
     */
    private String field;

    /**
     * 加密规则
     */
    private Class<? extends ICryptoRule> rule;

}
