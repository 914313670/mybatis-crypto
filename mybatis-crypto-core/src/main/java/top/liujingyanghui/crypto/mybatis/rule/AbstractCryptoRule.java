package top.liujingyanghui.crypto.mybatis.rule;

/**
 * 加解密规则
 *
 * @author ：wdh
 * @since ：Created in 2022/5/24 22:07
 */
public class AbstractCryptoRule implements ICryptoRule {
    @Override
    public String encrypt(String content) {
        return content;
    }

    @Override
    public String decrypt(String content) {
        return content;
    }
}
